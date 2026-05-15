package com.example.resume_service.messaging;

import com.example.common.events.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationProducer, "exchangeName", "resumeai.exchange");
        ReflectionTestUtils.setField(notificationProducer, "routingKey", "notification.routing");
    }

    @Test
    void sendNotification_publishesMessage() {
        notificationProducer.sendNotification(1L, "RESUME_CREATED", "user@example.com", "Welcome", "Account created");

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.Mockito.eq("resumeai.exchange"),
                org.mockito.Mockito.eq("notification.routing"),
                eventCaptor.capture()
        );

        NotificationEvent event = eventCaptor.getValue();
        assertEquals(1L, event.getRecipientId());
        assertEquals("RESUME_CREATED", event.getType());
        assertEquals("user@example.com", event.getEmail());
        assertEquals("Welcome", event.getSubject());
        assertEquals("Account created", event.getMessage());
    }
}
