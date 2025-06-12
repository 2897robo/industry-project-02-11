package com.team11.backend.domain.recommendation.entity; // 기존 패키지 경로 유지

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recommendation_logs")
@Getter
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String action; // 예: accept, ignore 등

    @Column(columnDefinition = "TEXT")
    private String reason; // 선택 입력

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RecommendationLog(Recommendation recommendation, Long userId, String action, String reason) {
        this.recommendation = recommendation;
        this.userId = userId;
        this.action = action;
        this.reason = reason;
    }
}
