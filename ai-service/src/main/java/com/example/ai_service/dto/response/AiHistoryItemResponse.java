package com.example.ai_service.dto.response;

import java.time.Instant;

public record AiHistoryItemResponse(
        String requestType,
        Long resumeId,
        String model,
        int tokensUsed,
        String status,
        Instant createdAt
) {
}
