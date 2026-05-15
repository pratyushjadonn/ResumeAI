package com.example.jobmatch_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalyzeJobRequest(
        @NotNull Long userId,
        @NotNull Long resumeId,
        @NotBlank @Size(max = 120) String jobTitle,
        @NotBlank @Size(max = 20000) String jobDescription,
        @NotBlank @Size(max = 20000) String resumeText
) {
}
