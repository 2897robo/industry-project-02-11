package com.team11.backend.controller;

import com.team11.backend.service.UserService;
import com.team11.backend.service.dto.ReadUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{uid}")
    public ResponseEntity<ReadUserResponse> findByUid(@PathVariable("uid") String uid) {
        return ResponseEntity.ok(userService.getByUid(uid));
    }
}
