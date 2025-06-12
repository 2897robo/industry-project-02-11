package com.team11.backend.domain.resource.controller;

import com.team11.backend.domain.resource.dto.ResourceDto;
import com.team11.backend.domain.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    // 리소스 생성 (Create)
    @PostMapping
    public ResponseEntity<ResourceDto.Response> createResource(@RequestBody ResourceDto.CreateRequest request) {
        ResourceDto.Response response = resourceService.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 리소스 조회 (Read all)
    @GetMapping
    public ResponseEntity<List<ResourceDto.Response>> getAllResources() {
        List<ResourceDto.Response> responses = resourceService.getAllResources();
        return ResponseEntity.ok(responses);
    }

    // ID로 리소스 조회 (Read by ID)
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDto.Response> getResourceById(@PathVariable Long id) {
        ResourceDto.Response response = resourceService.getResourceById(id);
        return ResponseEntity.ok(response);
    }

    // 사용자 ID로 리소스 목록 조회 (Read by User ID)
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<ResourceDto.Response>> getResourcesByUserId(@PathVariable Long userId) {
        List<ResourceDto.Response> responses = resourceService.getResourcesByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    // AWS 리소스 ID로 리소스 조회 (Read by AWS Resource ID)
    @GetMapping("/by-aws-resource-id/{awsResourceId}")
    public ResponseEntity<ResourceDto.Response> getResourceByAwsResourceId(@PathVariable String awsResourceId) {
        ResourceDto.Response response = resourceService.getResourceByAwsResourceId(awsResourceId);
        return ResponseEntity.ok(response);
    }

    // 리소스 업데이트 (Update)
    @PutMapping("/{id}")
    public ResponseEntity<ResourceDto.Response> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceDto.UpdateRequest request) {
        ResourceDto.Response response = resourceService.updateResource(id, request);
        return ResponseEntity.ok(response);
    }

    // 리소스 삭제 (Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
