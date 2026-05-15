package com.example.section_service.controller;

import com.example.section_service.dto.request.BulkUpdateSectionsRequest;
import com.example.section_service.dto.request.CreateSectionRequest;
import com.example.section_service.dto.request.UpdateSectionRequest;
import com.example.section_service.dto.response.SectionResponse;
import com.example.section_service.service.SectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable Long resumeId,
                                         @Valid @RequestBody CreateSectionRequest request) {
        return sectionService.createSection(userId, resumeId, request);
    }

    @GetMapping
    public List<SectionResponse> getSections(@RequestHeader("X-User-Id") Long userId,
                                             @PathVariable Long resumeId,
                                             @RequestParam(required = false) String type) {
        return type == null || type.isBlank()
                ? sectionService.getSections(userId, resumeId)
                : sectionService.getSectionsByType(userId, resumeId, type);
    }

    @GetMapping("/{sectionId}")
    public SectionResponse getSection(@RequestHeader("X-User-Id") Long userId,
                                      @PathVariable Long resumeId,
                                      @PathVariable Long sectionId) {
        return sectionService.getSection(userId, resumeId, sectionId);
    }

    @PutMapping("/{sectionId}")
    public SectionResponse updateSection(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable Long resumeId,
                                         @PathVariable Long sectionId,
                                         @Valid @RequestBody UpdateSectionRequest request) {
        return sectionService.updateSection(userId, resumeId, sectionId, request);
    }

    @PatchMapping("/{sectionId}/reorder")
    public SectionResponse reorderSection(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable Long resumeId,
                                          @PathVariable Long sectionId,
                                          @RequestParam @Min(1) int position) {
        return sectionService.reorderSection(userId, resumeId, sectionId, position);
    }

    @PatchMapping("/{sectionId}/visibility")
    public SectionResponse toggleVisibility(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long resumeId,
                                            @PathVariable Long sectionId) {
        return sectionService.toggleVisibility(userId, resumeId, sectionId);
    }

    @PutMapping("/bulk")
    public List<SectionResponse> bulkUpdateSections(@RequestHeader("X-User-Id") Long userId,
                                                    @PathVariable Long resumeId,
                                                    @Valid @RequestBody BulkUpdateSectionsRequest request) {
        return sectionService.bulkUpdateSections(userId, resumeId, request);
    }

    @DeleteMapping("/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@RequestHeader("X-User-Id") Long userId,
                              @PathVariable Long resumeId,
                              @PathVariable Long sectionId) {
        sectionService.deleteSection(userId, resumeId, sectionId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllSections(@RequestHeader("X-User-Id") Long userId,
                                  @PathVariable Long resumeId) {
        sectionService.deleteAllSections(userId, resumeId);
    }
}
