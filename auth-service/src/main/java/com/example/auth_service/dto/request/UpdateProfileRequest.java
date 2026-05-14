package com.example.auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Email String email,
        @Size(max = 20)
        @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Phone can contain digits and + - ( ) only")
        String phone
) {
}
