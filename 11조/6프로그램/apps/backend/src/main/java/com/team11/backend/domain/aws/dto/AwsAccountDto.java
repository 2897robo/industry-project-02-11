package com.team11.backend.domain.aws.dto;

import com.team11.backend.domain.aws.entity.AwsAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AwsAccountDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private String accountAlias;
        private String awsAccountId;
        private String accessKeyId;
        private String secretAccessKey;
        private String region;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String accountAlias;
        private String accessKeyId;
        private String secretAccessKey;
        private String region;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String accountAlias;
        private String awsAccountId;
        private String region;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(AwsAccount entity) {
            return Response.builder()
                    .id(entity.getId())
                    .accountAlias(entity.getAccountAlias())
                    .awsAccountId(entity.getAwsAccountId())
                    .region(entity.getRegion())
                    .isActive(entity.getIsActive())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Credentials {
        private String accessKeyId;
        private String secretAccessKey;
        private String region;
    }
}
