package com.example.export_service.model;

import java.time.Instant;

public record ExportJob(
        Long jobId,
        Long userId,
        Long resumeId,
        ExportFormat format,
        ExportJobStatus status,
        String downloadUrl,
        Instant createdAt,
        Instant expiresAt,
        String filePath,
        String fileName
) {
}
