package com.team11.backend.domain.cost.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_histories", indexes = {
    @Index(name = "idx_cost_histories_user_uid", columnList = "user_uid"),
    @Index(name = "idx_cost_histories_usage_date", columnList = "usage_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid", nullable = false)
    private String userUid;

    @Column(name = "aws_account_id")
    private Long awsAccountId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "cost", precision = 15, scale = 4)
    private BigDecimal cost;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // JSON 형태의 상세 데이터

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
