package com.team11.backend.domain.recommendation;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_logs")
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천과 연관관계
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
}
