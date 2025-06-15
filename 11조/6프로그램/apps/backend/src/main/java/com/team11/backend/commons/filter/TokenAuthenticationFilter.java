package com.team11.backend.commons.filter;

import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.TokenException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import com.team11.backend.infrastructure.adapter.AuthAdapter;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_PREFIX = "Bearer ";
    private final SecretKey secretKey;
    private final AuthAdapter authAdapter;

    public TokenAuthenticationFilter(@Value("${jwt.secret}") String key, AuthAdapter authAdapter) {
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes());
        this.authAdapter = authAdapter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if(shouldSkipFilter(uri)){
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = resolveToken(request);
        String refreshToken = resolveRefreshToken(request);

        if (validateToken(accessToken)) {
            setAuthentication(accessToken);
        } else if(validateToken(refreshToken)) {

            String reissueAccessToken;

            try {
                reissueAccessToken = authAdapter.loginByRefreshToken();
            } catch (FeignException e) {
                throw new TokenException(ErrorStatus.toErrorStatus(e.getMessage(), 401, LocalDateTime.now()));
            }

            if(StringUtils.hasText(reissueAccessToken)){
                setAuthentication(reissueAccessToken);
                response.setHeader(AUTHORIZATION, TOKEN_PREFIX + reissueAccessToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(String uri) {
        AntPathMatcher pathMatcher = new AntPathMatcher();

        List<String> skippedUris = Arrays.asList(
                "/favicon.ico",
                "/error",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/users/login",
                "/users/check"
        );

        for (String skippedUri : skippedUris) {
            if (pathMatcher.match(skippedUri, uri)) {
                return true;
            }
        }

        if(uri.equals("/user") && "POST".equalsIgnoreCase(getRequestMethod())) {
            return true;
        }

        return false;
    }

    private void setAuthentication(String accessToken) {
        Authentication authentication = getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION);
        if (ObjectUtils.isEmpty(token) || !token.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        return token.substring(TOKEN_PREFIX.length());
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for(Cookie cookie : cookies) {
                if(cookie.getName().equals("refresh_token")) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        User principal = new User(claims.getSubject(), "", Collections.emptyList());
        return new UsernamePasswordAuthenticationToken(principal, token, Collections.emptyList());
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

    private boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        Claims claims = parseClaims(token);
        return claims.getExpiration().after(new Date());
    }

    private String getRequestMethod() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs.getRequest().getMethod();
    }
}

