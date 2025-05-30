package com.team11.auth.authservice.application.service;

import com.team11.auth.authservice.commons.exception.TokenException;
import com.team11.auth.authservice.commons.exception.payload.ErrorStatus;
import com.team11.auth.authservice.persistence.domain.Token;
import com.team11.auth.authservice.persistence.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final TokenRepository tokenRepository;

    public void deleteRefreshToken(String memberKey) {
        tokenRepository.deleteById(memberKey);
    }

    public String saveOrUpdate(String userKey, String refreshToken, String accessToken) {

        Token token = tokenRepository.findByAccessToken(accessToken)
                .map(o -> o.updateRefreshToken(refreshToken))
                .orElseGet(() -> tokenRepository.save(Token.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .id(userKey)
                        .build()));

        tokenRepository.save(token);

        return refreshToken;
    }

    public Token findByAccessTokenOrThrow(String accessToken) {
        return tokenRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new TokenException(
                        ErrorStatus.toErrorStatus("토큰을 찾을 수 없습니다.", 401, LocalDateTime.now())));
    }

    @Transactional
    public void updateToken(String accessToken, Token token) {
        token.updateAccessToken(accessToken);
        tokenRepository.save(token);
    }
}
