package com.example.section_service.dto.response;

import com.example.section_service.entity.SectionType;

import java.time.Instant;

public record SectionResponse(
        Long id,
        Long resumeId,
        Long userId,
        String title,
        SectionType type,
        String content,
        int displayOrder,
        boolean visible,
        boolean aiGenerated,
        Instant createdAt,
        Instant updatedAt
) {
}
