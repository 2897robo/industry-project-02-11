package com.team11.backend.domain.config.dto.request;

public record UpdateConfigRequest(
        Long id,
        Float idleThreshold,
        Integer budgetLimit
) {
}
