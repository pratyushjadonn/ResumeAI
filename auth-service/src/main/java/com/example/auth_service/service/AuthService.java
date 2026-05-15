package com.example.auth_service.service;

import com.example.auth_service.dto.request.ChangePasswordRequest;
import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.ForgotPasswordRequest;
import com.example.auth_service.dto.request.RefreshTokenRequest;
import com.example.auth_service.dto.request.RegisterRequest;
import com.example.auth_service.dto.request.ResendOtpRequest;
import com.example.auth_service.dto.request.ResetPasswordRequest;
import com.example.auth_service.dto.request.TokenValidationRequest;
import com.example.auth_service.dto.request.UpdateProfileRequest;
import com.example.auth_service.dto.request.UpdateSubscriptionRequest;
import com.example.auth_service.dto.request.VerifyOtpRequest;
import com.example.auth_service.dto.response.AuthResponse;
import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.dto.response.OtpVerificationResponse;
import com.example.auth_service.dto.response.TokenValidationResponse;
import com.example.auth_service.dto.response.UserProfileResponse;

public interface AuthService {

    OtpDispatchResponse register(RegisterRequest request);

    OtpVerificationResponse verifyOtp(VerifyOtpRequest request);

    OtpDispatchResponse resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    OtpDispatchResponse forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    AuthResponse oauth2Login(String email, String fullName);

    AuthResponse refreshToken(RefreshTokenRequest request);

    TokenValidationResponse validateToken(TokenValidationRequest request);

    UserProfileResponse getCurrentUser(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    UserProfileResponse updateSubscription(String email, UpdateSubscriptionRequest request);

    void deactivateAccount(String email);

    void logout(String email);
}
