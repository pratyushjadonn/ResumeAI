package com.example.auth_service.controller;

import com.example.auth_service.dto.request.ChangePasswordRequest;
import com.example.auth_service.dto.request.ForgotPasswordRequest;
import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.RefreshTokenRequest;
import com.example.auth_service.dto.request.RegisterRequest;
import com.example.auth_service.dto.request.ResendOtpRequest;
import com.example.auth_service.dto.request.ResetPasswordRequest;
import com.example.auth_service.dto.request.TokenValidationRequest;
import com.example.auth_service.dto.request.UpdateProfileRequest;
import com.example.auth_service.dto.request.UpdateSubscriptionRequest;
import com.example.auth_service.dto.request.VerifyOtpRequest;
import com.example.auth_service.dto.response.AuthResponse;
import com.example.auth_service.dto.response.MessageResponse;
import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.dto.response.OtpVerificationResponse;
import com.example.auth_service.dto.response.TokenValidationResponse;
import com.example.auth_service.dto.response.UserProfileResponse;
import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final UserProfileResponse USER_PROFILE = userProfile();

    @Mock
    private AuthService authService;

    @Test
    void delegatesPublicAuthEndpoints() {
        AuthController controller = new AuthController(authService);
        Instant now = Instant.now();
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "Password1");
        VerifyOtpRequest verifyOtpRequest = new VerifyOtpRequest("test@example.com", "123456", OtpType.VERIFY_EMAIL);
        ResendOtpRequest resendOtpRequest = new ResendOtpRequest("test@example.com", OtpType.VERIFY_EMAIL);
        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password1");
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest("test@example.com");
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest("test@example.com", "123456", "Password2");
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("refresh-token");
        TokenValidationRequest tokenValidationRequest = new TokenValidationRequest("access-token");

        OtpDispatchResponse dispatchResponse = new OtpDispatchResponse("sent", "test@example.com", OtpType.VERIFY_EMAIL, now, now.plusSeconds(60));
        OtpVerificationResponse verificationResponse = new OtpVerificationResponse("verified", "test@example.com", OtpType.VERIFY_EMAIL, true);
        AuthResponse authResponse = new AuthResponse("access", "refresh", "Bearer", 3600L, USER_PROFILE);
        TokenValidationResponse validationResponse = new TokenValidationResponse(true, "test@example.com", "USER");

        when(authService.register(registerRequest)).thenReturn(dispatchResponse);
        when(authService.verifyOtp(verifyOtpRequest)).thenReturn(verificationResponse);
        when(authService.resendOtp(resendOtpRequest)).thenReturn(dispatchResponse);
        when(authService.login(loginRequest)).thenReturn(authResponse);
        when(authService.forgotPassword(forgotPasswordRequest)).thenReturn(dispatchResponse);
        when(authService.refreshToken(refreshTokenRequest)).thenReturn(authResponse);
        when(authService.validateToken(tokenValidationRequest)).thenReturn(validationResponse);

        assertEquals(HttpStatus.CREATED, controller.register(registerRequest).getStatusCode());
        assertEquals(dispatchResponse, controller.register(registerRequest).getBody());
        assertEquals(verificationResponse, controller.verifyOtp(verifyOtpRequest).getBody());
        assertEquals(dispatchResponse, controller.resendOtp(resendOtpRequest).getBody());
        assertEquals(authResponse, controller.login(loginRequest).getBody());
        assertEquals(dispatchResponse, controller.forgotPassword(forgotPasswordRequest).getBody());
        assertEquals("Password reset successfully", controller.resetPassword(resetPasswordRequest).getBody().message());
        assertEquals(authResponse, controller.refresh(refreshTokenRequest).getBody());
        assertEquals(validationResponse, controller.validate(tokenValidationRequest).getBody());

        verify(authService).resetPassword(resetPasswordRequest);
    }

    @Test
    void delegatesAuthenticatedUserEndpoints() {
        AuthController controller = new AuthController(authService);
        UserDetails userDetails = User.withUsername("test@example.com").password("secret").roles("USER").build();
        UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest("Updated User", "9999999999", "Portfolio");
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("Password1", "Password2");
        UpdateSubscriptionRequest updateSubscriptionRequest = new UpdateSubscriptionRequest("PREMIUM");

        when(authService.getCurrentUser("test@example.com")).thenReturn(USER_PROFILE);
        when(authService.updateProfile("test@example.com", updateProfileRequest)).thenReturn(USER_PROFILE);
        when(authService.updateSubscription("test@example.com", updateSubscriptionRequest)).thenReturn(USER_PROFILE);

        assertEquals(USER_PROFILE, controller.me(userDetails).getBody());
        assertEquals(USER_PROFILE, controller.updateProfile(userDetails, updateProfileRequest).getBody());
        assertEquals("Password updated successfully", controller.changePassword(userDetails, changePasswordRequest).getBody().message());
        assertEquals(USER_PROFILE, controller.updateSubscription(userDetails, updateSubscriptionRequest).getBody());
        assertEquals("Account deactivated successfully", controller.deactivate(userDetails).getBody().message());
        assertEquals("Logged out successfully", controller.logout(userDetails).getBody().message());

        verify(authService).changePassword("test@example.com", changePasswordRequest);
        verify(authService).deactivateAccount("test@example.com");
        verify(authService).logout("test@example.com");
    }

    private static UserProfileResponse userProfile() {
        Instant now = Instant.now();
        return new UserProfileResponse(
                1L,
                "Test User",
                "test@example.com",
                "9999999999",
                UserRole.USER,
                AuthProvider.LOCAL,
                SubscriptionPlan.FREE,
                true,
                true,
                now,
                now,
                now
        );
    }
}
