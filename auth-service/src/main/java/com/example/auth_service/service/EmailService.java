package com.example.auth_service.service;

import com.example.auth_service.entity.OtpType;

public interface EmailService {

    void sendOtpEmail(String email, String fullName, String otp, OtpType otpType, long expiryMinutes);

    void sendWelcomeEmail(String email, String fullName);

    void sendForgotPasswordOtpEmail(String email, String fullName, String otp, long expiryMinutes);

    void sendPasswordResetSuccessEmail(String email, String fullName);
}
