package com.team11.backend.domain.config.dto.request;

import com.team11.backend.domain.config.entity.Config;

import java.time.LocalDateTime;

public record CreateConfigRequest(
        Float idleThreshold,
        Integer budgetLimit,
        LocalDateTime createdAt
) {
    public Config toEntity(String userId) {
        return Config.builder()
                .userId(userId)
                .idleThreshold(idleThreshold)
                .budgetLimit(budgetLimit)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
