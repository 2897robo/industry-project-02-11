package com.team11.backend.domain.audit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(length = 100)
    private String action; // 예: login, update_config

    @Column(name = "target_type", length = 100)
    private String targetType; // 예: resource, config

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "jsonb")
    private String meta; // 부가 정보 (IP, 변경 전/후 데이터 등)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AuditLog(Long id, String userId, String action, String targetType, Long targetId, String meta, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.meta = meta;
        this.createdAt = createdAt;
    }
}
