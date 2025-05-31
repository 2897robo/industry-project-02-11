package com.team11.auth.authservice.application.service;

import com.team11.auth.authservice.infrastructure.dto.ReadUserResponse;
import com.team11.auth.authservice.commons.utils.TokenProvider;
import com.team11.auth.authservice.infrastructure.adapter.UserAdapter;
import com.team11.auth.authservice.persistence.domain.Token;
import com.team11.auth.authservice.presentation.dto.LoginRequest;
import com.team11.auth.authservice.presentation.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;


@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserAdapter userAdapter;
    private final TokenProvider tokenProvider;
    private static String TOKEN_PREFIX = "Bearer ";

    public TokenResponse login(LoginRequest request) {
        ReadUserResponse user = userAdapter.findByUidAndPassword(request.uid(), request.password());
        String accessToken = tokenProvider.generateAccessToken(user.uid());
        String refreshToken = tokenProvider.generateRefreshToken(user.uid(), accessToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String refreshTokenLogin(String accessToken, String refreshToken) {

        accessToken = resolveToken(accessToken);

        Token token = tokenProvider.validRefreshToken(accessToken, refreshToken);
        String uid = tokenProvider.getUidFromToken(refreshToken);
        return tokenProvider.reissueAccessToken(token, uid);
    }

    private String resolveToken(String token) {
        if (ObjectUtils.isEmpty(token) || !token.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        return token.substring(TOKEN_PREFIX.length());
    }
}
