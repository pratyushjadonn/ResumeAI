package com.example.section_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 30) String type,
        @NotBlank @Size(max = 20000) String content,
        @NotNull Boolean visible,
        Boolean aiGenerated
) {
}
