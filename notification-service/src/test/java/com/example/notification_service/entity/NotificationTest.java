package com.example.notification_service.entity;

import com.example.notification_service.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationTest {

    @Test
    void onCreateSetsTimestampWhenMissing() {
        Notification notification = Notification.builder()
                .recipientId(1L)
                .type(NotificationType.BROADCAST)
                .message("hello")
                .read(false)
                .build();

        notification.onCreate();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void onCreatePreservesExistingTimestamp() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Notification notification = Notification.builder()
                .recipientId(1L)
                .type(NotificationType.BROADCAST)
                .message("hello")
                .read(false)
                .createdAt(createdAt)
                .build();

        notification.onCreate();

        assertEquals(createdAt, notification.getCreatedAt());
    }
}
