package com.example.template_service.service;

import com.example.template_service.dto.request.CreateTemplateRequest;
import com.example.template_service.dto.request.UpdateTemplateRequest;
import com.example.template_service.dto.response.TemplateResponse;

import java.util.List;

public interface TemplateService {

    TemplateResponse createTemplate(CreateTemplateRequest request);

    List<TemplateResponse> getTemplates(String category);

    List<TemplateResponse> getFreeTemplates();

    List<TemplateResponse> getPremiumTemplates();

    List<TemplateResponse> getFeaturedTemplates();

    List<TemplateResponse> getPopularTemplates();

    TemplateResponse getTemplate(Long templateId);

    TemplateResponse updateTemplate(Long templateId, UpdateTemplateRequest request);

    TemplateResponse activateTemplate(Long templateId);

    TemplateResponse deactivateTemplate(Long templateId);

    TemplateResponse incrementUsage(Long templateId);

    void deleteTemplate(Long templateId);
}
