package com.pivotseoul.domain.simulation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pivotseoul.global.observability.PipelineRunLogService;
import com.pivotseoul.domain.simulation.dto.RunSimulationRequest;
import com.pivotseoul.domain.simulation.dto.RunSimulationResponse;
import com.pivotseoul.domain.simulation.entity.Scenario;
import com.pivotseoul.domain.simulation.entity.SimulationRun;
import com.pivotseoul.domain.simulation.entity.SimulationSession;
import com.pivotseoul.domain.simulation.repository.ScenarioRepository;
import com.pivotseoul.domain.simulation.repository.SimulationRunRepository;
import com.pivotseoul.domain.simulation.repository.SimulationSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class SimulationEngineService {

    private static final double HOUSING_RED_ZONE_THRESHOLD = 0.4;

    private final AiGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;
    private final SimulationRunRepository simulationRunRepository;
    private final SimulationSessionRepository simulationSessionRepository;
    private final CalculationLogService calculationLogService;
    private final SimulationResultSaveService simulationResultSaveService;
    private final ScenarioRepository scenarioRepository;
    private final PipelineRunLogService pipelineRunLogService;

    public SimulationEngineService(
            AiGatewayService aiGatewayService,
            ObjectMapper objectMapper,
            SimulationRunRepository simulationRunRepository,
            SimulationSessionRepository simulationSessionRepository,
            CalculationLogService calculationLogService,
            SimulationResultSaveService simulationResultSaveService,
            ScenarioRepository scenarioRepository,
            PipelineRunLogService pipelineRunLogService) {
        this.aiGatewayService = aiGatewayService;
        this.objectMapper = objectMapper;
        this.simulationRunRepository = simulationRunRepository;
        this.simulationSessionRepository = simulationSessionRepository;
        this.calculationLogService = calculationLogService;
        this.simulationResultSaveService = simulationResultSaveService;
        this.scenarioRepository = scenarioRepository;
        this.pipelineRunLogService = pipelineRunLogService;
    }

    @Transactional
    public ResponseEntity<RunSimulationResponse> runSimulation(
            String sessionId,
            RunSimulationRequest request) {
        long pipelineLogId = pipelineRunLogService.start("simulation_run");
        long startedAt = System.currentTimeMillis();

        try {
            return executeRun(sessionId, request, pipelineLogId, startedAt);
        } catch (RuntimeException ex) {
            pipelineRunLogService.fail(
                    pipelineLogId,
                    ex.getMessage(),
                    System.currentTimeMillis() - startedAt
            );
            throw ex;
        }
    }

    private ResponseEntity<RunSimulationResponse> executeRun(
            String sessionId,
            RunSimulationRequest request,
            long pipelineLogId,
            long startedAt
    ) {
        Long numericSessionId = resolveSessionId(sessionId);
        assertSessionExists(numericSessionId);

        SimulationRun simulationRun = createRunningSimulationRun(numericSessionId);
        JsonNode aiRequestBody = objectMapper.valueToTree(request);

        ResponseEntity<JsonNode> aiResponse = aiGatewayService.housingAnalyze(aiRequestBody);
        JsonNode aiResult = aiResponse.getBody();

        boolean success = aiResponse.getStatusCode().is2xxSuccessful();

        if (!success) {
            simulationRun.setRunStatus("FAILED");
            simulationRun.setCompletedAt(Instant.now());
            simulationRunRepository.save(simulationRun);

            calculationLogService.saveFailureLog(
                    simulationRun.getSimulationRunId(),
                    aiRequestBody,
                    aiResult,
                    extractText(aiResult, "error", "FASTAPI_ERROR"),
                    extractText(aiResult, "detail", "FastAPI request failed"));

            RunSimulationResponse failedResponse = new RunSimulationResponse(
                    String.valueOf(numericSessionId),
                    null,
                    "FAILED",
                    "UNKNOWN",
                    null,
                    0,
                    0.0,
                    List.of(),
                    aiResult);

            pipelineRunLogService.fail(
                    pipelineLogId,
                    extractText(aiResult, "error", "FASTAPI_ERROR"),
                    System.currentTimeMillis() - startedAt
            );

            return ResponseEntity.status(aiResponse.getStatusCode()).body(failedResponse);
        }

        String resultStatus = extractText(aiResult, "housing_status", "UNKNOWN").toUpperCase();
        Integer riskScore = extractInt(aiResult, "risk_score", 0);
        Double confidenceScore = extractDouble(aiResult, "confidence_score", 0.0);
        Double rir = extractDouble(aiResult, "rir", null);
        Boolean isRedZone = extractBoolean(aiResult, "is_red_zone", false);

        simulationRun.setRunStatus("COMPLETED");
        simulationRun.setTotalConfidenceScore(BigDecimal.valueOf(confidenceScore));
        simulationRun.setCompletedAt(Instant.now());
        simulationRunRepository.save(simulationRun);

        calculationLogService.saveSuccessLog(
                simulationRun.getSimulationRunId(),
                aiRequestBody,
                aiResult);

        Long scenarioId = resolveScenarioId(numericSessionId);

        Long scenarioResultId = simulationResultSaveService.saveHousingResult(
                simulationRun.getSimulationRunId(),
                scenarioId,
                aiResult);

        RunSimulationResponse.ThresholdResultItem housingThreshold = new RunSimulationResponse.ThresholdResultItem(
                "HOUSING",
                rir,
                HOUSING_RED_ZONE_THRESHOLD,
                resultStatus,
                isRedZone);

        RunSimulationResponse response = new RunSimulationResponse(
                String.valueOf(numericSessionId),
                scenarioResultId,
                "COMPLETED",
                resultStatus,
                rir,
                riskScore,
                confidenceScore,
                List.of(housingThreshold),
                aiResult);

        pipelineRunLogService.complete(
                pipelineLogId,
                1,
                System.currentTimeMillis() - startedAt,
                simulationRun.getCompletedAt()
        );

        return ResponseEntity.ok(response);
    }

    private void assertSessionExists(Long sessionId) {
        if (!simulationSessionRepository.existsById(sessionId)) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
    }

    private Long resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId는 필수입니다.");
        }

        try {
            return Long.parseLong(sessionId.trim());
        } catch (NumberFormatException ignored) {
            SimulationSession session = simulationSessionRepository.findBySessionUuid(sessionId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
            return session.getSessionId();
        }
    }

    private SimulationRun createRunningSimulationRun(Long sessionId) {
        SimulationRun simulationRun = new SimulationRun();
        simulationRun.setSessionId(sessionId);
        simulationRun.setRunStatus("RUNNING");
        simulationRun.setCalculationEngineVersion("MVP_V1");
        simulationRun.setAiPipelineVersion("FASTAPI_HOUSING_V1");
        simulationRun.setModelVersion("RIR_RULE_V1");
        simulationRun.setStartedAt(Instant.now());

        return simulationRunRepository.save(simulationRun);
    }

    private Long resolveScenarioId(Long sessionId) {
        Scenario scenario = scenarioRepository
                .findFirstBySessionIdOrderByDisplayOrderAscScenarioIdAsc(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Scenario is not initialized for sessionId=" + sessionId));

        return scenario.getScenarioId();
    }

    private String extractText(JsonNode node, String fieldName, String defaultValue) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        return node.get(fieldName).asText(defaultValue);
    }

    private Integer extractInt(JsonNode node, String fieldName, Integer defaultValue) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        return node.get(fieldName).asInt(defaultValue);
    }

    private Double extractDouble(JsonNode node, String fieldName, Double defaultValue) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        return node.get(fieldName).asDouble();
    }

    private Boolean extractBoolean(JsonNode node, String fieldName, Boolean defaultValue) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return defaultValue;
        }

        return node.get(fieldName).asBoolean(defaultValue);
    }
}
