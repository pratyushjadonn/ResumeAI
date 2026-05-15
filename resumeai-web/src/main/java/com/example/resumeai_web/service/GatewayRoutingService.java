package com.example.resumeai_web.service;

import com.example.resumeai_web.config.GatewayProperties;
import org.springframework.stereotype.Service;

@Service
public class GatewayRoutingService {

    private final GatewayProperties properties;

    public GatewayRoutingService(GatewayProperties properties) {
        this.properties = properties;
    }

    public String resolveTargetBaseUrl(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return properties.getAuthServiceUrl();
        }
        if (path.matches("^/api/v1/resumes/[^/]+/sections(?:/.*)?$")) {
            return properties.getSectionServiceUrl();
        }
        if (path.startsWith("/api/v1/resumes")) {
            return properties.getResumeServiceUrl();
        }
        if (path.startsWith("/api/v1/templates")) {
            return properties.getTemplateServiceUrl();
        }
        if (path.startsWith("/api/v1/ai")) {
            return properties.getAiServiceUrl();
        }
        if (path.startsWith("/api/v1/exports")) {
            return properties.getExportServiceUrl();
        }
        if (path.startsWith("/api/v1/notifications")) {
            return properties.getNotificationServiceUrl();
        }
        if (path.startsWith("/api/v1/job-matches")) {
            return properties.getJobmatchServiceUrl();
        }
        throw new IllegalArgumentException("No downstream route configured for path: " + path);
    }
}
