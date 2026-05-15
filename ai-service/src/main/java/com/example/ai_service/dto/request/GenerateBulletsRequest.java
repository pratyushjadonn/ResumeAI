package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GenerateBulletsRequest(
        @NotBlank @Size(max = 120) String role,
        @NotBlank @Size(max = 120) String projectName,
        @NotEmpty List<@NotBlank @Size(max = 120) String> responsibilities,
        List<@NotBlank @Size(max = 50) String> technologies,
        @Size(max = 200) String measurableImpact
) {
}
