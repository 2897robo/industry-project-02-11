package com.team11.backend.domain.recommendation.dto;

import com.team11.backend.domain.recommendation.entity.RecommendationLog;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class RecommendationLogDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long recommendationId; // Recommendation의 ID를 받아서 연관관계 설정
        private Long userId;
        private String action;
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long recommendationId; // Recommendation의 ID 반환
        private Long userId;
        private String action;
        private String reason;
        private LocalDateTime createdAt;

        public static Response from(RecommendationLog log) {
            return Response.builder()
                    .id(log.getId())
                    .recommendationId(log.getRecommendation() != null ? log.getRecommendation().getId() : null) // 연관된 Recommendation의 ID 사용
                    .userId(log.getUserId())
                    .action(log.getAction())
                    .reason(log.getReason())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }
}
