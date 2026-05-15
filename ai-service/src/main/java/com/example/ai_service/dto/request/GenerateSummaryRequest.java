package com.example.ai_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GenerateSummaryRequest(
        @NotBlank @Size(max = 120) String targetRole,
        @Min(0) @Max(40) int yearsOfExperience,
        @NotEmpty List<@NotBlank @Size(max = 50) String> skills,
        @Size(max = 500) String focusArea
) {
}
