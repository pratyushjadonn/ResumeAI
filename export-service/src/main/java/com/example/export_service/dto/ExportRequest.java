package com.example.export_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportRequest(
        @NotNull Long userId,
        @NotNull Long resumeId,
        @Size(max = 80) String templateKey
) {
}
