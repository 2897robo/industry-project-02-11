package com.team11.backend.domain.resource.service;

import com.team11.backend.domain.resource.entity.Resource;
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
                .userId(request.getUserId())
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
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));
        return ResourceDto.Response.from(resource);
    }

    // 사용자 ID로 리소스 목록 조회
    public List<ResourceDto.Response> getResourcesByUserId(Long userId) {
        return resourceRepository.findByUserId(userId).stream() // findByUserId 사용
                .map(ResourceDto.Response::from)
                .collect(Collectors.toList());
    }

    // AWS 리소스 ID로 리소스 조회
    public ResourceDto.Response getResourceByAwsResourceId(String awsResourceId) {
        Resource resource = resourceRepository.findByAwsResourceId(awsResourceId) // findByAwsResourceId 사용
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with AWS resource id: " + awsResourceId, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));
        return ResourceDto.Response.from(resource);
    }

    // 리소스 업데이트
    @Transactional
    public ResourceDto.Response updateResource(Long id, ResourceDto.UpdateRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now())));

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

    // 리소스 삭제
    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ApplicationException(ErrorStatus.toErrorStatus("Resource not found with id: " + id, HttpStatus.NOT_FOUND.value(), LocalDateTime.now()));
        }
        resourceRepository.deleteById(id);
    }
}
