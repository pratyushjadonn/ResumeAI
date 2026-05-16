package com.example.resumeai_web.controller;

import com.example.resumeai_web.service.GatewayRoutingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Set;

@RestController
public class ApiGatewayController {

    private static final Set<String> REQUEST_HEADERS_TO_SKIP = Set.of(
            "host",
            "connection",
            "content-length"
    );
    private static final Set<String> RESPONSE_HEADERS_TO_SKIP = Set.of(
            "connection",
            "content-length",
            "transfer-encoding"
    );

    private final GatewayRoutingService gatewayRoutingService;
    private final HttpClient httpClient;

    public ApiGatewayController(GatewayRoutingService gatewayRoutingService) {
        this(gatewayRoutingService, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    ApiGatewayController(GatewayRoutingService gatewayRoutingService, HttpClient httpClient) {
        this.gatewayRoutingService = gatewayRoutingService;
        this.httpClient = httpClient;
    }

    @GetMapping({"/api/v1", "/api/v1/{*path}"})
    public ResponseEntity<byte[]> proxyGet(HttpServletRequest request) {
        return proxy(request);
    }

    @PostMapping({"/api/v1", "/api/v1/{*path}"})
    public ResponseEntity<byte[]> proxyPost(HttpServletRequest request) {
        return proxy(request);
    }

    @PutMapping({"/api/v1", "/api/v1/{*path}"})
    public ResponseEntity<byte[]> proxyPut(HttpServletRequest request) {
        return proxy(request);
    }

    @PatchMapping({"/api/v1", "/api/v1/{*path}"})
    public ResponseEntity<byte[]> proxyPatch(HttpServletRequest request) {
        return proxy(request);
    }

    @DeleteMapping({"/api/v1", "/api/v1/{*path}"})
    public ResponseEntity<byte[]> proxyDelete(HttpServletRequest request) {
        return proxy(request);
    }

    private ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        String path = request.getRequestURI();
        try {
            String targetBaseUrl = gatewayRoutingService.resolveTargetBaseUrl(path);
            String targetUrl = buildTargetUrl(targetBaseUrl, path, request.getQueryString());

            byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(60));

            copyRequestHeaders(request, builder);

            HttpRequest.BodyPublisher bodyPublisher = requiresRequestBody(request.getMethod())
                    ? HttpRequest.BodyPublishers.ofByteArray(requestBody)
                    : HttpRequest.BodyPublishers.noBody();

            HttpResponse<byte[]> response = httpClient.send(
                    builder.method(request.getMethod(), bodyPublisher).build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            HttpHeaders headers = new HttpHeaders();
            response.headers().map().forEach((name, values) -> {
                if (!RESPONSE_HEADERS_TO_SKIP.contains(name.toLowerCase())) {
                    headers.put(name, values);
                }
            });

            return new ResponseEntity<>(response.body(), headers, HttpStatus.valueOf(response.statusCode()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream service is unavailable", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream service is unavailable", ex);
        }
    }

    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("ResumeAI gateway is running.");
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (REQUEST_HEADERS_TO_SKIP.contains(headerName.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                builder.header(headerName, values.nextElement());
            }
        }
    }

    private boolean requiresRequestBody(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method);
    }

    private String buildTargetUrl(String baseUrl, String path, String queryString) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (queryString == null || queryString.isBlank()) {
            return normalizedBaseUrl + path;
        }
        return normalizedBaseUrl + path + "?" + queryString;
    }
}
