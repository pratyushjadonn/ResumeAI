package com.example.template_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 30) String category,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 500) String previewImageUrl,
        String htmlLayout,
        String cssStyles,
        @Size(max = 20) String accentColor,
        @Size(max = 50) String layoutStyle,
        @NotNull Boolean premium,
        @NotNull Boolean active,
        @NotNull Boolean featured,
        List<@Size(max = 40) String> tags
) {
}
