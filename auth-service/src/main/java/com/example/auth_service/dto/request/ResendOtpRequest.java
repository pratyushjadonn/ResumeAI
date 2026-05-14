package com.example.auth_service.dto.request;

import com.example.auth_service.entity.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequest(
        @NotBlank @Email String email,
        @NotNull OtpType type
) {
}
