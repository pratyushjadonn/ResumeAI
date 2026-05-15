package com.example.section_service.service;

import com.example.section_service.dto.request.BulkUpdateSectionsRequest;
import com.example.section_service.dto.request.CreateSectionRequest;
import com.example.section_service.dto.request.UpdateSectionRequest;
import com.example.section_service.dto.response.SectionResponse;

import java.util.List;

public interface SectionService {

    SectionResponse createSection(Long userId, Long resumeId, CreateSectionRequest request);

    List<SectionResponse> getSections(Long userId, Long resumeId);

    SectionResponse getSection(Long userId, Long resumeId, Long sectionId);

    SectionResponse updateSection(Long userId, Long resumeId, Long sectionId, UpdateSectionRequest request);

    SectionResponse reorderSection(Long userId, Long resumeId, Long sectionId, int position);

    SectionResponse toggleVisibility(Long userId, Long resumeId, Long sectionId);

    List<SectionResponse> getSectionsByType(Long userId, Long resumeId, String type);

    List<SectionResponse> bulkUpdateSections(Long userId, Long resumeId, BulkUpdateSectionsRequest request);

    void deleteAllSections(Long userId, Long resumeId);

    void deleteSection(Long userId, Long resumeId, Long sectionId);
}
