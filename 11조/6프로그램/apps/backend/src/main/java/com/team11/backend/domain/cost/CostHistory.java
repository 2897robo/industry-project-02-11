package com.team11.backend.domain.cost;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_histories")
public class CostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "cost_usd")
    private Float costUsd;

    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // JSONB → String 처리 (나중에 Jackson 등으로 매핑 가능)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}