package com.team11.backend.domain.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 수행한 사용자
    @Column(name = "user_id")
    private Long userId;

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
}
