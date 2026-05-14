package com.example.auth_service.dto.response;

public record TokenValidationResponse(
        boolean valid,
        String subject,
        String role
) {
}
