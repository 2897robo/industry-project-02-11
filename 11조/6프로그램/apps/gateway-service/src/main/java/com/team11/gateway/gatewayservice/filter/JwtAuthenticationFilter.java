//package com.team11.gateway.gatewayservice.filter;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import javax.crypto.SecretKey;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
//@Slf4j
//@Component
//public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
//
//    @Value("${jwt.secret}")
//    private String jwtSecret;
//
//    private static final List<String> EXCLUDED_PATHS = List.of(
//            "/auth-service/auth/login",
//            "/auth-service/auth/refresh-token",
//            "/user-service/users/check",
//            "/user-service/users"  // POST 회원가입
//    );
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getURI().getPath();
//        String method = request.getMethod().toString();
//
//        // 인증 제외 경로 체크
//        if (isExcludedPath(path, method)) {
//            return chain.filter(exchange);
//        }
//
//        // Authorization 헤더 확인
//        String authHeader = request.getHeaders().getFirst("Authorization");
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
//        }
//
//        // JWT 토큰 추출
//        String token = authHeader.substring(7);
//
//        try {
//            // 토큰 검증
//            Claims claims = validateToken(token);
//            String userUid = claims.getSubject();
//
//            // 검증된 사용자 정보를 헤더에 추가
//            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
//                    .header("X-User-Id", userUid)
//                    .build();
//
//            return chain.filter(exchange.mutate().request(modifiedRequest).build());
//
//        } catch (Exception e) {
//            log.error("JWT validation failed: {}", e.getMessage());
//            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
//        }
//    }
//
//    private boolean isExcludedPath(String path, String method) {
//        // 회원가입은 POST 메서드일 때만 제외
//        if (path.equals("/user-service/users") && method.equals("POST")) {
//            return true;
//        }
//
//        // 나머지 제외 경로
//        return EXCLUDED_PATHS.stream()
//                .anyMatch(excludedPath -> path.startsWith(excludedPath));
//    }
//
//    private Claims validateToken(String token) {
//        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(httpStatus);
//        log.error("JWT Authentication Error: {}", err);
//        return response.setComplete();
//    }
//
//    @Override
//    public int getOrder() {
//        return -100;
//    }
//}
