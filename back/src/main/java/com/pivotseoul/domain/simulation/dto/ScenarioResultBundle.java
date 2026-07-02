package com.pivotseoul.domain.simulation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ScenarioResultBundle(
        Long scenarioResultId,
        Long simulationRunId,
        Long scenarioId,
        String resultStatus,
        BigDecimal totalScore,
        BigDecimal riskScore,
        BigDecimal confidenceScore,
        BigDecimal housingScore,
        List<ThresholdResultResponse> thresholds
) {
    public ScenarioResultBundle withThresholds(List<ThresholdResultResponse> newThresholds) {
        return new ScenarioResultBundle(
                scenarioResultId,
                simulationRunId,
                scenarioId,
                resultStatus,
                totalScore,
                riskScore,
                confidenceScore,
                housingScore,
                newThresholds
        );
    }
}
