package com.pivotseoul.domain.simulation.controller;

import com.pivotseoul.domain.simulation.dto.SimulationResultResponse;
import com.pivotseoul.domain.simulation.service.SimulationResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
public class SimulationResultController {

    private final SimulationResultService simulationResultService;

    public SimulationResultController(SimulationResultService simulationResultService) {
        this.simulationResultService = simulationResultService;
    }

    @GetMapping("/results/{scenarioResultId}")
    public SimulationResultResponse getResult(@PathVariable Long scenarioResultId) {
        return simulationResultService.getResult(scenarioResultId);
    }
}
