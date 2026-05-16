package com.example.ai_service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Publisher for AI quota reset events to RabbitMQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaResetPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbit.quota-events.exchange:quota.events.exchange}")
    private String exchangeName;

    @Value("${app.rabbit.quota-reset.routing-key:quota.reset}")
    private String quotaResetRoutingKey;

    /**
     * Publish a quota reset event
     *
     * @param userId         The user ID
     * @param quotaType      The quota type (GENERATION or ATS)
     * @param previousQuota  The previous quota value
     * @param newQuota       The new quota value after reset
     * @param isPremium      Whether the user is premium
     */
    public void publishQuotaReset(Long userId, String quotaType, Integer previousQuota, 
                                 Integer newQuota, Boolean isPremium) {
        QuotaResetEvent event = QuotaResetEvent.builder()
                .userId(userId)
                .quotaType(quotaType)
                .previousQuota(previousQuota)
                .newQuota(newQuota)
                .isPremium(isPremium)
                .resetDate(YearMonth.now().toString())
                .timestamp(Instant.now())
                .eventId(UUID.randomUUID().toString())
                .build();

        try {
            rabbitTemplate.convertAndSend(exchangeName, quotaResetRoutingKey, event);
            log.info("Published quota reset event - userId: {}, quotaType: {}, newQuota: {}", 
                     userId, quotaType, newQuota);
        } catch (AmqpException e) {
            log.error("Failed to publish quota reset event - userId: {}, error: {}", 
                      userId, e.getMessage());
            throw new IllegalStateException("Failed to publish quota reset event", e);
        }
    }

    /**
     * Publish batch quota reset events for multiple users
     *
     * @param userIds       List of user IDs to reset quotas for
     * @param quotaType     The quota type to reset
     * @param newQuota      The new quota value
     * @param isPremiumMap  Map of user IDs to premium status
     */
    public void publishBatchQuotaReset(List<Long> userIds, String quotaType,
                                      Integer newQuota, Map<Long, Boolean> isPremiumMap) {
        if (userIds == null || userIds.isEmpty()) {
            log.info("No quota reset events to publish for quotaType={}", quotaType);
            return;
        }

        int safeNewQuota = Objects.requireNonNullElse(newQuota, 0);
        int successfulPublishes = 0;
        for (Long userId : userIds) {
            Boolean isPremium = isPremiumMap == null ? Boolean.FALSE : isPremiumMap.getOrDefault(userId, false);
            Integer previousQuota = safeNewQuota * 2;
            try {
                publishQuotaReset(userId, quotaType, previousQuota, safeNewQuota, isPremium);
                successfulPublishes++;
            } catch (IllegalStateException ex) {
                log.error("Skipping failed quota reset publish for userId={} quotaType={}", userId, quotaType, ex);
            }
        }
        log.info("Published batch quota reset for {} of {} users", successfulPublishes, userIds.size());
    }
}
