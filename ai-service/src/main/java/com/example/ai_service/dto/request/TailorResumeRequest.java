package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TailorResumeRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 40000) String resumeJson,
        @NotBlank @Size(max = 20000) String jobDescription
) {
}
