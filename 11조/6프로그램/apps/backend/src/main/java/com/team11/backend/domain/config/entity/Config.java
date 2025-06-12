package com.team11.backend.domain.config.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "idle_threshold")
    private Float idleThreshold;

    @Column(name = "budget_limit")
    private Integer budgetLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Config(Long id, String userId, Float idleThreshold, Integer budgetLimit, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.idleThreshold = idleThreshold;
        this.budgetLimit = budgetLimit;
        this.createdAt = createdAt;
    }

    public void updateIdleThreshold(Float idleThreshold) {
        this.idleThreshold = idleThreshold;
    }

    public void updateBudgetLimit(Integer budgetLimit) {
        this.budgetLimit = budgetLimit;
    }
}
