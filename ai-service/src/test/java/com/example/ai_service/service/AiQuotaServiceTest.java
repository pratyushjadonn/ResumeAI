package com.example.ai_service.service;

import com.example.ai_service.exception.AiQuotaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AiQuotaService aiQuotaService;

    @Test
    void aiQuotaExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:1:ai_calls")).thenReturn("5");

        assertThrows(AiQuotaException.class,
                () -> aiQuotaService.checkAiGenerationQuota(1L, "FREE"));
    }

    @Test
    void premiumUser_unlimitedAccess() {
        aiQuotaService.checkAiGenerationQuota(1L, "PREMIUM");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void incrementAiUsage_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("user:1:ai_calls")).thenReturn(1L);

        aiQuotaService.incrementAiGeneration(1L);

        verify(valueOperations).increment("user:1:ai_calls");
        verify(redisTemplate).expire("user:1:ai_calls", Duration.ofDays(30));
    }

    @Test
    void getRemainingAtsChecks_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:2:ats_checks")).thenReturn("1");

        int remainingChecks = aiQuotaService.getRemainingAtsChecks(2L);

        assertEquals(2, remainingChecks);
    }
}
