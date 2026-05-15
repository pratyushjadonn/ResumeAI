package com.example.notification_service.messaging;

import com.example.common.events.NotificationEvent;
import com.example.notification_service.dto.CreateNotificationRequest;
import com.example.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final Pattern DEMO_USER_EMAIL_PATTERN = Pattern.compile("^user(\\d+)@example\\.com$");

    private final NotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.notification-queue:notification.queue}")
    public void consumeNotification(NotificationEvent event) {
        Long recipientId = resolveRecipientId(event);
        if (recipientId == null) {
            log.warn("Notification event skipped because recipientId could not be resolved for email={}", event.getEmail());
            return;
        }

        log.info("Consumed notification event for recipientId={} with subject={}", recipientId, event.getSubject());
        var savedNotification = notificationService.create(new CreateNotificationRequest(
                recipientId,
                defaultType(event.getType()),
                null,
                event.getMessage(),
                null
        ));
        log.info("Notification saved with id={} for recipientId={}", savedNotification.id(), recipientId);
    }

    private Long resolveRecipientId(NotificationEvent event) {
        if (event.getRecipientId() != null) {
            return event.getRecipientId();
        }
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            return null;
        }

        Matcher matcher = DEMO_USER_EMAIL_PATTERN.matcher(event.getEmail().trim().toLowerCase());
        if (!matcher.matches()) {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    private String defaultType(String type) {
        return (type == null || type.isBlank()) ? "RESUME_CREATED" : type.trim();
    }
}
