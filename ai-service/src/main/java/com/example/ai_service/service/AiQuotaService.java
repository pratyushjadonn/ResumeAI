package com.example.ai_service.service;

import com.example.ai_service.exception.AiQuotaException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final int FREE_AI_GENERATION_LIMIT = 5;
    private static final int FREE_ATS_LIMIT = 3;
    private static final Duration QUOTA_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public void checkAiGenerationQuota(Long userId, String subscriptionPlan) {
        if (isPremium(subscriptionPlan)) {
            return;
        }

        long currentUsage = getCurrentUsage(buildKey(userId, QuotaType.AI_GENERATION));
        if (currentUsage >= FREE_AI_GENERATION_LIMIT) {
            throw new AiQuotaException("Monthly AI generation quota exceeded for FREE plan");
        }
    }

    public void incrementAiGeneration(Long userId) {
        incrementCounter(buildKey(userId, QuotaType.AI_GENERATION));
    }

    public void checkAtsQuota(Long userId, String subscriptionPlan) {
        if (isPremium(subscriptionPlan)) {
            return;
        }

        long currentUsage = getCurrentUsage(buildKey(userId, QuotaType.ATS_CHECK));
        if (currentUsage >= FREE_ATS_LIMIT) {
            throw new AiQuotaException("Monthly ATS quota exceeded for FREE plan");
        }
    }

    public void incrementAtsUsage(Long userId) {
        incrementCounter(buildKey(userId, QuotaType.ATS_CHECK));
    }

    public int getRemainingAiCalls(Long userId) {
        long currentUsage = getCurrentUsage(buildKey(userId, QuotaType.AI_GENERATION));
        return Math.max(0, FREE_AI_GENERATION_LIMIT - (int) currentUsage);
    }

    public int getRemainingAtsChecks(Long userId) {
        long currentUsage = getCurrentUsage(buildKey(userId, QuotaType.ATS_CHECK));
        return Math.max(0, FREE_ATS_LIMIT - (int) currentUsage);
    }

    private void incrementCounter(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        Long updatedValue = valueOperations.increment(key);

        if (updatedValue != null && updatedValue == 1L) {
            redisTemplate.expire(key, QUOTA_TTL);
        }
    }

    private long getCurrentUsage(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }

    private String buildKey(Long userId, QuotaType quotaType) {
        Long safeUserId = userId == null ? 0L : userId;

        if (quotaType == QuotaType.AI_GENERATION) {
            return "user:" + safeUserId + ":ai_calls";
        }
        return "user:" + safeUserId + ":ats_checks";
    }

    private boolean isPremium(String subscriptionPlan) {
        return "PREMIUM".equalsIgnoreCase(subscriptionPlan);
    }
}
