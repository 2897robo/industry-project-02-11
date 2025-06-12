package com.team11.backend.domain.recommendation.entity; // 기존 패키지 경로 유지

import com.team11.backend.domain.resource.entity.Resource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recommendations")
@Getter
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "recommendation_text", columnDefinition = "TEXT")
    private String recommendationText;

    @Column(name = "expected_saving")
    private Float expectedSaving;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Recommendation(Resource resource, String recommendationText, Float expectedSaving, String status) {
        this.resource = resource;
        this.recommendationText = recommendationText;
        this.expectedSaving = expectedSaving;
        this.status = status;
    }

    public void update(String recommendationText, Float expectedSaving, String status) {
        this.recommendationText = recommendationText;
        this.expectedSaving = expectedSaving;
        this.status = status;
    }
}
