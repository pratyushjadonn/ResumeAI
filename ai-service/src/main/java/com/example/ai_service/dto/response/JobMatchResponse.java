package com.example.ai_service.dto.response;

import java.util.List;

public record JobMatchResponse(
        int matchScore,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> recommendations
) {
}
