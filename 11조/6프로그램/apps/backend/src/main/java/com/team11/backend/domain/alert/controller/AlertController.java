package com.team11.backend.domain.alert.controller;

import com.team11.backend.domain.alert.dto.request.CreateAlertRequest;
import com.team11.backend.domain.alert.dto.response.ReadAlertResponse;
import com.team11.backend.domain.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Transactional
@RequestMapping("/alert")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<ReadAlertResponse>> getAlertResponse(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.getByUserUid(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Void> createAlert(@RequestBody CreateAlertRequest request) {
        alertService.createAlert(request);
        return ResponseEntity.noContent().build();
    }
}
