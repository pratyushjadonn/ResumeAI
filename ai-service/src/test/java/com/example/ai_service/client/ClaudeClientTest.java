package com.example.ai_service.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClaudeClientTest {

    @Test
    void generatesTrimmedTextFromResponse() {
        ClaudeClient client = new ClaudeClient(builderForJson("{\"content\":[{\"text\":\"  Tailored resume  \"}]}"));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "claude-test");

        assertEquals("Tailored resume", client.generateText("Tailor this"));
    }

    @Test
    void rejectsMissingApiKey() {
        ClaudeClient client = new ClaudeClient(WebClient.builder());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("CLAUDE_API_KEY is not configured", exception.getMessage());
    }

    @Test
    void rejectsEmptyResponseBody() {
        ClaudeClient client = new ClaudeClient(WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK).build())));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "claude-test");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("Claude response was empty", exception.getMessage());
    }

    @Test
    void rejectsBlankResponseContent() {
        ClaudeClient client = new ClaudeClient(builderForJson("{\"content\":[{\"text\":\"   \"}]}"));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "claude-test");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("Claude response content is empty", exception.getMessage());
    }

    private WebClient.Builder builderForJson(String json) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build()
        );
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }
}
