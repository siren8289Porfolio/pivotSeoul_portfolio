package com.pivotseoul.domain.simulation.dto;

public record CreateSessionResponse(
        Long sessionId,
        String sessionUuid,
        String sessionStatus
) {}