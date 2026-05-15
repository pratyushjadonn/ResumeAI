package com.example.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotNull Long recipientId,
        @NotBlank String type,
        Long relatedId,
        @NotBlank @Size(max = 500) String message,
        @Size(max = 300) String actionUrl
) {
}
