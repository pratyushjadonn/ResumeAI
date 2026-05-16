package com.example.resumeai_web.service;

import com.example.resumeai_web.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayRoutingServiceTest {

    private GatewayRoutingService gatewayRoutingService;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.setAuthServiceUrl("http://auth");
        properties.setResumeServiceUrl("http://resume");
        properties.setSectionServiceUrl("http://section");
        properties.setTemplateServiceUrl("http://template");
        properties.setAiServiceUrl("http://ai");
        properties.setExportServiceUrl("http://export");
        properties.setNotificationServiceUrl("http://notification");
        properties.setJobmatchServiceUrl("http://jobmatch");
        gatewayRoutingService = new GatewayRoutingService(properties);
    }

    @Test
    void resolvesConfiguredRoutes() {
        assertEquals("http://auth", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/auth/login"));
        assertEquals("http://section", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/resumes/10/sections"));
        assertEquals("http://section", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/resumes/10/sections/25"));
        assertEquals("http://resume", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/resumes"));
        assertEquals("http://template", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/templates"));
        assertEquals("http://ai", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/ai/summary"));
        assertEquals("http://export", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/exports/download/1"));
        assertEquals("http://notification", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/notifications"));
        assertEquals("http://jobmatch", gatewayRoutingService.resolveTargetBaseUrl("/api/v1/job-matches"));
    }

    @Test
    void throwsWhenNoRouteMatches() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> gatewayRoutingService.resolveTargetBaseUrl("/api/v1/unknown"));

        assertEquals("No downstream route configured for path: /api/v1/unknown", exception.getMessage());
    }
}
