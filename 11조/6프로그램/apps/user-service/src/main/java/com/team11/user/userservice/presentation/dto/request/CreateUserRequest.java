package com.team11.user.userservice.presentation.dto.request;

import com.team11.user.userservice.persistence.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

public record CreateUserRequest(
        String uid,
        String password,
        String email,
        String name
) {
    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .uid(uid)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
