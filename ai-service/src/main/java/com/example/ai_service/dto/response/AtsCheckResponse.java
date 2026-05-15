package com.example.ai_service.dto.response;

import java.util.List;

public record AtsCheckResponse(
        int score,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> recommendations,
        boolean analyzed
) {
}
