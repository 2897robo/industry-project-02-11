package com.team11.backend.domain.resource.repository;

import com.team11.backend.domain.resource.entity.Resource; // 엔티티 패키지 경로 확인
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    // 특정 사용자 ID로 Resource 목록 조회
    List<Resource> findByUserId(Long userId);

    // 특정 AWS 리소스 ID로 Resource 조회
    Optional<Resource> findByAwsResourceId(String awsResourceId);
}
