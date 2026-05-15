package com.example.resume_service.messaging;

import com.example.common.events.ExportRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExportProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:resumeai.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.export-routing-key:export.routing}")
    private String routingKey;

    public void sendExportRequest(Long resumeId, String userEmail, String exportType) {
        ExportRequestEvent event = ExportRequestEvent.builder()
                .resumeId(resumeId)
                .userEmail(userEmail)
                .exportType(exportType)
                .build();

        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
    }
}
