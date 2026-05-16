package com.example.notification_service.service;

import com.example.notification_service.dto.BroadcastNotificationRequest;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.entity.Notification;
import com.example.notification_service.model.NotificationMessage;
import com.example.notification_service.model.NotificationType;
import com.example.notification_service.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createPersistsNotificationAndMapsToMessage() {
        CreateNotificationRequest request = new CreateNotificationRequest(7L, " export_ready ", 15L, "Ready", "/downloads/file");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> savedNotification(101L, invocation.getArgument(0)));

        NotificationMessage message = notificationService.create(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(NotificationType.EXPORT_READY, captor.getValue().getType());
        assertEquals(101L, message.id());
        assertEquals(7L, message.recipientId());
        assertEquals(NotificationType.EXPORT_READY, message.type());
        assertEquals("Ready", message.message());
    }

    @Test
    void getByUserMapsSavedNotifications() {
        Notification first = savedNotification(1L, Notification.builder()
                .recipientId(9L).type(NotificationType.BROADCAST).message("One").read(false).build());
        Notification second = savedNotification(2L, Notification.builder()
                .recipientId(9L).type(NotificationType.ATS_COMPLETE).message("Two").read(true).build());
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(9L)).thenReturn(List.of(first, second));

        List<NotificationMessage> messages = notificationService.getByUser(9L);

        assertEquals(2, messages.size());
        assertEquals(NotificationType.BROADCAST, messages.getFirst().type());
        assertTrue(messages.get(1).read());
    }

    @Test
    void markReadUpdatesNotification() {
        Notification notification = savedNotification(55L, Notification.builder()
                .recipientId(3L).type(NotificationType.JOB_MATCH_FOUND).message("match").read(false).build());
        when(notificationRepository.findById(55L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationMessage message = notificationService.markRead(55L);

        assertTrue(notification.isRead());
        assertTrue(message.read());
    }

    @Test
    void markReadThrowsWhenNotificationIsMissing() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> notificationService.markRead(404L));
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse(4L)).thenReturn(6);

        assertEquals(6, notificationService.unreadCount(4L));
    }

    @Test
    void broadcastCreatesNotificationsForTwoRecipients() {
        AtomicLong ids = new AtomicLong(1);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation ->
                savedNotification(ids.getAndIncrement(), invocation.getArgument(0)));

        List<NotificationMessage> messages = notificationService.broadcast(new BroadcastNotificationRequest("Scheduled update", "/updates"));

        assertEquals(2, messages.size());
        assertEquals(1L, messages.get(0).recipientId());
        assertEquals(2L, messages.get(1).recipientId());
        assertEquals(NotificationType.BROADCAST, messages.get(0).type());
    }

    private Notification savedNotification(Long id, Notification notification) {
        notification.setId(id);
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        return notification;
    }
}
