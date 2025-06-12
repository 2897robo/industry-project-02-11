package com.team11.backend.domain.resource.dto;

import com.team11.backend.domain.resource.entity.Resource;
import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class ResourceDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long userId;
        private String awsResourceId;
        private AwsServiceType serviceType;
        private String region;
        private Boolean isIdle;
        private Float usageRate;
        private Float costUsd;
        private LocalDateTime lastCheckedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private AwsServiceType serviceType;
        private String region;
        private Boolean isIdle;
        private Float usageRate;
        private Float costUsd;
        private LocalDateTime lastCheckedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String awsResourceId;
        private AwsServiceType serviceType;
        private String region;
        private Boolean isIdle;
        private Float usageRate;
        private Float costUsd;
        private LocalDateTime lastCheckedAt;
        private LocalDateTime createdAt;

        public static Response from(Resource resource) {
            return Response.builder()
                    .id(resource.getId())
                    .userId(resource.getUserId())
                    .awsResourceId(resource.getAwsResourceId())
                    .serviceType(resource.getServiceType())
                    .region(resource.getRegion())
                    .isIdle(resource.getIsIdle())
                    .usageRate(resource.getUsageRate())
                    .costUsd(resource.getCostUsd())
                    .lastCheckedAt(resource.getLastCheckedAt())
                    .createdAt(resource.getCreatedAt())
                    .build();
        }
    }
}
