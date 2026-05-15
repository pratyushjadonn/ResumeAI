package com.example.jobmatch_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FetchJobsRequest(
        @NotBlank @Size(max = 120) String jobTitle,
        @Size(max = 120) String location
) {
}
