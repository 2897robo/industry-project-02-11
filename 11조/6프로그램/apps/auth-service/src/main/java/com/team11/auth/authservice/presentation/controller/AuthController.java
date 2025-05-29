package com.team11.auth.authservice.presentation.controller;

import com.team11.auth.authservice.application.service.AuthService;
import com.team11.auth.authservice.presentation.dto.LoginRequest;
import com.team11.auth.authservice.presentation.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<String> loginByRefreshToken(@RequestHeader("Authorization") String accessToken, @RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshTokenLogin(accessToken, refreshToken));
    }
}
