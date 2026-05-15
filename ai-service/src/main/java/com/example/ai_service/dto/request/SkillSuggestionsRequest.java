package com.example.ai_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillSuggestionsRequest(
        Long userId,
        @NotBlank @Size(max = 120) String targetRole
) {
}
