package com.team11.user.userservice.presentation.controller;

import com.team11.user.userservice.application.service.UserService;
import com.team11.user.userservice.presentation.dto.request.CreateUserRequest;
import com.team11.user.userservice.presentation.dto.response.ReadUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/login")
    public ResponseEntity<ReadUserResponse> findByUidAndPassword(@RequestParam("uid") String uid, @RequestParam("password") String password) {
        return ResponseEntity.ok(userService.getByUidAndPassword(uid, password));
    }

    @GetMapping
    public ResponseEntity<ReadUserResponse> findByUid(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(userService.getByUid(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CreateUserRequest request) {
        userService.createUser(request);
        return ResponseEntity.noContent().build();
    }
}
