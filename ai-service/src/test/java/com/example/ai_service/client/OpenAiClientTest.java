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

class OpenAiClientTest {

    @Test
    void generatesTrimmedTextFromResponse() {
        OpenAiClient client = new OpenAiClient(builderForJson("{\"choices\":[{\"message\":{\"content\":\"  Ready summary  \"}}]}"));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "gpt-4o-mini");

        assertEquals("Ready summary", client.generateText("Create summary"));
    }

    @Test
    void rejectsMissingApiKey() {
        OpenAiClient client = new OpenAiClient(WebClient.builder());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("OPENAI_API_KEY is not configured", exception.getMessage());
    }

    @Test
    void rejectsEmptyResponseBody() {
        OpenAiClient client = new OpenAiClient(WebClient.builder().exchangeFunction(request ->
                Mono.just(ClientResponse.create(HttpStatus.OK).build())));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "gpt-4o-mini");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("OpenAI response was empty", exception.getMessage());
    }

    @Test
    void rejectsBlankResponseContent() {
        OpenAiClient client = new OpenAiClient(builderForJson("{\"choices\":[{\"message\":{\"content\":\"   \"}}]}"));
        ReflectionTestUtils.setField(client, "apiKey", "secret-key");
        ReflectionTestUtils.setField(client, "model", "gpt-4o-mini");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.generateText("prompt"));

        assertEquals("OpenAI response content is empty", exception.getMessage());
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
