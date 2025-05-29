package com.team11.auth.authservice.presentation.dto;

public record LoginRequest(
        String uid,
        String password
) {
}
