package com.team11.backend.domain.resource;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "aws_resource_id", length = 100)
    private String awsResourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private AwsServiceType serviceType;

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
}