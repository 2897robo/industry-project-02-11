package com.team11.backend.domain.resource.repository;

import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    // 특정 사용자의 모든 리소스 조회
    List<Resource> findByUserUid(String userUid);

    // 특정 사용자의 서비스 타입별 리소스 조회
    List<Resource> findByUserUidAndServiceType(String userUid, AwsServiceType serviceType);

    // 특정 사용자의 유휴 리소스만 조회
    List<Resource> findByUserUidAndIsIdleTrue(String userUid);

    // 특정 AWS 리소스 ID로 Resource 조회
    Optional<Resource> findByAwsResourceId(String awsResourceId);

    // 특정 사용자와 AWS 리소스 ID로 조회
    Optional<Resource> findByUserUidAndAwsResourceId(String userUid, String awsResourceId);

    // 특정 사용자의 리소스 ID로 조회
    Optional<Resource> findByIdAndUserUid(Long id, String userUid);
}
