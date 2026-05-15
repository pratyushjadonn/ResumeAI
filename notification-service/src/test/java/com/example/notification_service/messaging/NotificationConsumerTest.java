package com.example.notification_service.messaging;

import com.example.common.events.NotificationEvent;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.model.NotificationMessage;
import com.example.notification_service.model.NotificationType;
import com.example.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void consumeNotification_savesNotification() {
        NotificationEvent event = NotificationEvent.builder()
                .recipientId(1L)
                .type("RESUME_CREATED")
                .email("user1@example.com")
                .subject("Resume created successfully")
                .message("Your resume was created.")
                .build();

        when(notificationService.create(org.mockito.ArgumentMatchers.any(CreateNotificationRequest.class)))
                .thenReturn(new NotificationMessage(10L, 1L, NotificationType.RESUME_CREATED, null, "Your resume was created.", null, false, Instant.now()));

        notificationConsumer.consumeNotification(event);

        ArgumentCaptor<CreateNotificationRequest> requestCaptor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).create(requestCaptor.capture());
        CreateNotificationRequest request = requestCaptor.getValue();
        assertEquals(1L, request.recipientId());
        assertEquals("RESUME_CREATED", request.type());
        assertEquals("Your resume was created.", request.message());
    }
}
