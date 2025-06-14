package com.team11.backend.domain.recommendation.controller;

import com.team11.backend.domain.recommendation.dto.RecommendationDto;
import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.recommendation.service.CostOptimizationService;
import com.team11.backend.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CostOptimizationService costOptimizationService;

    // 내 추천 목록 조회
    @GetMapping
    public ResponseEntity<List<RecommendationDto.Response>> getMyRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "pending") String status) {
        
        String userUid = userDetails.getUsername();
        List<RecommendationDto.Response> recommendations;
        
        if ("all".equals(status)) {
            recommendations = recommendationService.getRecommendationsByUserUid(userUid);
        } else {
            recommendations = recommendationService.getPendingRecommendationsByUserUid(userUid);
        }
        
        return ResponseEntity.ok(recommendations);
    }

    // 추천 생성 (모든 리소스 분석)
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userUid = userDetails.getUsername();
        List<Recommendation> recommendations = costOptimizationService.generateRecommendations(userUid);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "비용 최적화 추천이 생성되었습니다.");
        response.put("count", recommendations.size());
        response.put("totalExpectedSaving", calculateTotalSaving(recommendations));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 특정 리소스에 대한 추천 재생성
    @PostMapping("/generate/resource/{resourceId}")
    public ResponseEntity<List<RecommendationDto.Response>> regenerateForResource(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long resourceId) {
        
        String userUid = userDetails.getUsername();
        List<Recommendation> recommendations = costOptimizationService.regenerateRecommendationsForResource(userUid, resourceId);
        
        List<RecommendationDto.Response> responses = recommendations.stream()
                .map(RecommendationDto.Response::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    // 추천 요약 정보 조회
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getRecommendationSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String userUid = userDetails.getUsername();
        
        List<RecommendationDto.Response> pendingRecommendations = 
            recommendationService.getPendingRecommendationsByUserUid(userUid);
        
        Float totalSaving = recommendationService.getTotalExpectedSaving(userUid);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRecommendations", pendingRecommendations.size());
        summary.put("totalExpectedSaving", totalSaving != null ? totalSaving : 0.0f);
        summary.put("currency", "USD");
        summary.put("monthlyProjectedSaving", (totalSaving != null ? totalSaving : 0.0f) * 30);
        
        return ResponseEntity.ok(summary);
    }

    // 특정 추천 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<RecommendationDto.Response> getRecommendationById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        String userUid = userDetails.getUsername();
        // 권한 확인은 서비스 레이어에서 처리
        RecommendationDto.Response response = recommendationService.getRecommendationById(id);
        return ResponseEntity.ok(response);
    }

    // 추천 상태 업데이트 (수락/거절)
    @PutMapping("/{id}/status")
    public ResponseEntity<RecommendationDto.Response> updateRecommendationStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        
        String userUid = userDetails.getUsername();
        String newStatus = statusUpdate.get("status");
        
        if (!"accepted".equals(newStatus) && !"ignored".equals(newStatus)) {
            throw new IllegalArgumentException("Invalid status. Must be 'accepted' or 'ignored'");
        }
        
        RecommendationDto.UpdateRequest request = RecommendationDto.UpdateRequest.builder()
                .status(newStatus)
                .build();
        
        RecommendationDto.Response response = recommendationService.updateRecommendation(id, request);
        return ResponseEntity.ok(response);
    }

    // 특정 리소스의 추천 목록 조회
    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<RecommendationDto.Response>> getRecommendationsByResourceId(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long resourceId) {
        
        String userUid = userDetails.getUsername();
        List<RecommendationDto.Response> responses = recommendationService.getRecommendationsByResourceId(resourceId);
        return ResponseEntity.ok(responses);
    }

    private Float calculateTotalSaving(List<Recommendation> recommendations) {
        return recommendations.stream()
                .map(Recommendation::getExpectedSaving)
                .filter(saving -> saving != null)
                .reduce(0.0f, Float::sum);
    }
}
