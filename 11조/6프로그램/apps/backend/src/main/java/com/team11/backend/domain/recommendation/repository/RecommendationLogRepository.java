package com.team11.backend.domain.recommendation.repository;

import com.team11.backend.domain.recommendation.entity.Recommendation; // Recommendation 엔티티 임포트
import com.team11.backend.domain.recommendation.entity.RecommendationLog; // 엔티티 패키지 경로 확인
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {
    // 특정 Recommendation 객체로 RecommendationLog 목록 조회
    List<RecommendationLog> findByRecommendation(Recommendation recommendation);
}
