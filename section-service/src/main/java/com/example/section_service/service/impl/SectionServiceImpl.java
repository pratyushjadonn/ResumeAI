package com.example.section_service.service.impl;

import com.example.section_service.dto.request.BulkUpdateSectionsRequest;
import com.example.section_service.dto.request.CreateSectionRequest;
import com.example.section_service.dto.request.UpdateSectionRequest;
import com.example.section_service.dto.response.SectionResponse;
import com.example.section_service.entity.Section;
import com.example.section_service.entity.SectionType;
import com.example.section_service.exception.BadRequestException;
import com.example.section_service.repository.SectionRepository;
import com.example.section_service.service.SectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;

    @Override
    @Transactional
    public SectionResponse createSection(Long userId, Long resumeId, CreateSectionRequest request) {
        SectionType parsedType = parseType(request.type());
        validateUniqueTitle(userId, resumeId, request.title(), null);
        validateUniqueType(userId, resumeId, parsedType, null);

        Section section = Section.builder()
                .resumeId(resumeId)
                .userId(userId)
                .title(request.title().trim())
                .type(parsedType)
                .content(request.content().trim())
                .displayOrder((int) sectionRepository.countByResumeIdAndUserId(resumeId, userId) + 1)
                .visible(request.visible())
                .aiGenerated(Boolean.TRUE.equals(request.aiGenerated()))
                .build();

        return toResponse(sectionRepository.save(section));
    }

    @Override
    public List<SectionResponse> getSections(Long userId, Long resumeId) {
        return sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SectionResponse> getSectionsByType(Long userId, Long resumeId, String type) {
        SectionType sectionType = parseType(type);
        return sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId).stream()
                .filter(section -> section.getType() == sectionType)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SectionResponse getSection(Long userId, Long resumeId, Long sectionId) {
        return toResponse(getOwnedSection(userId, resumeId, sectionId));
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long userId, Long resumeId, Long sectionId, UpdateSectionRequest request) {
        Section section = getOwnedSection(userId, resumeId, sectionId);
        SectionType parsedType = parseType(request.type());
        validateUniqueTitle(userId, resumeId, request.title(), sectionId);
        validateUniqueType(userId, resumeId, parsedType, sectionId);

        section.setTitle(request.title().trim());
        section.setType(parsedType);
        section.setContent(request.content().trim());
        section.setVisible(request.visible());
        section.setAiGenerated(Boolean.TRUE.equals(request.aiGenerated()));

        return toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionResponse reorderSection(Long userId, Long resumeId, Long sectionId, int position) {
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        Section section = sections.stream()
                .filter(item -> item.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Section not found for this resume"));

        if (position < 1 || position > sections.size()) {
            throw new BadRequestException("Position must be between 1 and " + sections.size());
        }

        sections.remove(section);
        sections.add(position - 1, section);

        for (int index = 0; index < sections.size(); index++) {
            sections.get(index).setDisplayOrder(index + 1);
        }

        sectionRepository.saveAll(sections);
        return toResponse(section);
    }

    @Override
    @Transactional
    public SectionResponse toggleVisibility(Long userId, Long resumeId, Long sectionId) {
        Section section = getOwnedSection(userId, resumeId, sectionId);
        section.setVisible(!section.isVisible());
        return toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public List<SectionResponse> bulkUpdateSections(Long userId, Long resumeId, BulkUpdateSectionsRequest request) {
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        if (sections.size() != request.sections().size()) {
            throw new BadRequestException("Bulk update payload must match existing section count");
        }

        List<String> normalizedTitles = request.sections().stream()
                .map(item -> item.title().trim().toLowerCase())
                .toList();
        long uniqueTitles = normalizedTitles.stream().distinct().count();
        if (uniqueTitles != normalizedTitles.size()) {
            throw new BadRequestException("Duplicate section titles are not allowed");
        }

        List<SectionType> nonCustomTypes = request.sections().stream()
                .map(item -> parseType(item.type()))
                .filter(type -> type != SectionType.CUSTOM)
                .toList();
        long uniqueNonCustomTypes = nonCustomTypes.stream().distinct().count();
        if (uniqueNonCustomTypes != nonCustomTypes.size()) {
            throw new BadRequestException("Duplicate section types are not allowed except CUSTOM");
        }

        for (int index = 0; index < sections.size(); index++) {
            UpdateSectionRequest item = request.sections().get(index);
            Section section = sections.get(index);
            section.setTitle(item.title().trim());
            section.setType(parseType(item.type()));
            section.setContent(item.content().trim());
            section.setVisible(item.visible());
            section.setAiGenerated(Boolean.TRUE.equals(item.aiGenerated()));
            section.setDisplayOrder(index + 1);
        }

        return sectionRepository.saveAll(sections).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteAllSections(Long userId, Long resumeId) {
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        sectionRepository.deleteAll(sections);
    }

    @Override
    @Transactional
    public void deleteSection(Long userId, Long resumeId, Long sectionId) {
        Section section = getOwnedSection(userId, resumeId, sectionId);
        sectionRepository.delete(section);
        normalizeOrder(userId, resumeId);
    }

    private void normalizeOrder(Long userId, Long resumeId) {
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        for (int index = 0; index < sections.size(); index++) {
            sections.get(index).setDisplayOrder(index + 1);
        }
        sectionRepository.saveAll(sections);
    }

    private Section getOwnedSection(Long userId, Long resumeId, Long sectionId) {
        return sectionRepository.findByIdAndResumeIdAndUserId(sectionId, resumeId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found for this resume"));
    }

    private void validateUniqueTitle(Long userId, Long resumeId, String title, Long sectionId) {
        String normalizedTitle = title.trim();
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        boolean duplicate = sections.stream()
                .anyMatch(section -> !section.getId().equals(sectionId)
                        && section.getTitle().equalsIgnoreCase(normalizedTitle));
        if (duplicate) {
            throw new BadRequestException("Section title already exists for this resume");
        }
    }

    private void validateUniqueType(Long userId, Long resumeId, SectionType type, Long sectionId) {
        if (type == SectionType.CUSTOM) {
            return;
        }
        List<Section> sections = sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAsc(resumeId, userId);
        boolean duplicate = sections.stream()
                .anyMatch(section -> !section.getId().equals(sectionId) && section.getType() == type);
        if (duplicate) {
            throw new BadRequestException("Section type already exists for this resume");
        }
    }

    private SectionType parseType(String type) {
        try {
            return SectionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid type. Allowed values: PERSONAL_INFO, SUMMARY, EXPERIENCE, EDUCATION, PROJECTS, SKILLS, CERTIFICATIONS, LANGUAGES, VOLUNTEER, CUSTOM");
        }
    }

    private SectionResponse toResponse(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getResumeId(),
                section.getUserId(),
                section.getTitle(),
                section.getType(),
                section.getContent(),
                section.getDisplayOrder(),
                section.isVisible(),
                section.isAiGenerated(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }
}
