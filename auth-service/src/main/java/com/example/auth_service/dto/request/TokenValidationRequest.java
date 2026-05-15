package com.example.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenValidationRequest(
        @NotBlank String token
) {
}
