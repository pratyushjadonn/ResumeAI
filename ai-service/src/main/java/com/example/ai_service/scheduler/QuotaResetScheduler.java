package com.example.ai_service.scheduler;

import com.example.ai_service.messaging.QuotaResetPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled task for resetting AI quotas monthly
 * Publishes quota reset events to RabbitMQ for notification service
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class QuotaResetScheduler {

    private final QuotaResetPublisher quotaResetPublisher;

    @Value("${app.rabbit.quota-reset.enabled:true}")
    private boolean quotaResetEnabled;

    @Value("${app.ai.free-monthly-generation-limit:5}")
    private Integer freeGenerationQuota;

    @Value("${app.ai.free-monthly-ats-limit:3}")
    private Integer freeAtsQuota;

    @Value("${app.ai.premium-monthly-generation-limit:100}")
    private Integer premiumGenerationQuota;

    @Value("${app.ai.premium-monthly-ats-limit:50}")
    private Integer premiumAtsQuota;

    /**
     * Reset generation and ATS quotas at the start of each month
     * Scheduled to run at 00:00 UTC on the 1st of each month
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    public void resetMonthlyQuotas() {
        if (!quotaResetEnabled) {
            log.warn("Quota reset is disabled");
            return;
        }

        log.info("Starting monthly quota reset process...");

        boolean generationReset = runResetStep("generation", this::resetGenerationQuotas);
        boolean atsReset = runResetStep("ATS", this::resetAtsQuotas);
        if (generationReset && atsReset) {
            log.info("Monthly quota reset completed successfully");
        } else {
            log.warn("Monthly quota reset finished with partial failures");
        }
    }

    /**
     * Reset generation quotas for all users
     */
    private void resetGenerationQuotas() {
        log.info("Resetting generation quotas...");

        // Get all users (this is a mock implementation)
        List<Long> allUserIds = getAllUserIds();
        Map<Long, Boolean> userPremiumStatus = getUserPremiumStatus(allUserIds);

        for (Long userId : allUserIds) {
            boolean isPremium = Boolean.TRUE.equals(userPremiumStatus.get(userId));
            Integer newQuota = isPremium ? premiumGenerationQuota : freeGenerationQuota;

            quotaResetPublisher.publishQuotaReset(
                    userId,
                    "GENERATION",
                    0, // Previous quota (would be calculated from usage)
                    newQuota,
                    isPremium
            );
        }

        log.info("Generation quota reset published for {} users", allUserIds.size());
    }

    /**
     * Reset ATS quotas for all users
     */
    private void resetAtsQuotas() {
        log.info("Resetting ATS quotas...");

        // Get all users
        List<Long> allUserIds = getAllUserIds();
        Map<Long, Boolean> userPremiumStatus = getUserPremiumStatus(allUserIds);

        for (Long userId : allUserIds) {
            boolean isPremium = Boolean.TRUE.equals(userPremiumStatus.get(userId));
            Integer newQuota = isPremium ? premiumAtsQuota : freeAtsQuota;

            quotaResetPublisher.publishQuotaReset(
                    userId,
                    "ATS",
                    0, // Previous quota
                    newQuota,
                    isPremium
            );
        }

        log.info("ATS quota reset published for {} users", allUserIds.size());
    }

    /**
     * Get all active user IDs (mock implementation)
     * In production, this would query your user service or database
     */
    private List<Long> getAllUserIds() {
        // Mock implementation - replace with actual user retrieval
        return new ArrayList<>();
    }

    /**
     * Get premium status for users (mock implementation)
     */
    private Map<Long, Boolean> getUserPremiumStatus(List<Long> userIds) {
        Map<Long, Boolean> premiumStatus = new HashMap<>();
        // Mock implementation - replace with actual premium status retrieval
        for (Long userId : userIds) {
            premiumStatus.put(userId, false); // Default to free tier
        }
        return premiumStatus;
    }

    /**
     * Optional: Reset quotas at a specific time if needed (e.g., 00:30 UTC)
     * This can be used as a fallback or for manual triggers
     */
    @Scheduled(cron = "0 30 0 1 * *", zone = "UTC")
    public void verifyQuotaReset() {
        log.debug("Verifying quota reset completion...");
        // Add verification logic here if needed
    }

    private boolean runResetStep(String stepName, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException ex) {
            log.error("Error during {} quota reset", stepName, ex);
            return false;
        }
    }
}
