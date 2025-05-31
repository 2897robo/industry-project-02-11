package com.team11.backend.domain.alert;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 연관관계
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "alert_type", length = 50)
    private String alertType; // 예: 예산 초과, 유휴 지속 등

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(length = 50)
    private String channel; // Slack, Email 등
}
