package com.example.auth_service.service.impl;

import com.example.auth_service.exception.TooManyRequestsException;
import com.example.auth_service.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
public class InMemoryRateLimitService implements RateLimitService {

    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    @Override
    public void checkRateLimit(String key, int maxRequests, long windowSeconds) {
        Instant now = Instant.now();
        Instant boundary = now.minusSeconds(windowSeconds);
        Deque<Instant> timestamps = requestLog.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(boundary)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxRequests) {
                log.warn("Rate limit exceeded for key={}", key);
                throw new TooManyRequestsException("Too many requests. Please wait and try again.");
            }
            timestamps.addLast(now);
        }
    }
}
