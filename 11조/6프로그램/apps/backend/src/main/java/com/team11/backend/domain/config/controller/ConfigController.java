package com.team11.backend.domain.config.controller;

import com.team11.backend.domain.config.dto.request.CreateConfigRequest;
import com.team11.backend.domain.config.dto.request.UpdateConfigRequest;
import com.team11.backend.domain.config.dto.response.ReadConfigResponse;
import com.team11.backend.domain.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config")
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public ResponseEntity<ReadConfigResponse> getByUserId(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(configService.getByUserId(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Long> create(@AuthenticationPrincipal UserDetails user, @RequestBody CreateConfigRequest request) {
        return ResponseEntity.ok(configService.createConfig(user.getUsername(), request));
    }

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody UpdateConfigRequest request) {
        configService.updateConfig(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails user) {
        configService.deleteByUserId(user.getUsername());
        return ResponseEntity.ok().build();
    }
}
