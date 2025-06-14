package com.team11.backend.domain.resource.entity; // 기존 패키지 경로 유지

import com.team11.backend.domain.resource.entity.type.AwsServiceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "resources")
@Getter
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid", nullable = false)
    private String userUid; // 사용자 UID

    @Column(name = "aws_resource_id", length = 100)
    private String awsResourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private AwsServiceType serviceType;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "is_idle")
    private Boolean isIdle;

    @Column(name = "usage_rate")
    private Float usageRate;

    @Column(name = "cost_usd")
    private Float costUsd;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Resource(String userUid, String awsResourceId, AwsServiceType serviceType, String region, Boolean isIdle, Float usageRate, Float costUsd, LocalDateTime lastCheckedAt) {
        this.userUid = userUid;
        this.awsResourceId = awsResourceId;
        this.serviceType = serviceType;
        this.region = region;
        this.isIdle = isIdle;
        this.usageRate = usageRate;
        this.costUsd = costUsd;
        this.lastCheckedAt = lastCheckedAt;
    }

    // 엔티티 필드를 업데이트하는 메소드 추가
    public void update(AwsServiceType serviceType, String region, Boolean isIdle, Float usageRate, Float costUsd, LocalDateTime lastCheckedAt) {
        this.serviceType = serviceType;
        this.region = region;
        this.isIdle = isIdle;
        this.usageRate = usageRate;
        this.costUsd = costUsd;
        this.lastCheckedAt = lastCheckedAt;
    }
}
