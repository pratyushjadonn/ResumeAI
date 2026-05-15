package com.example.auth_service.dto.response;

import com.example.auth_service.entity.OtpType;

import java.time.Instant;

public record OtpDispatchResponse(
        String message,
        String email,
        OtpType type,
        Instant expiresAt,
        Instant resendAvailableAt
) {
}
