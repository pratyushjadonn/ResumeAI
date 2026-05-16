package com.example.ai_service.scheduler;

import com.example.ai_service.messaging.QuotaResetPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QuotaResetSchedulerTest {

    @Mock
    private QuotaResetPublisher quotaResetPublisher;

    private QuotaResetScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new QuotaResetScheduler(quotaResetPublisher);
        ReflectionTestUtils.setField(scheduler, "quotaResetEnabled", true);
        ReflectionTestUtils.setField(scheduler, "freeGenerationQuota", 5);
        ReflectionTestUtils.setField(scheduler, "freeAtsQuota", 3);
        ReflectionTestUtils.setField(scheduler, "premiumGenerationQuota", 100);
        ReflectionTestUtils.setField(scheduler, "premiumAtsQuota", 50);
    }

    @Test
    void resetMonthlyQuotasReturnsEarlyWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "quotaResetEnabled", false);

        scheduler.resetMonthlyQuotas();

        verifyNoInteractions(quotaResetPublisher);
    }

    @Test
    void resetMonthlyQuotasHandlesEmptyUserList() {
        scheduler.resetMonthlyQuotas();

        verifyNoInteractions(quotaResetPublisher);
    }

    @Test
    void privateHelpersReturnExpectedDefaults() {
        @SuppressWarnings("unchecked")
        List<Long> allUserIds = (List<Long>) ReflectionTestUtils.invokeMethod(scheduler, "getAllUserIds");
        @SuppressWarnings("unchecked")
        Map<Long, Boolean> premiumStatus = (Map<Long, Boolean>) ReflectionTestUtils.invokeMethod(
                scheduler, "getUserPremiumStatus", List.of(1L, 2L)
        );

        assertTrue(allUserIds.isEmpty());
        assertEquals(2, premiumStatus.size());
        assertFalse(premiumStatus.get(1L));
        assertFalse(premiumStatus.get(2L));
    }

    @Test
    void resetStepsAndVerifierAreSafeToInvoke() {
        assertTrue((Boolean) ReflectionTestUtils.invokeMethod(scheduler, "runResetStep", "generation", (Runnable) () -> { }));
        assertFalse((Boolean) ReflectionTestUtils.invokeMethod(scheduler, "runResetStep", "generation", (Runnable) () -> {
            throw new IllegalStateException("boom");
        }));

        ReflectionTestUtils.invokeMethod(scheduler, "resetGenerationQuotas");
        ReflectionTestUtils.invokeMethod(scheduler, "resetAtsQuotas");
        scheduler.verifyQuotaReset();

        verifyNoInteractions(quotaResetPublisher);
    }
}
