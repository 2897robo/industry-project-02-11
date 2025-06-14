package com.team11.backend.domain.cost.controller;

import com.team11.backend.domain.cost.dto.CostHistoryDto;
import com.team11.backend.domain.cost.service.CostHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cost-history")
@RequiredArgsConstructor
public class CostHistoryController {

    private final CostHistoryService costHistoryService;

    // 특정 기간 비용 이력 조회
    @GetMapping
    public ResponseEntity<List<CostHistoryDto.Response>> getCostHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String userUid = userDetails.getUsername();
        List<CostHistoryDto.Response> costHistory = costHistoryService.getCostHistory(userUid, startDate, endDate);
        
        return ResponseEntity.ok(costHistory);
    }

    // 특정 AWS 계정의 비용 이력 조회
    @GetMapping("/aws-account/{awsAccountId}")
    public ResponseEntity<List<CostHistoryDto.Response>> getCostHistoryByAwsAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long awsAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String userUid = userDetails.getUsername();
        List<CostHistoryDto.Response> costHistory = costHistoryService.getCostHistoryByAwsAccount(
                userUid, awsAccountId, startDate, endDate);
        
        return ResponseEntity.ok(costHistory);
    }

    // 서비스별 비용 요약
    @GetMapping("/service-summary")
    public ResponseEntity<List<CostHistoryDto.ServiceCostSummary>> getServiceCostSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String userUid = userDetails.getUsername();
        List<CostHistoryDto.ServiceCostSummary> summary = costHistoryService.getServiceCostSummary(
                userUid, startDate, endDate);
        
        return ResponseEntity.ok(summary);
    }

    // 일별 비용 추이
    @GetMapping("/daily-trend")
    public ResponseEntity<CostHistoryDto.CostTrend> getDailyCostTrend(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        String userUid = userDetails.getUsername();
        CostHistoryDto.CostTrend trend = costHistoryService.getDailyCostTrend(userUid, startDate, endDate);
        
        return ResponseEntity.ok(trend);
    }

    // 현재 월 비용 요약
    @GetMapping("/current-month")
    public ResponseEntity<CostHistoryDto.MonthlyCostSummary> getCurrentMonthSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userUid = userDetails.getUsername();
        CostHistoryDto.MonthlyCostSummary summary = costHistoryService.getCurrentMonthSummary(userUid);
        
        return ResponseEntity.ok(summary);
    }

    // 월별 비용 추이 (최근 N개월)
    @GetMapping("/monthly-trend")
    public ResponseEntity<List<CostHistoryDto.MonthlyCostSummary>> getMonthlyTrend(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "6") int months) {
        
        String userUid = userDetails.getUsername();
        List<CostHistoryDto.MonthlyCostSummary> trend = costHistoryService.getMonthlyTrend(userUid, months);
        
        return ResponseEntity.ok(trend);
    }
}
