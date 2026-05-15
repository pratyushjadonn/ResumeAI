package com.example.auth_service.dto.response;

import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.SubscriptionPlan;
import com.example.auth_service.entity.UserRole;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        AuthProvider provider,
        SubscriptionPlan subscriptionPlan,
        boolean active,
        boolean verified,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
