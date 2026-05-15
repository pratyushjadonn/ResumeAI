package com.example.resumeai_web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resumeai.gateway")
public class GatewayProperties {

    private String authServiceUrl;
    private String resumeServiceUrl;
    private String sectionServiceUrl;
    private String templateServiceUrl;
    private String aiServiceUrl;
    private String exportServiceUrl;
    private String notificationServiceUrl;
    private String jobmatchServiceUrl;

    public String getAuthServiceUrl() {
        return authServiceUrl;
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
    }

    public String getResumeServiceUrl() {
        return resumeServiceUrl;
    }

    public void setResumeServiceUrl(String resumeServiceUrl) {
        this.resumeServiceUrl = resumeServiceUrl;
    }

    public String getSectionServiceUrl() {
        return sectionServiceUrl;
    }

    public void setSectionServiceUrl(String sectionServiceUrl) {
        this.sectionServiceUrl = sectionServiceUrl;
    }

    public String getTemplateServiceUrl() {
        return templateServiceUrl;
    }

    public void setTemplateServiceUrl(String templateServiceUrl) {
        this.templateServiceUrl = templateServiceUrl;
    }

    public String getAiServiceUrl() {
        return aiServiceUrl;
    }

    public void setAiServiceUrl(String aiServiceUrl) {
        this.aiServiceUrl = aiServiceUrl;
    }

    public String getExportServiceUrl() {
        return exportServiceUrl;
    }

    public void setExportServiceUrl(String exportServiceUrl) {
        this.exportServiceUrl = exportServiceUrl;
    }

    public String getNotificationServiceUrl() {
        return notificationServiceUrl;
    }

    public void setNotificationServiceUrl(String notificationServiceUrl) {
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public String getJobmatchServiceUrl() {
        return jobmatchServiceUrl;
    }

    public void setJobmatchServiceUrl(String jobmatchServiceUrl) {
        this.jobmatchServiceUrl = jobmatchServiceUrl;
    }
}
