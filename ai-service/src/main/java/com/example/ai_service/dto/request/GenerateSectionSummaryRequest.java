package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateSectionSummaryRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 40) String sectionType,
        @Size(max = 120) String sectionTitle,
        @NotBlank @Size(max = 20000) String sectionContent,
        @Size(max = 120) String targetRole
) {
}
