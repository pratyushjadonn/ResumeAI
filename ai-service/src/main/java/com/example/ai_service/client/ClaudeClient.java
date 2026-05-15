package com.example.ai_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${claude.api-key:}")
    private String apiKey;

    @Value("${claude.model:claude-3-5-sonnet-20241022}")
    private String model;

    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("CLAUDE_API_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "max_tokens", 800,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        JsonNode response = webClientBuilder.baseUrl("https://api.anthropic.com")
                .build()
                .post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (response == null) {
            throw new IllegalStateException("Claude response was empty");
        }

        JsonNode contentNode = response.path("content").path(0).path("text");
        String content = contentNode.isMissingNode() ? "" : contentNode.asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("Claude response content is empty");
        }
        return content.trim();
    }
}
