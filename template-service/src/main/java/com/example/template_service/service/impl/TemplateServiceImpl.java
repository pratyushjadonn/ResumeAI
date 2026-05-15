package com.example.template_service.service.impl;

import com.example.template_service.dto.request.CreateTemplateRequest;
import com.example.template_service.dto.request.UpdateTemplateRequest;
import com.example.template_service.dto.response.TemplateResponse;
import com.example.template_service.entity.Template;
import com.example.template_service.entity.TemplateCategory;
import com.example.template_service.exception.BadRequestException;
import com.example.template_service.repository.TemplateRepository;
import com.example.template_service.service.TemplateService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        String templateKey = normalizeRequired(request.templateKey(), "Template key");
        if (templateRepository.existsByTemplateKeyIgnoreCase(templateKey)) {
            throw new BadRequestException("Template key already exists");
        }

        Template template = Template.builder()
                .templateKey(templateKey)
                .name(normalizeRequired(request.name(), "Template name"))
                .category(parseCategory(request.category()).name())
                .description(normalizeRequired(request.description(), "Description"))
                .previewImageUrl(normalizeOptional(request.previewImageUrl()))
                .htmlLayout(normalizeOptional(request.htmlLayout()))
                .cssStyles(normalizeOptional(request.cssStyles()))
                .accentColor(normalizeOptional(request.accentColor()))
                .layoutStyle(normalizeOptional(request.layoutStyle()))
                .premium(request.premium())
                .active(request.active())
                .featured(request.featured())
                .usageCount(0L)
                .tags(normalizeTags(request.tags()))
                .build();

        return toResponse(templateRepository.save(template));
    }

    @Override
    public List<TemplateResponse> getTemplates(String category) {
        List<Template> templates = category == null || category.isBlank()
                ? templateRepository.findByActiveTrueOrderByFeaturedDescNameAsc()
                : templateRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByFeaturedDescNameAsc(
                        parseCategory(category).name());
        return templates.stream().map(this::toResponse).toList();
    }

    @Override
    public List<TemplateResponse> getFeaturedTemplates() {
        return templateRepository.findByActiveTrueAndFeaturedTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TemplateResponse> getFreeTemplates() {
        return templateRepository.findByActiveTrueAndPremiumFalseOrderByFeaturedDescNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TemplateResponse> getPremiumTemplates() {
        return templateRepository.findByActiveTrueAndPremiumTrueOrderByFeaturedDescNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TemplateResponse> getPopularTemplates() {
        return templateRepository.findByActiveTrueOrderByUsageCountDescNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TemplateResponse getTemplate(Long templateId) {
        return toResponse(getExistingTemplate(templateId));
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(Long templateId, UpdateTemplateRequest request) {
        Template template = getExistingTemplate(templateId);
        template.setName(normalizeRequired(request.name(), "Template name"));
        template.setCategory(parseCategory(request.category()).name());
        template.setDescription(normalizeRequired(request.description(), "Description"));
        template.setPreviewImageUrl(normalizeOptional(request.previewImageUrl()));
        template.setHtmlLayout(normalizeOptional(request.htmlLayout()));
        template.setCssStyles(normalizeOptional(request.cssStyles()));
        template.setAccentColor(normalizeOptional(request.accentColor()));
        template.setLayoutStyle(normalizeOptional(request.layoutStyle()));
        template.setPremium(request.premium());
        template.setActive(request.active());
        template.setFeatured(request.featured());
        template.setTags(normalizeTags(request.tags()));
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse activateTemplate(Long templateId) {
        Template template = getExistingTemplate(templateId);
        template.setActive(true);
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse deactivateTemplate(Long templateId) {
        Template template = getExistingTemplate(templateId);
        template.setActive(false);
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse incrementUsage(Long templateId) {
        Template template = getExistingTemplate(templateId);
        template.setUsageCount(template.getUsageCount() + 1);
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        templateRepository.delete(getExistingTemplate(templateId));
    }

    private Template getExistingTemplate(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
    }

    private TemplateCategory parseCategory(String category) {
        try {
            return TemplateCategory.valueOf(normalizeRequired(category, "Category").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid category. Allowed values: PROFESSIONAL, ATS_OPTIMIZED, MODERN, MINIMAL, CREATIVE, EXECUTIVE");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .map(this::normalizeOptional)
                .filter(tag -> tag != null)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private TemplateResponse toResponse(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getTemplateKey(),
                template.getName(),
                template.getCategory(),
                template.getDescription(),
                template.getPreviewImageUrl(),
                template.getHtmlLayout(),
                template.getCssStyles(),
                template.getAccentColor(),
                template.getLayoutStyle(),
                template.isPremium(),
                template.isActive(),
                template.isFeatured(),
                template.getUsageCount(),
                template.getTags(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
