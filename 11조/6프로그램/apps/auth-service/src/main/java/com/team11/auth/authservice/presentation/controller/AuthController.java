package com.team11.auth.authservice.presentation.controller;

import com.team11.auth.authservice.application.service.AuthService;
import com.team11.auth.authservice.presentation.dto.LoginRequest;
import com.team11.auth.authservice.presentation.dto.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request, HttpServletResponse resp) {
        TokenResponse token = authService.login(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", token.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("NONE")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(TokenResponse.builder().accessToken(token.accessToken()).build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<String> loginByRefreshToken(@RequestHeader("Authorization") String accessToken,
                                                      @CookieValue(name = "refresh_token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshTokenLogin(accessToken, refreshToken));
    }
}
