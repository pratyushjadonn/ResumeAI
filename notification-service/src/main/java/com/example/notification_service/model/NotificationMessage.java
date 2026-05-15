package com.example.notification_service.model;

import java.time.Instant;

public record NotificationMessage(
        Long id,
        Long recipientId,
        NotificationType type,
        Long relatedId,
        String message,
        String actionUrl,
        boolean read,
        Instant createdAt
) {
}
