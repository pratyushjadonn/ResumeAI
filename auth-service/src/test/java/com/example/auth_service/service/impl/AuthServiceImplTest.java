package com.example.auth_service.service.impl;

import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.RegisterRequest;
import com.example.auth_service.dto.request.UpdateSubscriptionRequest;
import com.example.auth_service.dto.response.AuthResponse;
import com.example.auth_service.dto.response.OtpDispatchResponse;
import com.example.auth_service.dto.response.UserProfileResponse;
import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.repository.RefreshTokenRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.EmailService;
import com.example.auth_service.service.JwtService;
import com.example.auth_service.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 3600000L);
    }

    @Test
    void registerUser_success() {
        RegisterRequest request = new RegisterRequest("Alice Johnson", "alice@example.com", "Password1");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpDispatchResponse response = authService.register(request);

        assertEquals("Account created successfully. Please login.", response.message());
        assertEquals("alice@example.com", response.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Alice Johnson", savedUser.getFullName());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(SubscriptionPlan.FREE, savedUser.getSubscriptionPlan());
        assertEquals(AuthProvider.LOCAL, savedUser.getProvider());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertTrue(savedUser.isActive());
        assertTrue(savedUser.isVerified());
    }

    @Test
    void registerUser_emailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Alice Johnson", "alice@example.com", "Password1");
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success() {
        User user = buildLocalUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(List.of());
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.extractExpiration("refresh-token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "Password1"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("user@example.com", response.user().email());

        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_invalidPassword() {
        User user = buildLocalUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("user@example.com", "WrongPassword1")));

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void updateSubscription_success() {
        User user = buildLocalUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = authService.updateSubscription(
                "user@example.com",
                new UpdateSubscriptionRequest("PREMIUM")
        );

        assertEquals(SubscriptionPlan.PREMIUM, response.subscriptionPlan());
        verify(userRepository).save(user);
    }

    private User buildLocalUser() {
        return User.builder()
                .fullName("Test User")
                .email("user@example.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .active(true)
                .verified(true)
                .verifiedAt(Instant.now())
                .failedOtpAttempts(0)
                .build();
    }
}
