package com.team11.backend.domain.recommendation.repository;

import com.team11.backend.domain.recommendation.entity.Recommendation; // 엔티티 패키지 경로 확인
import com.team11.backend.domain.resource.entity.Resource; // Resource 엔티티 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    // 특정 Resource 객체로 Recommendation 목록 조회
    List<Recommendation> findByResource(Resource resource);
}
