package com.example.template_service.controller;

import com.example.template_service.dto.request.CreateTemplateRequest;
import com.example.template_service.dto.request.UpdateTemplateRequest;
import com.example.template_service.dto.response.TemplateResponse;
import com.example.template_service.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @GetMapping
    public List<TemplateResponse> getTemplates(@RequestParam(required = false) String category) {
        return templateService.getTemplates(category);
    }

    @GetMapping("/free")
    public List<TemplateResponse> getFreeTemplates() {
        return templateService.getFreeTemplates();
    }

    @GetMapping("/premium")
    public List<TemplateResponse> getPremiumTemplates() {
        return templateService.getPremiumTemplates();
    }

    @GetMapping("/featured")
    public List<TemplateResponse> getFeaturedTemplates() {
        return templateService.getFeaturedTemplates();
    }

    @GetMapping("/popular")
    public List<TemplateResponse> getPopularTemplates() {
        return templateService.getPopularTemplates();
    }

    @GetMapping("/{templateId}")
    public TemplateResponse getTemplate(@PathVariable Long templateId) {
        return templateService.getTemplate(templateId);
    }

    @PutMapping("/{templateId}")
    public TemplateResponse updateTemplate(@PathVariable Long templateId,
                                           @Valid @RequestBody UpdateTemplateRequest request) {
        return templateService.updateTemplate(templateId, request);
    }

    @PatchMapping("/{templateId}/activate")
    public TemplateResponse activateTemplate(@PathVariable Long templateId) {
        return templateService.activateTemplate(templateId);
    }

    @PatchMapping("/{templateId}/deactivate")
    public TemplateResponse deactivateTemplate(@PathVariable Long templateId) {
        return templateService.deactivateTemplate(templateId);
    }

    @PatchMapping("/{templateId}/usage")
    public TemplateResponse incrementUsage(@PathVariable Long templateId) {
        return templateService.incrementUsage(templateId);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
    }
}
