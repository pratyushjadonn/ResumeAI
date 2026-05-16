package com.example.auth_service.service.impl;

import com.example.auth_service.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryRateLimitServiceTest {

    @Test
    void allowsRequestsWithinConfiguredWindow() {
        InMemoryRateLimitService service = new InMemoryRateLimitService();

        assertDoesNotThrow(() -> service.checkRateLimit("otp:test@example.com", 2, 60L));
        assertDoesNotThrow(() -> service.checkRateLimit("otp:test@example.com", 2, 60L));
    }

    @Test
    void rejectsRequestsAfterLimitIsReached() {
        InMemoryRateLimitService service = new InMemoryRateLimitService();
        service.checkRateLimit("otp:test@example.com", 1, 60L);

        assertThrows(TooManyRequestsException.class,
                () -> service.checkRateLimit("otp:test@example.com", 1, 60L));
    }
}
