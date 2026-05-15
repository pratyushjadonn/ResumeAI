package com.example.ai_service.dto.response;

import java.util.List;

public record SummaryResponse(
        String summary,
        List<String> suggestedKeywords
) {
}
