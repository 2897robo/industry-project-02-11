package com.team11.backend.domain.resource.controller;

import com.team11.backend.domain.resource.dto.ResourceDto;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import com.team11.backend.domain.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    // 내 리소스 목록 조회
    @GetMapping
    public ResponseEntity<List<ResourceDto.Response>> getMyResources(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userUid = userDetails.getUsername();
        List<ResourceDto.Response> responses = resourceService.getResourcesByUserUid(userUid);
        return ResponseEntity.ok(responses);
    }

    // 서비스 타입별 리소스 조회
    @GetMapping("/by-service-type/{serviceType}")
    public ResponseEntity<List<ResourceDto.Response>> getResourcesByServiceType(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable AwsServiceType serviceType) {
        String userUid = userDetails.getUsername();
        List<ResourceDto.Response> responses = resourceService.getResourcesByUserUidAndServiceType(userUid, serviceType);
        return ResponseEntity.ok(responses);
    }

    // 유휴 리소스만 조회
    @GetMapping("/idle")
    public ResponseEntity<List<ResourceDto.Response>> getIdleResources(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userUid = userDetails.getUsername();
        List<ResourceDto.Response> responses = resourceService.getIdleResourcesByUserUid(userUid);
        return ResponseEntity.ok(responses);
    }

    // 특정 리소스 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDto.Response> getResourceById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        String userUid = userDetails.getUsername();
        // 권한 체크는 서비스 레이어에서 처리
        ResourceDto.Response response = resourceService.getResourceById(id);
        return ResponseEntity.ok(response);
    }

    // 리소스 업데이트 (관리자용)
    @PutMapping("/{id}")
    public ResponseEntity<ResourceDto.Response> updateResource(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody ResourceDto.UpdateRequest request) {
        String userUid = userDetails.getUsername();
        ResourceDto.Response response = resourceService.updateResource(userUid, id, request);
        return ResponseEntity.ok(response);
    }

    // 리소스 삭제 (관리자용)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        String userUid = userDetails.getUsername();
        resourceService.deleteResource(userUid, id);
        return ResponseEntity.noContent().build();
    }
}
