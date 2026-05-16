package com.example.notification_service.controller;

import com.example.notification_service.dto.BroadcastNotificationRequest;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.model.NotificationMessage;
import com.example.notification_service.model.NotificationType;
import com.example.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void delegatesAllEndpoints() {
        NotificationController controller = new NotificationController(notificationService);
        CreateNotificationRequest createRequest = new CreateNotificationRequest(1L, "BROADCAST", null, "hello", "/next");
        BroadcastNotificationRequest broadcastRequest = new BroadcastNotificationRequest("broadcast", "/broadcast");
        NotificationMessage notification = new NotificationMessage(10L, 1L, NotificationType.BROADCAST, null, "hello", "/next", false, Instant.now());
        List<NotificationMessage> notifications = List.of(notification);

        when(notificationService.create(createRequest)).thenReturn(notification);
        when(notificationService.getByUser(1L)).thenReturn(notifications);
        when(notificationService.markRead(10L)).thenReturn(notification);
        when(notificationService.unreadCount(1L)).thenReturn(3);
        when(notificationService.broadcast(broadcastRequest)).thenReturn(notifications);

        assertEquals(notification, controller.create(createRequest));
        assertEquals(notifications, controller.getByUser(1L));
        assertEquals(notification, controller.markRead(10L));
        assertEquals(3, controller.unreadCount(1L));
        assertEquals(notifications, controller.broadcast(broadcastRequest));

        verify(notificationService).create(createRequest);
        verify(notificationService).getByUser(1L);
        verify(notificationService).markRead(10L);
        verify(notificationService).unreadCount(1L);
        verify(notificationService).broadcast(broadcastRequest);
    }
}
