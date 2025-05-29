package com.team11.auth.authservice.application.dto;

public record ReadUserResponse(
        String uid,
        String password
) {
}
