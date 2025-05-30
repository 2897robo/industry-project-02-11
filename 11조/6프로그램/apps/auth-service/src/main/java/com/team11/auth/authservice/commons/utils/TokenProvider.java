package com.team11.auth.authservice.commons.utils;

import com.team11.auth.authservice.application.dto.ReadUserResponse;
import com.team11.auth.authservice.application.service.TokenService;
import com.team11.auth.authservice.commons.exception.ApplicationException;
import com.team11.auth.authservice.commons.exception.TokenException;
import com.team11.auth.authservice.commons.exception.payload.ErrorStatus;
import com.team11.auth.authservice.persistence.domain.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;

@Component
public class TokenProvider {

    private final SecretKey secretKey;
    private final TokenService tokenService;

    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000;
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60L * 24;

    @Autowired
    public TokenProvider(TokenService tokenService, @Value("${jwt.secret}") String key) {
        this.tokenService = tokenService;
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateAccessToken(ReadUserResponse user) {
        return generateToken(user, ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String generateRefreshToken(ReadUserResponse user, String accessToken) {
        String refreshToken = generateToken(user, REFRESH_TOKEN_EXPIRE_TIME);
        return tokenService.saveOrUpdate(user.uid(), refreshToken, accessToken);
    }

    private String generateToken(ReadUserResponse user, long expireTime) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .subject(user.uid())
                .issuedAt(now)
                .expiration(expiredDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public Token validRefreshToken(String accessToken, String refreshToken) {

        if (StringUtils.hasText(accessToken)) {
            Token token = tokenService.findByAccessTokenOrThrow(accessToken);

            if(refreshToken.equals(token.getRefreshToken()) && validateToken(refreshToken)) {
                return token;
            }
        }

        throw new ApplicationException(
                ErrorStatus.toErrorStatus("유효하지 않은 리프레쉬 토큰입니다.", 401, LocalDateTime.now())
        );
    }

    public String reissueAccessToken(Token token, ReadUserResponse user) {

        String reissueAccessToken = generateAccessToken(user);
        tokenService.updateToken(reissueAccessToken ,token);

        return reissueAccessToken;
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        Claims claims = parseClaims(token);
        return claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (MalformedJwtException e) {
            throw new TokenException(ErrorStatus.
                            toErrorStatus("INVALID_TOKEN", 401, LocalDateTime.now()
                    ));
        } catch (SecurityException e) {
            throw new TokenException(ErrorStatus.
                            toErrorStatus("INVALID_JWT_SIGNATURE", 401, LocalDateTime.now()
                    ));
        }
    }

    public String getUidFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }
}
