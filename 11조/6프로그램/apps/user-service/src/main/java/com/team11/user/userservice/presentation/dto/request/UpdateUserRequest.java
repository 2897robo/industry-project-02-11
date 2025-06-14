package com.team11.user.userservice.presentation.dto.request;

public record UpdateUserRequest(
        String currentPassword,
        String newPassword,
        String email,
        String name
) {
}
