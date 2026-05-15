package com.example.notification_service.controller;

import com.example.notification_service.dto.BroadcastNotificationRequest;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.model.NotificationMessage;
import com.example.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public NotificationMessage create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }

    @GetMapping
    public List<NotificationMessage> getByUser(@RequestParam Long userId) {
        List<NotificationMessage> notifications = notificationService.getByUser(userId);
        log.info("Returning {} notifications for userId={}", notifications.size(), userId);
        return notifications;
    }

    @PostMapping("/{id}/read")
    public NotificationMessage markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @GetMapping("/unread-count")
    public int unreadCount(@RequestParam Long userId) {
        return notificationService.unreadCount(userId);
    }

    @PostMapping("/broadcast")
    public List<NotificationMessage> broadcast(@Valid @RequestBody BroadcastNotificationRequest request) {
        return notificationService.broadcast(request);
    }
}
