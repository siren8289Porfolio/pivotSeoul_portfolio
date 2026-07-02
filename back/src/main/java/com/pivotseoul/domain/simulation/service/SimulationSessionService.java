package com.pivotseoul.domain.simulation.service;

import com.pivotseoul.domain.simulation.dto.CreateSessionRequest;
import com.pivotseoul.domain.simulation.dto.CreateSessionResponse;
import com.pivotseoul.domain.simulation.entity.Scenario;
import com.pivotseoul.domain.simulation.entity.SimulationSession;
import com.pivotseoul.domain.simulation.entity.UserCondition;
import com.pivotseoul.domain.simulation.repository.ScenarioRepository;
import com.pivotseoul.domain.simulation.repository.SimulationSessionRepository;
import com.pivotseoul.domain.simulation.entity.LifeStage;
import com.pivotseoul.domain.simulation.repository.LifeStageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SimulationSessionService {

    private final LifeStageRepository lifeStageRepository;
    private final SimulationSessionRepository simulationSessionRepository;
    private final ScenarioRepository scenarioRepository;

    public SimulationSessionService(
            LifeStageRepository lifeStageRepository,
            SimulationSessionRepository simulationSessionRepository,
            ScenarioRepository scenarioRepository
    ) {
        this.lifeStageRepository = lifeStageRepository;
        this.simulationSessionRepository = simulationSessionRepository;
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional
    public CreateSessionResponse createSession(CreateSessionRequest request) {
        String stageCode = normalizeLifeStageCode(request.lifeStageCode());
        LifeStage lifeStage = lifeStageRepository.findByStageCode(stageCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 생애단계 코드입니다: " + request.lifeStageCode()));

        String sessionUuid = UUID.randomUUID().toString();
        SimulationSession session = SimulationSession.create(
                sessionUuid,
                lifeStage,
                null,
                "READY",
                false,
                Instant.now()
        );

        UserCondition userCondition = new UserCondition(
                session,
                request.currentDistrict(),
                null,
                request.monthlyIncome(),
                request.monthlyHousing(),
                request.monthlyLiving(),
                request.commuteTime(),
                request.childcareCost(),
                request.returnToWorkMonths(),
                request.retirementAge(),
                request.savings()
        );
        session.setUserCondition(userCondition);

        SimulationSession saved = simulationSessionRepository.save(session);

        scenarioRepository.save(Scenario.createMvp(saved.getSessionId()));

        return new CreateSessionResponse(
                saved.getSessionId(),
                saved.getSessionUuid(),
                saved.getSessionStatus()
        );
    }

    private String normalizeLifeStageCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("lifeStageCode는 필수입니다.");
        }
        return switch (code.trim().toLowerCase()) {
            case "youth" -> "YOUTH";
            case "family" -> "FAMILY";
            case "senior" -> "SENIOR";
            default -> code.trim().toUpperCase();
        };
    }
}
