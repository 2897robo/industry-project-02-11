package com.team11.backend.domain.recommendation.service;

import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.recommendation.entity.RecommendationLog;
import com.team11.backend.domain.recommendation.dto.RecommendationLogDto;
import com.team11.backend.domain.recommendation.repository.RecommendationLogRepository;
import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
import com.team11.backend.commons.exception.ApplicationException;
import com.team11.backend.commons.exception.payload.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationLogService {

    private final RecommendationLogRepository recommendationLogRepository;
    private final RecommendationRepository recommendationRepository;

    // 추천 로그 생성
    @Transactional
    public RecommendationLogDto.Response createRecommendationLog(RecommendationLogDto.CreateRequest request) {
        // Recommendation 엔티티 조회 (recommendationId를 통해)
        Recommendation recommendation = recommendationRepository.findById(request.getRecommendationId())
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Recommendation not found with id: " + request.getRecommendationId(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

        RecommendationLog log = RecommendationLog.builder()
                .recommendation(recommendation)
                .userId(request.getUserId())
                .action(request.getAction())
                .reason(request.getReason())
                .build();
        RecommendationLog savedLog = recommendationLogRepository.save(log);
        return RecommendationLogDto.Response.from(savedLog);
    }

    // 모든 추천 로그 조회
    public List<RecommendationLogDto.Response> getAllRecommendationLogs() {
        return recommendationLogRepository.findAll().stream()
                .map(RecommendationLogDto.Response::from)
                .collect(Collectors.toList());
    }

    // ID로 추천 로그 조회
    public RecommendationLogDto.Response getRecommendationLogById(Long id) {
        RecommendationLog log = recommendationLogRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("RecommendationLog not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));
        return RecommendationLogDto.Response.from(log);
    }

    // 특정 Recommendation ID로 로그 목록 조회
    public List<RecommendationLogDto.Response> getRecommendationLogsByRecommendationId(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Recommendation not found with id: " + recommendationId, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

        return recommendationLogRepository.findByRecommendation(recommendation).stream() // findByRecommendation 사용
                .map(RecommendationLogDto.Response::from)
                .collect(Collectors.toList());
    }

    // 추천 로그 삭제
    @Transactional
    public void deleteRecommendationLog(Long id) {
        if (!recommendationLogRepository.existsById(id)) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("RecommendationLog not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now()));
        }
        recommendationLogRepository.deleteById(id);
    }
}
