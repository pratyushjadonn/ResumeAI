package com.example.resumeai_web.controller;

import com.example.resumeai_web.service.GatewayRoutingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiGatewayControllerTest {

    @Mock
    private GatewayRoutingService gatewayRoutingService;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<byte[]> httpResponse;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void proxyGetForwardsRequestAndResponse() throws Exception {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/templates");
        request.setQueryString("page=1");
        request.addHeader("Authorization", "Bearer token");

        when(gatewayRoutingService.resolveTargetBaseUrl("/api/v1/templates")).thenReturn("http://template-service/");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of(
                        "Content-Type", List.of("application/json"),
                        "Connection", List.of("keep-alive")
                ),
                (left, right) -> true
        ));

        var responseEntity = controller.proxyGet(request);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest forwardedRequest = requestCaptor.getValue();
        assertEquals("GET", forwardedRequest.method());
        assertEquals(URI.create("http://template-service/api/v1/templates?page=1"), forwardedRequest.uri());
        assertEquals("Bearer token", forwardedRequest.headers().firstValue("Authorization").orElseThrow());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(List.of("application/json"), responseEntity.getHeaders().get("Content-Type"));
        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), responseEntity.getBody());
    }

    @Test
    void proxyPostForwardsBodyBasedMethods() throws Exception {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ai/summary");
        request.setContent("{\"targetRole\":\"Java Developer\"}".getBytes(StandardCharsets.UTF_8));

        when(gatewayRoutingService.resolveTargetBaseUrl("/api/v1/ai/summary")).thenReturn("http://ai-service");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("created".getBytes(StandardCharsets.UTF_8));
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (left, right) -> true));

        var responseEntity = controller.proxyPost(request);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("POST", requestCaptor.getValue().method());
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertArrayEquals("created".getBytes(StandardCharsets.UTF_8), responseEntity.getBody());
    }

    @Test
    void proxyReturnsNotFoundWhenRouteIsUnknown() {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unknown");

        when(gatewayRoutingService.resolveTargetBaseUrl("/api/v1/unknown"))
                .thenThrow(new IllegalArgumentException("No route"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.proxyGet(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void proxyReturnsBadGatewayWhenInterrupted() throws Exception {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/exports/1");

        when(gatewayRoutingService.resolveTargetBaseUrl("/api/v1/exports/1")).thenReturn("http://export-service");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.proxyDelete(request));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void proxyReturnsBadGatewayWhenIoFails() throws Exception {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/resumes/9");

        when(gatewayRoutingService.resolveTargetBaseUrl("/api/v1/resumes/9")).thenReturn("http://resume-service");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("down"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.proxyPut(request));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void homeReturnsExpectedMessage() {
        ApiGatewayController controller = new ApiGatewayController(gatewayRoutingService, httpClient);

        var responseEntity = controller.home();

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("ResumeAI gateway is running.", responseEntity.getBody());
    }
}
