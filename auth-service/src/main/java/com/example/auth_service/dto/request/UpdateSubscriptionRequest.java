package com.example.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateSubscriptionRequest(
        @NotBlank String subscriptionPlan
) {
}
