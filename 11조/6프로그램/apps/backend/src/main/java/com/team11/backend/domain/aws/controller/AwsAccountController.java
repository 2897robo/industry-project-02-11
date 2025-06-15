package com.team11.backend.domain.aws.controller;

import com.team11.backend.domain.aws.dto.AwsAccountDto;
import com.team11.backend.domain.aws.service.AwsAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aws-accounts")
@RequiredArgsConstructor
public class AwsAccountController {

    private final AwsAccountService awsAccountService;

    @PostMapping
    public ResponseEntity<AwsAccountDto.Response> createAwsAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AwsAccountDto.CreateRequest request) {
        
        String userUid = userDetails.getUsername();
        AwsAccountDto.Response response = awsAccountService.createAwsAccount(userUid, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AwsAccountDto.Response>> getMyAwsAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userUid = userDetails.getUsername();
        List<AwsAccountDto.Response> accounts = awsAccountService.getMyAwsAccounts(userUid);
        
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AwsAccountDto.Response> getAwsAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long accountId) {
        
        String userUid = userDetails.getUsername();
        AwsAccountDto.Response account = awsAccountService.getAwsAccount(userUid, accountId);
        
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<Void> updateAwsAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long accountId,
            @RequestBody AwsAccountDto.UpdateRequest request) {
        
        String userUid = userDetails.getUsername();
        awsAccountService.updateAwsAccount(userUid, accountId, request);
        
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deactivateAwsAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long accountId) {
        
        String userUid = userDetails.getUsername();
        awsAccountService.deactivateAwsAccount(userUid, accountId);
        
        return ResponseEntity.noContent().build();
    }
}
