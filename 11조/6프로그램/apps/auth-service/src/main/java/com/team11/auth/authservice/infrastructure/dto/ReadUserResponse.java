package com.team11.auth.authservice.infrastructure.dto;

import java.time.LocalDateTime;

public record ReadUserResponse(
        Long id,
        String uid,
        String name,
        LocalDateTime createdAt
) {
}
