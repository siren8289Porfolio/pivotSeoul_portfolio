package com.pivotseoul.domain.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "scenario_result")
public class ScenarioResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_result_id")
    private Long scenarioResultId;

    @Column(name = "simulation_run_id", nullable = false)
    private Long simulationRunId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "result_status", length = 32)
    private String resultStatus;

    @Column(name = "total_score", precision = 14, scale = 6)
    private BigDecimal totalScore;

    @Column(name = "risk_score", precision = 14, scale = 6)
    private BigDecimal riskScore;

    @Column(name = "confidence_score", precision = 14, scale = 6)
    private BigDecimal confidenceScore;

    @Column(name = "housing_score", precision = 14, scale = 6)
    private BigDecimal housingScore;

    public ScenarioResult() {
    }

    public Long getScenarioResultId() {
        return scenarioResultId;
    }

    public void setScenarioResultId(Long scenarioResultId) {
        this.scenarioResultId = scenarioResultId;
    }

    public Long getSimulationRunId() {
        return simulationRunId;
    }

    public void setSimulationRunId(Long simulationRunId) {
        this.simulationRunId = simulationRunId;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public BigDecimal getHousingScore() {
        return housingScore;
    }

    public void setHousingScore(BigDecimal housingScore) {
        this.housingScore = housingScore;
    }
}
