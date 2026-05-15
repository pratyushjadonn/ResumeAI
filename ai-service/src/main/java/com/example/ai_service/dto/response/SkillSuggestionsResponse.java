package com.example.ai_service.dto.response;

import java.util.List;

public record SkillSuggestionsResponse(
        List<String> skills
) {
}
