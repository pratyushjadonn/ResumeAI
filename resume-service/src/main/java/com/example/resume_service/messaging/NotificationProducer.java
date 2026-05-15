package com.example.resume_service.messaging;

import com.example.common.events.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:resumeai.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification-routing-key:notification.routing}")
    private String routingKey;

    public void sendNotification(String email, String subject, String message) {
        sendNotification(null, null, email, subject, message);
    }

    public void sendNotification(Long recipientId, String type, String email, String subject, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .recipientId(recipientId)
                .type(type)
                .email(email)
                .subject(subject)
                .message(message)
                .build();

        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        log.info("Published notification event for recipientId={} to exchange={} with routingKey={}",
                recipientId, exchangeName, routingKey);
    }
}
