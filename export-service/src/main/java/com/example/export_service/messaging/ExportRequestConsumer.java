package com.example.export_service.messaging;

import com.example.common.events.ExportRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExportRequestConsumer {

    @RabbitListener(queues = "${app.rabbitmq.export-queue:export.queue}")
    public void consumeExportRequest(ExportRequestEvent event) {
        log.info("Generating {} export for resumeId {}", event.getExportType(), event.getResumeId());
        log.info("Export request received for {}", event.getUserEmail());
    }
}
