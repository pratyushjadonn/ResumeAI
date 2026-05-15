package com.example.notification_service.service;

import com.example.notification_service.dto.BroadcastNotificationRequest;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.model.NotificationMessage;

import java.util.List;

public interface NotificationService {

    NotificationMessage create(CreateNotificationRequest request);

    List<NotificationMessage> getByUser(Long userId);

    NotificationMessage markRead(Long id);

    int unreadCount(Long userId);

    List<NotificationMessage> broadcast(BroadcastNotificationRequest request);
}
