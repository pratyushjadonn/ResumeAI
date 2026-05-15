package com.example.auth_service.service.impl;

import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.OtpVerification;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.TooManyRequestsException;
import com.example.auth_service.repository.OtpVerificationRepository;
import com.example.auth_service.service.EmailService;
import com.example.auth_service.service.OtpService;
import com.example.auth_service.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private long otpExpiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxOtpAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.otp.rate-limit.max-requests:10}")
    private int otpMaxRequestsPerWindow;

    @Value("${app.otp.rate-limit.window-seconds:3600}")
    private long otpRateLimitWindowSeconds;

    @Override
    @Transactional
    public OtpDispatchResponse createAndSendOtp(String email, String fullName, OtpType type) {
        String normalizedEmail = normalizeEmail(email);
        rateLimitService.checkRateLimit(
                "otp-create:" + type.name() + ":" + normalizedEmail,
                otpMaxRequestsPerWindow,
                otpRateLimitWindowSeconds
        );

        markAllUnusedAsUsed(normalizedEmail, type);
        String otpCode = generateOtpCode();
        Instant now = Instant.now();
        Instant expiryTime = now.plusSeconds(otpExpiryMinutes * 60);

        OtpVerification otpVerification = otpVerificationRepository.save(
                OtpVerification.builder()
                        .email(normalizedEmail)
                        .otp(otpCode)
                        .type(type)
                        .expiryTime(expiryTime)
                        .used(false)
                        .attempts(0)
                        .createdAt(now)
                        .build()
        );

        if (type == OtpType.RESET_PASSWORD) {
            emailService.sendForgotPasswordOtpEmail(normalizedEmail, fullName, otpCode, otpExpiryMinutes);
        } else {
            emailService.sendOtpEmail(normalizedEmail, fullName, otpCode, type, otpExpiryMinutes);
        }

        return new OtpDispatchResponse(
                type == OtpType.RESET_PASSWORD
                        ? "Password reset OTP sent successfully"
                        : "Verification OTP sent successfully",
                normalizedEmail,
                type,
                otpVerification.getExpiryTime(),
                otpVerification.getCreatedAt().plusSeconds(resendCooldownSeconds)
        );
    }

    @Override
    @Transactional
    public OtpVerification validateOtp(String email, String otp, OtpType type) {
        return verifyOtp(email, otp, type, false);
    }

    @Override
    @Transactional
    public OtpVerification consumeOtp(String email, String otp, OtpType type) {
        return verifyOtp(email, otp, type, true);
    }

    private OtpVerification verifyOtp(String email, String otp, OtpType type, boolean consume) {
        String normalizedEmail = normalizeEmail(email);
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, type)
                .orElseThrow(() -> new BadRequestException("OTP not found. Please request a new OTP."));

        if (otpVerification.isUsed()) {
            throw new BadRequestException("OTP has already been used");
        }
        if (Instant.now().isAfter(otpVerification.getExpiryTime())) {
            otpVerification.setUsed(true);
            otpVerificationRepository.save(otpVerification);
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }
        if (otpVerification.getAttempts() >= maxOtpAttempts) {
            otpVerification.setUsed(true);
            otpVerificationRepository.save(otpVerification);
            throw new TooManyRequestsException("Maximum OTP attempts reached. Please request a new OTP.");
        }

        if (!otpVerification.getOtp().equals(otp.trim())) {
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            if (otpVerification.getAttempts() >= maxOtpAttempts) {
                otpVerification.setUsed(true);
            }
            otpVerificationRepository.save(otpVerification);
            int attemptsLeft = Math.max(0, maxOtpAttempts - otpVerification.getAttempts());
            throw new BadRequestException("Invalid OTP. Attempts left: " + attemptsLeft);
        }

        if (consume) {
            otpVerification.setUsed(true);
        }
        return otpVerificationRepository.save(otpVerification);
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendOtp(String email, String fullName, OtpType type) {
        String normalizedEmail = normalizeEmail(email);
        otpVerificationRepository.findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, type)
                .ifPresent(existingOtp -> {
                    Instant resendAllowedAt = existingOtp.getCreatedAt().plusSeconds(resendCooldownSeconds);
                    if (Instant.now().isBefore(resendAllowedAt)) {
                        long secondsLeft = Math.max(1, resendAllowedAt.getEpochSecond() - Instant.now().getEpochSecond());
                        throw new TooManyRequestsException("Please wait " + secondsLeft + " seconds before requesting a new OTP.");
                    }
                });

        return createAndSendOtp(normalizedEmail, fullName, type);
    }

    @Override
    @Transactional
    public void markAsUsed(OtpVerification otpVerification) {
        otpVerification.setUsed(true);
        otpVerificationRepository.save(otpVerification);
    }

    @Override
    @Transactional
    @Scheduled(cron = "${app.otp.cleanup-cron:0 */10 * * * *}")
    public void cleanupExpiredOtps() {
        Instant now = Instant.now();
        long deletedExpired = otpVerificationRepository.deleteByExpiryTimeBefore(now);
        long deletedUsed = otpVerificationRepository.deleteByUsedTrueAndCreatedAtBefore(now.minusSeconds(86400));
        if (deletedExpired > 0 || deletedUsed > 0) {
            log.info("OTP cleanup completed: deletedExpired={}, deletedUsed={}", deletedExpired, deletedUsed);
        }
    }

    private void markAllUnusedAsUsed(String email, OtpType type) {
        List<OtpVerification> activeOtps = otpVerificationRepository.findAllByEmailIgnoreCaseAndTypeAndUsedFalse(email, type);
        if (activeOtps.isEmpty()) {
            return;
        }
        activeOtps.forEach(existingOtp -> existingOtp.setUsed(true));
        otpVerificationRepository.saveAll(activeOtps);
    }

    private String generateOtpCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
