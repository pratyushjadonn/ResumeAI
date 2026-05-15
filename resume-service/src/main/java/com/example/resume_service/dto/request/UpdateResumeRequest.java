package com.example.resume_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResumeRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 120) String targetRole,
        @Size(max = 60) String templateKey,
        Long templateId,
        @Size(max = 1000) String summary,
        @Size(max = 30) String language
) {
}
