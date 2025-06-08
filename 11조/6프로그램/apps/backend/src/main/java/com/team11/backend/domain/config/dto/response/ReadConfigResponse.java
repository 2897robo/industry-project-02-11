package com.team11.backend.domain.config.dto.response;

import com.team11.backend.domain.config.entity.Config;
import lombok.Builder;

@Builder
public record ReadConfigResponse(
        Long id,
        String userId,
        Float idleThreshold,
        Integer budgetLimit
) {
    public static ReadConfigResponse fromEntity(Config config) {
        return ReadConfigResponse.builder()
                .id(config.getId())
                .userId(config.getUserId())
                .idleThreshold(config.getIdleThreshold())
                .budgetLimit(config.getBudgetLimit())
                .build();
    }
}
