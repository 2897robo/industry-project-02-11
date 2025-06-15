package com.team11.backend.domain.resource.service;

import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.dto.ResourceDto;
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
public class ResourceService {

    private final ResourceRepository resourceRepository;

    // 리소스 생성
    @Transactional
    public ResourceDto.Response createResource(ResourceDto.CreateRequest request) {
        Resource resource = Resource.builder()
                .userUid(request.getUserUid())
                .awsResourceId(request.getAwsResourceId())
                .serviceType(request.getServiceType())
                .region(request.getRegion())
                .isIdle(request.getIsIdle())
                .usageRate(request.getUsageRate())
                .costUsd(request.getCostUsd())
                .lastCheckedAt(request.getLastCheckedAt())
                .build();
        Resource savedResource = resourceRepository.save(resource);
        return ResourceDto.Response.from(savedResource);
    }

    // 모든 리소스 조회
    public List<ResourceDto.Response> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(ResourceDto.Response::from)
                .collect(Collectors.toList());
    }

    // ID로 리소스 조회
    public ResourceDto.Response getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("리소스를 찾을 수 없습니다: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())
                ));
        return ResourceDto.Response.from(resource);
    }

    // 사용자의 모든 리소스 조회
    public List<ResourceDto.Response> getResourcesByUserUid(String userUid) {
        return resourceRepository.findByUserUid(userUid).stream()
                .map(ResourceDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 서비스 타입별 리소스 조회
    public List<ResourceDto.Response> getResourcesByUserUidAndServiceType(String userUid, AwsServiceType serviceType) {
        return resourceRepository.findByUserUidAndServiceType(userUid, serviceType).stream()
                .map(ResourceDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 유휴 리소스만 조회
    public List<ResourceDto.Response> getIdleResourcesByUserUid(String userUid) {
        return resourceRepository.findByUserUidAndIsIdleTrue(userUid).stream()
                .map(ResourceDto.Response::from)
                .collect(Collectors.toList());
    }

    // AWS 리소스 ID로 리소스 조회
    public ResourceDto.Response getResourceByAwsResourceId(String awsResourceId) {
        Resource resource = resourceRepository.findByAwsResourceId(awsResourceId)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("AWS 리소스를 찾을 수 없습니다: " + awsResourceId, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())
                ));
        return ResourceDto.Response.from(resource);
    }

    // 리소스 업데이트 (사용자 확인 포함)
    @Transactional
    public ResourceDto.Response updateResource(String userUid, Long id, ResourceDto.UpdateRequest request) {
        Resource resource = resourceRepository.findByIdAndUserUid(id, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("리소스를 찾을 수 없거나 권한이 없습니다: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())
                ));

        resource.update(
                request.getServiceType(),
                request.getRegion(),
                request.getIsIdle(),
                request.getUsageRate(),
                request.getCostUsd(),
                request.getLastCheckedAt()
        );
        return ResourceDto.Response.from(resource);
    }

    // 리소스 삭제 (사용자 확인 포함)
    @Transactional
    public void deleteResource(String userUid, Long id) {
        Resource resource = resourceRepository.findByIdAndUserUid(id, userUid)
                .orElseThrow(() -> new ApplicationException(
                    ErrorStatus.toErrorStatus("리소스를 찾을 수 없거나 권한이 없습니다: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())
                ));
        resourceRepository.delete(resource);
    }
}
