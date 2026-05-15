package com.example.auth_service.dto.response;

import com.example.auth_service.entity.OtpType;

public record OtpVerificationResponse(
        String message,
        String email,
        OtpType type,
        boolean verified
) {
}
