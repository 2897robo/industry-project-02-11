package com.team11.backend.domain.aws.controller;

import com.team11.backend.domain.aws.service.AwsCloudWatchService;
import com.team11.backend.domain.aws.service.AwsCostExplorerService;
import com.team11.backend.domain.aws.service.AwsResourceCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/aws-data")
@RequiredArgsConstructor
public class AwsDataCollectionController {

    private final AwsResourceCollectorService resourceCollectorService;
    private final AwsCostExplorerService costExplorerService;
    private final AwsCloudWatchService cloudWatchService;

    // 특정 AWS 계정의 리소스 수집 (수동 트리거)
    @PostMapping("/collect-resources/{awsAccountId}")
    public ResponseEntity<Map<String, String>> collectResources(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long awsAccountId) {
        
        String userUid = userDetails.getUsername();
        resourceCollectorService.collectResourcesForAccount(userUid, awsAccountId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "리소스 수집이 시작되었습니다. 잠시 후 확인해주세요.");
        
        return ResponseEntity.ok(response);
    }

    // 특정 AWS 계정의 비용 데이터 수집 (수동 트리거)
    @PostMapping("/collect-costs/{awsAccountId}")
    public ResponseEntity<Map<String, String>> collectCosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long awsAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String userUid = userDetails.getUsername();
        
        if (startDate == null || endDate == null) {
            // 기본값: 현재 월의 비용 데이터
            costExplorerService.collectMonthlyCostData(userUid, awsAccountId);
        } else {
            costExplorerService.collectCostDataForAccount(userUid, awsAccountId, startDate, endDate);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "비용 데이터 수집이 시작되었습니다. 잠시 후 확인해주세요.");
        
        return ResponseEntity.ok(response);
    }

    // 특정 AWS 계정의 리소스 메트릭 업데이트 (수동 트리거)
    @PostMapping("/update-metrics/{awsAccountId}")
    public ResponseEntity<Map<String, String>> updateMetrics(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long awsAccountId) {
        
        String userUid = userDetails.getUsername();
        cloudWatchService.updateResourceMetrics(userUid, awsAccountId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "리소스 메트릭 업데이트가 시작되었습니다. 잠시 후 확인해주세요.");
        
        return ResponseEntity.ok(response);
    }

    // 전체 데이터 수집 (리소스 + 비용 + 메트릭)
    @PostMapping("/collect-all/{awsAccountId}")
    public ResponseEntity<Map<String, String>> collectAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long awsAccountId) {
        
        String userUid = userDetails.getUsername();
        
        // 비동기로 모든 데이터 수집 시작
        resourceCollectorService.collectResourcesForAccount(userUid, awsAccountId);
        costExplorerService.collectMonthlyCostData(userUid, awsAccountId);
        cloudWatchService.updateResourceMetrics(userUid, awsAccountId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "전체 데이터 수집이 시작되었습니다. 완료까지 몇 분이 소요될 수 있습니다.");
        
        return ResponseEntity.ok(response);
    }
}
