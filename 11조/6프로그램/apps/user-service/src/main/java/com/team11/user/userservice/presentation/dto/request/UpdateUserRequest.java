package com.team11.user.userservice.presentation.dto.request;

public record UpdateUserRequest(
        String password,
        String email,
        String name
) {
}
