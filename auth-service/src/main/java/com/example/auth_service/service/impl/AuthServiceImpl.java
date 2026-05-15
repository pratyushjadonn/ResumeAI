package com.example.auth_service.service.impl;

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
import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.dto.response.OtpVerificationResponse;
import com.example.auth_service.dto.response.TokenValidationResponse;
import com.example.auth_service.dto.response.UserProfileResponse;
import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.OtpType;
import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.ForbiddenOperationException;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.AuthService;
import com.example.auth_service.service.EmailService;
import com.example.auth_service.service.JwtService;
import com.example.auth_service.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${security.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Override
    @Transactional
    public OtpDispatchResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Account already exists with this email");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .active(true)
                .verified(true)
                .verifiedAt(Instant.now())
                .failedOtpAttempts(0)
                .build();

        userRepository.save(user);
        log.info("User created without email verification requirement email={}", normalizedEmail);
        return new OtpDispatchResponse(
                "Account created successfully. Please login.",
                normalizedEmail,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional
    public OtpVerificationResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.type() == OtpType.VERIFY_EMAIL) {
            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new BadRequestException("Email verification OTP is only available for local accounts");
            }
            if (user.isVerified()) {
                return new OtpVerificationResponse("Email is already verified", normalizedEmail, request.type(), true);
            }
            try {
                otpService.consumeOtp(normalizedEmail, request.otp(), OtpType.VERIFY_EMAIL);
            } catch (RuntimeException ex) {
                user.setFailedOtpAttempts(user.getFailedOtpAttempts() + 1);
                userRepository.save(user);
                throw ex;
            }
            user.setVerified(true);
            user.setVerifiedAt(Instant.now());
            user.setFailedOtpAttempts(0);
            userRepository.save(user);
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
            log.info("Email verification completed for user={}", normalizedEmail);
            return new OtpVerificationResponse("Email verified successfully", normalizedEmail, request.type(), true);
        }

        validateResetEligibility(user);
        try {
            otpService.validateOtp(normalizedEmail, request.otp(), OtpType.RESET_PASSWORD);
        } catch (RuntimeException ex) {
            user.setFailedOtpAttempts(user.getFailedOtpAttempts() + 1);
            userRepository.save(user);
            throw ex;
        }
        user.setFailedOtpAttempts(0);
        userRepository.save(user);
        return new OtpVerificationResponse("OTP verified successfully", normalizedEmail, request.type(), true);
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendOtp(ResendOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.type() == OtpType.VERIFY_EMAIL) {
            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new BadRequestException("Email verification OTP is not required for Google accounts");
            }
            if (user.isVerified()) {
                throw new BadRequestException("Email is already verified");
            }
            return otpService.resendOtp(normalizedEmail, user.getFullName(), OtpType.VERIFY_EMAIL);
        }

        validateResetEligibility(user);
        return otpService.resendOtp(normalizedEmail, user.getFullName(), OtpType.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenOperationException("Your account is deactivated");
        }
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new ForbiddenOperationException("This account uses Google login. Please continue with Google.");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        revokeActiveRefreshTokens(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public OtpDispatchResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No account found with this email"));
        validateResetEligibility(user);
        return otpService.createAndSendOtp(normalizedEmail, user.getFullName(), OtpType.RESET_PASSWORD);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No account found with this email"));
        validateResetEligibility(user);
        try {
            otpService.consumeOtp(normalizedEmail, request.otp(), OtpType.RESET_PASSWORD);
        } catch (RuntimeException ex) {
            user.setFailedOtpAttempts(user.getFailedOtpAttempts() + 1);
            userRepository.save(user);
            throw ex;
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedOtpAttempts(0);
        userRepository.save(user);
        revokeActiveRefreshTokens(user);
        emailService.sendPasswordResetSuccessEmail(user.getEmail(), user.getFullName());
        log.info("Password reset completed for user={}", normalizedEmail);
    }

    @Override
    @Transactional
    public AuthResponse oauth2Login(String email, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedName = normalizeDisplayName(fullName, normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existingUser -> updateExistingOAuthUser(existingUser, normalizedName))
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .fullName(normalizedName)
                                .email(normalizedEmail)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .role(UserRole.USER)
                                .provider(AuthProvider.GOOGLE)
                                .subscriptionPlan(SubscriptionPlan.FREE)
                                .active(true)
                                .verified(true)
                                .verifiedAt(Instant.now())
                                .failedOtpAttempts(0)
                                .build()
                ));

        revokeActiveRefreshTokens(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));

        if (storedToken.isRevoked() || !storedToken.isActive()) {
            throw new BadRequestException("Refresh token is invalid or expired");
        }
        if (!jwtService.isRefreshToken(storedToken.getToken()) || jwtService.isTokenExpired(storedToken.getToken())) {
            storedToken.setRevoked(true);
            throw new BadRequestException("Refresh token is invalid or expired");
        }

        User user = storedToken.getUser();
        if (!user.isActive()) {
            throw new ForbiddenOperationException("Your account is deactivated");
        }
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        return issueTokens(user);
    }

    @Override
    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        String subject = jwtService.extractUsername(request.token());
        String role = jwtService.extractRole(request.token());
        return new TokenValidationResponse(true, subject, role);
    }

    @Override
    public UserProfileResponse getCurrentUser(String email) {
        return mapToProfile(findUserByEmail(email));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);
        String normalizedEmail = normalizeEmail(request.email());
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalize(request.phone()));
        return mapToProfile(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        revokeActiveRefreshTokens(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateSubscription(String email, UpdateSubscriptionRequest request) {
        User user = findUserByEmail(email);
        user.setSubscriptionPlan(parseSubscriptionPlan(request.subscriptionPlan()));
        return mapToProfile(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deactivateAccount(String email) {
        User user = findUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);
        revokeActiveRefreshTokens(user);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = findUserByEmail(email);
        revokeActiveRefreshTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .expiresAt(jwtService.extractExpiration(refreshTokenValue))
                .revoked(false)
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                accessTokenExpirationMs / 1000,
                mapToProfile(user)
        );
    }

    private UserProfileResponse mapToProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getProvider(),
                user.getSubscriptionPlan(),
                user.isActive(),
                user.isVerified(),
                user.getVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private SubscriptionPlan parseSubscriptionPlan(String subscriptionPlan) {
        try {
            return SubscriptionPlan.valueOf(subscriptionPlan.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid subscription plan. Allowed values: FREE, PREMIUM");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String fullName, String normalizedEmail) {
        String normalizedFullName = normalize(fullName);
        if (normalizedFullName != null) {
            return normalizedFullName;
        }
        return normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
    }

    private User updateExistingOAuthUser(User user, String normalizedName) {
        boolean changed = false;
        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }
        if (!user.isVerified()) {
            user.setVerified(true);
            user.setVerifiedAt(Instant.now());
            changed = true;
        }
        if (normalize(user.getFullName()) == null || user.getProvider() == AuthProvider.GOOGLE) {
            user.setFullName(normalizedName);
            changed = true;
        }
        if (user.getProvider() == AuthProvider.GOOGLE || normalize(user.getPassword()) == null) {
            user.setProvider(AuthProvider.GOOGLE);
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
    }

    private void revokeActiveRefreshTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        if (activeTokens.isEmpty()) {
            return;
        }
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void validateResetEligibility(User user) {
        if (!user.isActive()) {
            throw new ForbiddenOperationException("Your account is deactivated");
        }
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new ForbiddenOperationException("Password reset is not available for Google accounts.");
        }
    }
}
