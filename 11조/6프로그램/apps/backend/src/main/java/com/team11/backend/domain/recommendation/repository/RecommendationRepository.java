package com.team11.backend.domain.recommendation.repository;

import com.team11.backend.domain.recommendation.entity.Recommendation;
import com.team11.backend.domain.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    // 특정 Resource 객체로 Recommendation 목록 조회
    List<Recommendation> findByResource(Resource resource);
    
    // 특정 Resource ID로 Recommendation 목록 조회
    List<Recommendation> findByResourceId(Long resourceId);
    
    // 특정 상태의 Recommendation 목록 조회
    List<Recommendation> findByStatus(String status);
    
    // 특정 사용자의 모든 추천 조회 (Resource와 조인)
    @Query("SELECT r FROM Recommendation r JOIN r.resource res WHERE res.userUid = :userUid")
    List<Recommendation> findByUserUid(@Param("userUid") String userUid);
    
    // 특정 사용자의 pending 상태 추천만 조회
    @Query("SELECT r FROM Recommendation r JOIN r.resource res WHERE res.userUid = :userUid AND r.status = 'pending'")
    List<Recommendation> findPendingByUserUid(@Param("userUid") String userUid);
    
    // 특정 사용자의 총 예상 절감액 계산
    @Query("SELECT SUM(r.expectedSaving) FROM Recommendation r JOIN r.resource res WHERE res.userUid = :userUid AND r.status = 'pending'")
    Float calculateTotalExpectedSavingByUserUid(@Param("userUid") String userUid);
}
