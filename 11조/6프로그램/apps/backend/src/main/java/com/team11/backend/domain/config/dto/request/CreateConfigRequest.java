package com.team11.backend.domain.config.dto.request;

import com.team11.backend.domain.config.entity.Config;

import java.time.LocalDateTime;

public record CreateConfigRequest(
        Float idleThreshold,
        Integer budgetLimit,
        LocalDateTime createdAt
) {
    public Config toEntity(String userUid) {
        return Config.builder()
                .userUid(userUid)
                .idleThreshold(idleThreshold)
                .budgetLimit(budgetLimit)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
