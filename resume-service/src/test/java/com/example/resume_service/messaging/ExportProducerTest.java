package com.example.resume_service.messaging;

import com.example.common.events.ExportRequestEvent;
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
class ExportProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ExportProducer exportProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportProducer, "exchangeName", "resumeai.exchange");
        ReflectionTestUtils.setField(exportProducer, "routingKey", "export.routing");
    }

    @Test
    void sendExportRequest_publishesMessage() {
        exportProducer.sendExportRequest(12L, "user@example.com", "PDF");

        ArgumentCaptor<ExportRequestEvent> eventCaptor = ArgumentCaptor.forClass(ExportRequestEvent.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.Mockito.eq("resumeai.exchange"),
                org.mockito.Mockito.eq("export.routing"),
                eventCaptor.capture()
        );

        ExportRequestEvent event = eventCaptor.getValue();
        assertEquals(12L, event.getResumeId());
        assertEquals("user@example.com", event.getUserEmail());
        assertEquals("PDF", event.getExportType());
    }
}
