package com.example.resume_service.dto.response;

import com.example.resume_service.entity.ResumeStatus;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        Long userId,
        String title,
        String targetRole,
        String templateKey,
        Long templateId,
        String summary,
        Integer atsScore,
        String language,
        boolean isPublic,
        long viewCount,
        ResumeStatus status,
        int version,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
