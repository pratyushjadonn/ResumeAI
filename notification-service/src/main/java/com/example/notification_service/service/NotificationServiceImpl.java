package com.example.notification_service.service;

import com.example.notification_service.dto.BroadcastNotificationRequest;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.entity.Notification;
import com.example.notification_service.model.NotificationMessage;
import com.example.notification_service.model.NotificationType;
import com.example.notification_service.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationMessage create(CreateNotificationRequest request) {
        return createNotificationMessage(request);
    }

    private NotificationMessage createNotificationMessage(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.recipientId())
                .type(NotificationType.valueOf(request.type().trim().toUpperCase()))
                .relatedId(request.relatedId())
                .message(request.message())
                .actionUrl(request.actionUrl())
                .read(false)
                .build();
        return toMessage(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationMessage> getByUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toMessage)
                .toList();
    }

    @Override
    @Transactional
    public NotificationMessage markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setRead(true);
        return toMessage(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public int unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public List<NotificationMessage> broadcast(BroadcastNotificationRequest request) {
        return List.of(
                createNotificationMessage(new CreateNotificationRequest(1L, "BROADCAST", null, request.message(), request.actionUrl())),
                createNotificationMessage(new CreateNotificationRequest(2L, "BROADCAST", null, request.message(), request.actionUrl()))
        );
    }

    private NotificationMessage toMessage(Notification notification) {
        return new NotificationMessage(
                notification.getId(),
                notification.getRecipientId(),
                notification.getType(),
                notification.getRelatedId(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
