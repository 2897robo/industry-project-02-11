package com.team11.backend.domain.recommendation.controller;

import com.team11.backend.domain.recommendation.dto.RecommendationDto;
import com.team11.backend.domain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // 추천 생성 (Create)
    @PostMapping
    public ResponseEntity<RecommendationDto.Response> createRecommendation(@RequestBody RecommendationDto.CreateRequest request) {
        RecommendationDto.Response response = recommendationService.createRecommendation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 추천 조회 (Read all)
    @GetMapping
    public ResponseEntity<List<RecommendationDto.Response>> getAllRecommendations() {
        List<RecommendationDto.Response> responses = recommendationService.getAllRecommendations();
        return ResponseEntity.ok(responses);
    }

    // ID로 추천 조회 (Read by ID)
    @GetMapping("/{id}")
    public ResponseEntity<RecommendationDto.Response> getRecommendationById(@PathVariable Long id) {
        RecommendationDto.Response response = recommendationService.getRecommendationById(id);
        return ResponseEntity.ok(response);
    }

    // 특정 Resource ID로 추천 목록 조회 (Read by Resource ID)
    @GetMapping("/by-resource/{resourceId}")
    public ResponseEntity<List<RecommendationDto.Response>> getRecommendationsByResourceId(@PathVariable Long resourceId) {
        List<RecommendationDto.Response> responses = recommendationService.getRecommendationsByResourceId(resourceId);
        return ResponseEntity.ok(responses);
    }

    // 추천 업데이트 (Update)
    @PutMapping("/{id}")
    public ResponseEntity<RecommendationDto.Response> updateRecommendation(
            @PathVariable Long id,
            @RequestBody RecommendationDto.UpdateRequest request) {
        RecommendationDto.Response response = recommendationService.updateRecommendation(id, request);
        return ResponseEntity.ok(response);
    }

    // 추천 삭제 (Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationService.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }
}
