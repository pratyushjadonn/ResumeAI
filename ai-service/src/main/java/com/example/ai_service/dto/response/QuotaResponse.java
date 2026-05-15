package com.example.ai_service.dto.response;

public record QuotaResponse(
        boolean premium,
        int remainingGenerationCalls,
        int remainingAtsChecks
) {
}
