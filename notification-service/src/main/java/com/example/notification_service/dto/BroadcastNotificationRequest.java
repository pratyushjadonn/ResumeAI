package com.example.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastNotificationRequest(
        @NotBlank @Size(max = 500) String message,
        @Size(max = 300) String actionUrl
) {
}
