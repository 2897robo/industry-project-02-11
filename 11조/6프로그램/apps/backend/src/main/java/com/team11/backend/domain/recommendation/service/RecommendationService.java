package com.team11.backend.domain.recommendation.service;

import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.recommendation.dto.RecommendationDto;
import com.team11.backend.domain.recommendation.repository.RecommendationRepository;
import com.team11.backend.domain.resource.repository.ResourceRepository;
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
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public RecommendationDto.Response createRecommendation(RecommendationDto.CreateRequest request) {
        // Resource 엔티티 조회 (resourceId를 통해)
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with id: " + request.getResourceId(), HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

        Recommendation recommendation = Recommendation.builder()
                .resource(resource) // Resource 객체 주입
                .recommendationText(request.getRecommendationText())
                .expectedSaving(request.getExpectedSaving())
                .status(request.getStatus())
                .build();
        Recommendation savedRecommendation = recommendationRepository.save(recommendation);
        return RecommendationDto.Response.from(savedRecommendation);
    }

    // 모든 추천 조회
    public List<RecommendationDto.Response> getAllRecommendations() {
        return recommendationRepository.findAll().stream()
                .map(RecommendationDto.Response::from)
                .collect(Collectors.toList());
    }

    // ID로 추천 조회
    public RecommendationDto.Response getRecommendationById(Long id) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Recommendation not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));
        return RecommendationDto.Response.from(recommendation);
    }

    // 특정 Resource ID로 추천 목록 조회
    public List<RecommendationDto.Response> getRecommendationsByResourceId(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with id: " + resourceId, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

        return recommendationRepository.findByResource(resource).stream() // findByResource 사용
                .map(RecommendationDto.Response::from)
                .collect(Collectors.toList());
    }

    // 추천 업데이트
    @Transactional
    public RecommendationDto.Response updateRecommendation(Long id, RecommendationDto.UpdateRequest request) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Recommendation not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

        recommendation.update(request.getRecommendationText(), request.getExpectedSaving(), request.getStatus()); // 엔티티의 update 메소드 사용
        // save를 호출하지 않아도 @Transactional 어노테이션에 의해 변경 감지(Dirty Checking)가 되어 자동으로 업데이트됩니다.
        return RecommendationDto.Response.from(recommendation);
    }

    // 추천 삭제
    @Transactional
    public void deleteRecommendation(Long id) {
        if (!recommendationRepository.existsById(id)) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("Recommendation not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now()));
        }
        recommendationRepository.deleteById(id);
    }

    // 사용자의 모든 추천 조회
    public List<RecommendationDto.Response> getRecommendationsByUserUid(String userUid) {
        return recommendationRepository.findByUserUid(userUid).stream()
                .map(RecommendationDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 pending 상태 추천만 조회
    public List<RecommendationDto.Response> getPendingRecommendationsByUserUid(String userUid) {
        return recommendationRepository.findPendingByUserUid(userUid).stream()
                .map(RecommendationDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 총 예상 절감액 계산
    public Float getTotalExpectedSaving(String userUid) {
        Float totalSaving = recommendationRepository.calculateTotalExpectedSavingByUserUid(userUid);
        return totalSaving != null ? totalSaving : 0.0f;
    }
}
