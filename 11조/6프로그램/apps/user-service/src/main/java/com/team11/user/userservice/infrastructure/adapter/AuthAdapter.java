package com.team11.user.userservice.infrastructure.adapter;

import com.team11.user.userservice.infrastructure.fallback.AuthFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "auth-service", fallback = AuthFallBack.class)
public interface AuthAdapter {

    @PostMapping("/auth/refresh-token")
    String loginByRefreshToken();
}