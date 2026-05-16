package com.example.auth_service.service.impl;

import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.OtpVerification;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.TooManyRequestsException;
import com.example.auth_service.repository.OtpVerificationRepository;
import com.example.auth_service.service.EmailService;
import com.example.auth_service.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5L);
        ReflectionTestUtils.setField(otpService, "maxOtpAttempts", 3);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(otpService, "otpMaxRequestsPerWindow", 10);
        ReflectionTestUtils.setField(otpService, "otpRateLimitWindowSeconds", 3600L);
    }

    @Test
    void createAndSendOtpPersistsAndEmailsResetPasswordCode() {
        when(otpVerificationRepository.findAllByEmailIgnoreCaseAndTypeAndUsedFalse("test@example.com", OtpType.RESET_PASSWORD))
                .thenReturn(List.of());
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponse response = otpService.createAndSendOtp(" Test@example.com ", "Test User", OtpType.RESET_PASSWORD);

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(rateLimitService).checkRateLimit("otp-create:RESET_PASSWORD:test@example.com", 10, 3600L);
        verify(otpVerificationRepository).save(captor.capture());
        verify(emailService).sendForgotPasswordOtpEmail(eq("test@example.com"), eq("Test User"), eq(captor.getValue().getOtp()), eq(5L));
        assertEquals("Password reset OTP sent successfully", response.message());
        assertEquals("test@example.com", response.email());
        assertNotNull(response.expiresAt());
        assertNotNull(response.resendAvailableAt());
    }

    @Test
    void validateOtpThrowsWhenOtpIsMissing() {
        when(otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL))
                .thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> otpService.validateOtp("test@example.com", "123456", OtpType.VERIFY_EMAIL));

        assertEquals("OTP not found. Please request a new OTP.", exception.getMessage());
    }

    @Test
    void validateOtpMarksExpiredOtpAsUsed() {
        OtpVerification verification = otp(Instant.now().minusSeconds(120), false, 0, Instant.now().minusSeconds(10));
        when(otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL))
                .thenReturn(Optional.of(verification));
        when(otpVerificationRepository.save(verification)).thenReturn(verification);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> otpService.validateOtp("test@example.com", "123456", OtpType.VERIFY_EMAIL));

        assertEquals("OTP has expired. Please request a new OTP.", exception.getMessage());
        assertTrue(verification.isUsed());
    }

    @Test
    void validateOtpRejectsInvalidCodeAndTracksAttempts() {
        OtpVerification verification = otp(Instant.now().plusSeconds(120), false, 1, Instant.now());
        when(otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL))
                .thenReturn(Optional.of(verification));
        when(otpVerificationRepository.save(verification)).thenReturn(verification);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> otpService.validateOtp("test@example.com", "999999", OtpType.VERIFY_EMAIL));

        assertEquals("Invalid OTP. Attempts left: 1", exception.getMessage());
        assertEquals(2, verification.getAttempts());
        assertFalse(verification.isUsed());
    }

    @Test
    void consumeOtpMarksVerificationAsUsed() {
        OtpVerification verification = otp(Instant.now().plusSeconds(120), false, 0, Instant.now());
        when(otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL))
                .thenReturn(Optional.of(verification));
        when(otpVerificationRepository.save(verification)).thenReturn(verification);

        OtpVerification saved = otpService.consumeOtp("test@example.com", "123456", OtpType.VERIFY_EMAIL);

        assertTrue(saved.isUsed());
        verify(otpVerificationRepository).save(verification);
    }

    @Test
    void resendOtpHonorsCooldownWindow() {
        OtpVerification verification = otp(Instant.now().plusSeconds(300), false, 0, Instant.now());
        when(otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc("test@example.com", OtpType.VERIFY_EMAIL))
                .thenReturn(Optional.of(verification));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> otpService.resendOtp("test@example.com", "Test User", OtpType.VERIFY_EMAIL));

        assertTrue(exception.getMessage().startsWith("Please wait "));
        verifyNoInteractions(emailService);
    }

    @Test
    void cleanupExpiredOtpsDeletesExpiredAndUsedRecords() {
        when(otpVerificationRepository.deleteByExpiryTimeBefore(any(Instant.class))).thenReturn(2L);
        when(otpVerificationRepository.deleteByUsedTrueAndCreatedAtBefore(any(Instant.class))).thenReturn(1L);

        otpService.cleanupExpiredOtps();

        verify(otpVerificationRepository).deleteByExpiryTimeBefore(any(Instant.class));
        verify(otpVerificationRepository).deleteByUsedTrueAndCreatedAtBefore(any(Instant.class));
    }

    private OtpVerification otp(Instant expiryTime, boolean used, int attempts, Instant createdAt) {
        return OtpVerification.builder()
                .email("test@example.com")
                .otp("123456")
                .type(OtpType.VERIFY_EMAIL)
                .expiryTime(expiryTime)
                .used(used)
                .attempts(attempts)
                .createdAt(createdAt)
                .build();
    }
}
