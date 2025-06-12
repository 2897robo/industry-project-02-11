package com.team11.backend.domain.recommendation.controller;

import com.team11.backend.domain.recommendation.dto.RecommendationLogDto;
import com.team11.backend.domain.recommendation.service.RecommendationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendation-logs")
@RequiredArgsConstructor
public class RecommendationLogController {

    private final RecommendationLogService recommendationLogService;

    // 추천 로그 생성 (Create)
    @PostMapping
    public ResponseEntity<RecommendationLogDto.Response> createRecommendationLog(@RequestBody RecommendationLogDto.CreateRequest request) {
        RecommendationLogDto.Response response = recommendationLogService.createRecommendationLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 추천 로그 조회 (Read all)
    @GetMapping
    public ResponseEntity<List<RecommendationLogDto.Response>> getAllRecommendationLogs() {
        List<RecommendationLogDto.Response> responses = recommendationLogService.getAllRecommendationLogs();
        return ResponseEntity.ok(responses);
    }

    // ID로 추천 로그 조회 (Read by ID)
    @GetMapping("/{id}")
    public ResponseEntity<RecommendationLogDto.Response> getRecommendationLogById(@PathVariable Long id) {
        RecommendationLogDto.Response response = recommendationLogService.getRecommendationLogById(id);
        return ResponseEntity.ok(response);
    }

    // 특정 Recommendation ID로 로그 목록 조회 (Read by Recommendation ID)
    @GetMapping("/by-recommendation/{recommendationId}")
    public ResponseEntity<List<RecommendationLogDto.Response>> getRecommendationLogsByRecommendationId(@PathVariable Long recommendationId) {
        List<RecommendationLogDto.Response> responses = recommendationLogService.getRecommendationLogsByRecommendationId(recommendationId);
        return ResponseEntity.ok(responses);
    }

    // 추천 로그 삭제 (Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendationLog(@PathVariable Long id) {
        recommendationLogService.deleteRecommendationLog(id);
        return ResponseEntity.noContent().build();
    }
}
