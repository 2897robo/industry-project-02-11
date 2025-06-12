package com.team11.backend.domain.alert.dto.response;

import com.team11.backend.domain.alert.entity.Alert;
import com.team11.backend.domain.alert.entity.type.AlertType;
import com.team11.backend.domain.alert.entity.type.ChannelType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReadAlertResponse(
        Long id,
        AlertType alertType,
        String message,
        LocalDateTime sentAt,
        ChannelType channel
) {
    public static ReadAlertResponse fromEntity(Alert alert) {
        return ReadAlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .message(alert.getMessage())
                .sentAt(alert.getSentAt())
                .channel(alert.getChannel())
                .build();
    }
}
