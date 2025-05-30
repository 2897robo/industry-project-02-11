package com.team11.auth.authservice.infrastructure.adapter;

import com.team11.auth.authservice.application.dto.ReadUserResponse;
import com.team11.auth.authservice.infrastructure.fallback.UserFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", fallback = UserFallBack.class)
public interface UserAdapter {

    @GetMapping("/users/login")
    ReadUserResponse findByUidAndPassword(@RequestParam("uid") String uid, @RequestParam("password") String password);

    @GetMapping("/users/{uid}")
    ReadUserResponse findByUid(@PathVariable("uid") String uid);
}
