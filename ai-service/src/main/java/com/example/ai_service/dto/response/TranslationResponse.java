package com.example.ai_service.dto.response;

public record TranslationResponse(
        String translatedText,
        String targetLanguage
) {
}
