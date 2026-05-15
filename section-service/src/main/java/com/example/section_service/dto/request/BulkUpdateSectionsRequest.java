package com.example.section_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkUpdateSectionsRequest(
        @NotEmpty List<@Valid UpdateSectionRequest> sections
) {
}
