package com.pivotseoul.domain.simulation.service;

import com.pivotseoul.domain.simulation.dto.ResultSummaryResponse;
import com.pivotseoul.domain.simulation.dto.ScenarioResultBundle;
import com.pivotseoul.domain.simulation.dto.SimulationResultResponse;
import com.pivotseoul.domain.simulation.dto.ThresholdResultResponse;
import com.pivotseoul.domain.simulation.repository.ScenarioResultQueryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationResultService {

    private static final String STATUS_SAFE = "safe";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_DANGER = "danger";
    private static final String RESULT_STATUS_PLACEHOLDER = "PLACEHOLDER";
    private static final String MESSAGE_SAFE = "주요 임계값이 안정 범위입니다.";
    private static final String MESSAGE_WARNING = "일부 지표가 주의 구간에 진입했습니다.";
    private static final String MESSAGE_DANGER = "위험 임계값을 초과한 지표가 있습니다.";
    private static final BigDecimal WARNING_RISK_SCORE = new BigDecimal("40");
    private static final BigDecimal DANGER_RISK_SCORE = new BigDecimal("70");

    private final ScenarioResultQueryRepository scenarioResultQueryRepository;

    public SimulationResultService(ScenarioResultQueryRepository scenarioResultQueryRepository) {
        this.scenarioResultQueryRepository = scenarioResultQueryRepository;
    }

    public SimulationResultResponse getResult(Long scenarioResultId) {
        return scenarioResultQueryRepository.findBundleById(scenarioResultId)
                .map(this::toResultResponse)
                .orElseGet(() -> placeholderResult(scenarioResultId));
    }

    public ResultSummaryResponse getSummary(Long scenarioResultId) {
        return scenarioResultQueryRepository.findBundleById(scenarioResultId)
                .map(this::toSummaryResponse)
                .orElseGet(() -> placeholderSummary(scenarioResultId));
    }

    public List<ResultSummaryResponse> getHistory(Long simulationRunId) {
        if (simulationRunId == null) {
            return List.of();
        }
        return scenarioResultQueryRepository.findBundlesByRunId(simulationRunId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private SimulationResultResponse toResultResponse(ScenarioResultBundle bundle) {
        ResultSummaryResponse summary = toSummaryResponse(bundle);
        return new SimulationResultResponse(
                bundle.scenarioResultId(),
                bundle.simulationRunId(),
                bundle.scenarioId(),
                bundle.resultStatus(),
                summary.riskStatus(),
                summary,
                buildScoreMap(bundle),
                bundle.thresholds(),
                List.of()
        );
    }

    private ResultSummaryResponse toSummaryResponse(ScenarioResultBundle bundle) {
        long redZoneCount = bundle.thresholds().stream().filter(ThresholdResultResponse::redZone).count();
        String riskStatus = resolveRiskStatus(bundle.riskScore(), redZoneCount);
        return new ResultSummaryResponse(
                bundle.scenarioResultId(),
                bundle.simulationRunId(),
                bundle.scenarioId(),
                bundle.resultStatus(),
                riskStatus,
                bundle.totalScore(),
                bundle.riskScore(),
                bundle.confidenceScore(),
                redZoneCount,
                resolveSummaryMessage(riskStatus)
        );
    }

    private Map<String, BigDecimal> buildScoreMap(ScenarioResultBundle bundle) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        scores.put("housing", bundle.housingScore());
        return scores;
    }

    private String resolveRiskStatus(BigDecimal riskScore, long redZoneCount) {
        if (redZoneCount > 0 || isAtLeast(riskScore, DANGER_RISK_SCORE)) {
            return STATUS_DANGER;
        }
        if (isAtLeast(riskScore, WARNING_RISK_SCORE)) {
            return STATUS_WARNING;
        }
        return STATUS_SAFE;
    }

    private boolean isAtLeast(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private String resolveSummaryMessage(String riskStatus) {
        return switch (riskStatus) {
            case STATUS_DANGER -> MESSAGE_DANGER;
            case STATUS_WARNING -> MESSAGE_WARNING;
            default -> MESSAGE_SAFE;
        };
    }

    private SimulationResultResponse placeholderResult(Long scenarioResultId) {
        ResultSummaryResponse summary = placeholderSummary(scenarioResultId);
        return new SimulationResultResponse(
                scenarioResultId,
                null,
                null,
                RESULT_STATUS_PLACEHOLDER,
                STATUS_WARNING,
                summary,
                Map.of(),
                List.of(),
                List.of()
        );
    }

    private ResultSummaryResponse placeholderSummary(Long scenarioResultId) {
        return new ResultSummaryResponse(
                scenarioResultId,
                null,
                null,
                RESULT_STATUS_PLACEHOLDER,
                STATUS_WARNING,
                null,
                null,
                null,
                0,
                MESSAGE_WARNING
        );
    }
}
