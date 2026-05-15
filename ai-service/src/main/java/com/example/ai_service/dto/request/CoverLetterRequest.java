package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoverLetterRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 120) String applicantName,
        @NotBlank @Size(max = 120) String targetRole,
        @NotBlank @Size(max = 120) String companyName,
        @NotBlank @Size(max = 20000) String jobDescription,
        @Size(max = 2000) String highlights
) {
}
