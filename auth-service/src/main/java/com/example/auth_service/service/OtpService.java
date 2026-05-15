package com.example.auth_service.service;

import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.OtpVerification;

public interface OtpService {

    OtpDispatchResponse createAndSendOtp(String email, String fullName, OtpType type);

    OtpVerification validateOtp(String email, String otp, OtpType type);

    OtpVerification consumeOtp(String email, String otp, OtpType type);

    OtpDispatchResponse resendOtp(String email, String fullName, OtpType type);

    void markAsUsed(OtpVerification otpVerification);

    void cleanupExpiredOtps();
}
