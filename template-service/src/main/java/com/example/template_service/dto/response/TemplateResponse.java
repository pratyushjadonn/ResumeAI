package com.example.template_service.dto.response;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(
        Long id,
        String templateKey,
        String name,
        String category,
        String description,
        String previewImageUrl,
        String htmlLayout,
        String cssStyles,
        String accentColor,
        String layoutStyle,
        boolean premium,
        boolean active,
        boolean featured,
        long usageCount,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
