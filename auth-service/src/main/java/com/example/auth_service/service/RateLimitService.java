package com.example.auth_service.service;

public interface RateLimitService {

    void checkRateLimit(String key, int maxRequests, long windowSeconds);
}
