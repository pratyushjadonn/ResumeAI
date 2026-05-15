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
public class OpenAiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    public String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a professional resume writing assistant."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        JsonNode response = webClientBuilder.baseUrl("https://api.openai.com")
                .build()
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(apiKey))
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));

        if (response == null) {
            throw new IllegalStateException("OpenAI response was empty");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        String content = contentNode.isMissingNode() ? "" : contentNode.asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("OpenAI response content is empty");
        }
        return content.trim();
    }
}
