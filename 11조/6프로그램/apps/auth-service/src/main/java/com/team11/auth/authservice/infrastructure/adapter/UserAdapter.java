package com.team11.auth.authservice.infrastructure.adapter;

import com.team11.auth.authservice.application.dto.ReadUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "user-service")
public interface UserAdapter {

    @PostMapping("/users")
    ReadUserResponse findByUidAndPassword(String uid, String password);

    @GetMapping("/users/{uid}")
    ReadUserResponse findByUid(@PathVariable("uid") String uid);
}
