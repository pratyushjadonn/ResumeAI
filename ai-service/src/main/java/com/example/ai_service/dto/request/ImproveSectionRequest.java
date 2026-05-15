package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImproveSectionRequest(
        Long userId,
        Long resumeId,
        @NotBlank @Size(max = 40) String sectionType,
        @NotBlank @Size(max = 20000) String content,
        @Size(max = 500) String tone
) {
}
