package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslateResumeRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 40000) String resumeText,
        @NotBlank @Size(max = 40) String targetLanguage
) {
}
