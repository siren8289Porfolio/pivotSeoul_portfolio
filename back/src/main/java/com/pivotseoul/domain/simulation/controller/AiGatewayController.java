package com.pivotseoul.domain.simulation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pivotseoul.domain.simulation.service.AiGatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MVP AI 게이트웨이 — 주거(RIR) 분석만 FastAPI로 프록시합니다.
 */
@RestController
@RequestMapping("/api/ai")
public class AiGatewayController {

    private final AiGatewayService aiGatewayService;

    public AiGatewayController(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return aiGatewayService.bridgeStatus();
    }

    @PostMapping("/housing/analyze")
    public ResponseEntity<JsonNode> housingAnalyze(@RequestBody(required = false) JsonNode body) {
        return aiGatewayService.housingAnalyze(body);
    }
}
