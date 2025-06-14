package com.team11.backend.domain.alert.dto.request;

import com.team11.backend.domain.alert.entity.Alert;
import com.team11.backend.domain.alert.entity.type.AlertType;
import com.team11.backend.domain.alert.entity.type.ChannelType;

import java.time.LocalDateTime;

public record CreateAlertRequest(
        String userUid,
        AlertType alertType,
        String message,
        ChannelType channel
) {
    public Alert toEntity() {
        return Alert.builder()
                .userUid(userUid)
                .alertType(alertType)
                .sentAt(LocalDateTime.now())
                .message(message)
                .channel(channel)
                .build();
    }
}