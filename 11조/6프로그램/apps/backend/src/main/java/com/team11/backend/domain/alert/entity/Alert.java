package com.team11.backend.domain.alert.entity;

import com.team11.backend.domain.alert.entity.type.AlertType;
import com.team11.backend.domain.alert.entity.type.ChannelType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "alerts")
@Getter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid")
    private String userUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 50)
    private AlertType alertType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column
    private ChannelType channel;

    @Builder
    public Alert(Long id, String userUid, AlertType alertType, String message, LocalDateTime sentAt, ChannelType channel) {
        this.id = id;
        this.userUid = userUid;
        this.alertType = alertType;
        this.message = message;
        this.sentAt = sentAt;
        this.channel = channel;
    }
}
