package com.example.ai_service.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QuotaResetPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private QuotaResetPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new QuotaResetPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchangeName", "quota.exchange");
        ReflectionTestUtils.setField(publisher, "quotaResetRoutingKey", "quota.reset");
    }

    @Test
    void publishesQuotaResetEvent() {
        ArgumentCaptor<QuotaResetEvent> captor = ArgumentCaptor.forClass(QuotaResetEvent.class);

        publisher.publishQuotaReset(7L, "GENERATION", 3, 5, true);

        verify(rabbitTemplate).convertAndSend(eq("quota.exchange"), eq("quota.reset"), captor.capture());
        QuotaResetEvent event = captor.getValue();
        assertEquals(7L, event.getUserId());
        assertEquals("GENERATION", event.getQuotaType());
        assertEquals(3, event.getPreviousQuota());
        assertEquals(5, event.getNewQuota());
        assertEquals(Boolean.TRUE, event.getIsPremium());
        assertNotNull(event.getTimestamp());
        assertFalse(event.getEventId().isBlank());
    }

    @Test
    void wrapsPublishFailures() {
        doThrow(new AmqpException("downstream unavailable") { })
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(QuotaResetEvent.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> publisher.publishQuotaReset(1L, "ATS", 1, 3, false));

        assertEquals("Failed to publish quota reset event", exception.getMessage());
    }

    @Test
    void skipsBatchPublishWhenInputIsEmpty() {
        publisher.publishBatchQuotaReset(List.of(), "ATS", 3, Map.of());
        publisher.publishBatchQuotaReset(null, "ATS", 3, Map.of());

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void continuesBatchPublishingAfterSingleFailure() {
        QuotaResetPublisher spyPublisher = spy(publisher);
        doNothing().when(spyPublisher).publishQuotaReset(1L, "GENERATION", 8, 4, true);
        doThrow(new IllegalStateException("boom")).when(spyPublisher).publishQuotaReset(2L, "GENERATION", 8, 4, false);
        doNothing().when(spyPublisher).publishQuotaReset(3L, "GENERATION", 8, 4, false);

        spyPublisher.publishBatchQuotaReset(List.of(1L, 2L, 3L), "GENERATION", 4, Map.of(1L, true));

        verify(spyPublisher).publishQuotaReset(1L, "GENERATION", 8, 4, true);
        verify(spyPublisher).publishQuotaReset(2L, "GENERATION", 8, 4, false);
        verify(spyPublisher).publishQuotaReset(3L, "GENERATION", 8, 4, false);
    }
}
