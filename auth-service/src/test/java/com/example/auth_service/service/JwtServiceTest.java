package com.example.auth_service.service;

import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 120_000L);
        jwtService.validateConfiguration();
    }

    @Test
    void generatesAndValidatesAccessToken() {
        com.example.auth_service.entity.User user = user();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("user@example.com")
                .password("secret")
                .roles("USER")
                .build();

        String token = jwtService.generateAccessToken(user);

        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertEquals("USER", jwtService.extractRole(token));
        assertTrue(jwtService.extractExpiration(token).isAfter(Instant.now()));
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void generatesRefreshToken() {
        String token = jwtService.generateRefreshToken(user());

        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void rejectsInvalidSecrets() {
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "");
        IllegalStateException missingSecret = assertThrows(IllegalStateException.class, jwtService::validateConfiguration);
        assertEquals("security.jwt.secret must be configured", missingSecret.getMessage());

        ReflectionTestUtils.setField(jwtService, "jwtSecret", Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8)));
        IllegalStateException shortSecret = assertThrows(IllegalStateException.class, jwtService::validateConfiguration);
        assertEquals("security.jwt.secret must decode to at least 32 bytes", shortSecret.getMessage());
    }

    @Test
    void invalidTokenDoesNotValidateAgainstDifferentUser() {
        String token = jwtService.generateAccessToken(user());
        UserDetails differentUser = org.springframework.security.core.userdetails.User.withUsername("other@example.com")
                .password("secret")
                .roles("USER")
                .build();

        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    private com.example.auth_service.entity.User user() {
        com.example.auth_service.entity.User user = com.example.auth_service.entity.User.builder()
                .fullName("User Example")
                .email("user@example.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .active(true)
                .verified(true)
                .failedOtpAttempts(0)
                .build();
        ReflectionTestUtils.setField(user, "id", 99L);
        return user;
    }
}
