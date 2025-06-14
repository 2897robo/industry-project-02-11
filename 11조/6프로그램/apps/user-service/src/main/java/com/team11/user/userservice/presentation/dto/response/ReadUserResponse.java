package com.team11.user.userservice.presentation.dto.response;

import com.team11.user.userservice.persistence.domain.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReadUserResponse(
        Long id,
        String uid,
        String name,
        String email,
        LocalDateTime createdAt
) {
    public static ReadUserResponse from(User user) {
        return ReadUserResponse.builder()
                .id(user.getId())
                .uid(user.getUid())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .email(user.getEmail())
                .build();
    }
}