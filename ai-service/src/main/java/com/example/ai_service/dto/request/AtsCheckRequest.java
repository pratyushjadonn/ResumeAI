package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtsCheckRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 20000) String resumeText,
        @NotBlank @Size(max = 20000) String jobDescription
) {
}
