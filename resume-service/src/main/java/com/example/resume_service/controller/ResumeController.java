package com.example.resume_service.controller;

import com.example.resume_service.dto.request.CreateResumeRequest;
import com.example.resume_service.dto.request.UpdateResumeRequest;
import com.example.resume_service.dto.response.ResumeResponse;
import com.example.resume_service.service.ResumeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

@Validated
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse createResume(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody CreateResumeRequest request) {
        return resumeService.createResume(userId, request);
    }

    @GetMapping
    public Page<ResumeResponse> getResumes(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "10") @Min(1) int size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String search) {
        return resumeService.getResumes(userId, page, size, status, search);
    }

    @GetMapping("/{resumeId}")
    public ResumeResponse getResume(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long resumeId) {
        return resumeService.getResume(userId, resumeId);
    }

    @GetMapping("/public")
    public Page<ResumeResponse> getPublicResumes(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "10") @Min(1) int size) {
        return resumeService.getPublicResumes(page, size);
    }

    @GetMapping("/template/{templateId}")
    public Page<ResumeResponse> getResumesByTemplate(@PathVariable Long templateId,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "10") @Min(1) int size) {
        return resumeService.getResumesByTemplate(templateId, page, size);
    }

    @PutMapping("/{resumeId}")
    public ResumeResponse updateResume(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long resumeId,
                                       @Valid @RequestBody UpdateResumeRequest request) {
        return resumeService.updateResume(userId, resumeId, request);
    }

    @PostMapping("/{resumeId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse duplicateResume(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable Long resumeId) {
        return resumeService.duplicateResume(userId, resumeId);
    }

    @PatchMapping("/{resumeId}/publish")
    public ResumeResponse publishResume(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long resumeId) {
        return resumeService.publishResume(userId, resumeId);
    }

    @PatchMapping("/{resumeId}/unpublish")
    public ResumeResponse unpublishResume(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable Long resumeId) {
        return resumeService.unpublishResume(userId, resumeId);
    }

    @PatchMapping("/{resumeId}/ats-score")
    public ResumeResponse updateAtsScore(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable Long resumeId,
                                         @RequestParam @Min(0) @Max(100) Integer score) {
        return resumeService.updateAtsScore(userId, resumeId, score);
    }

    @PatchMapping("/{resumeId}/views")
    public ResumeResponse incrementViewCount(@PathVariable Long resumeId) {
        return resumeService.incrementViewCount(resumeId);
    }

    @PatchMapping("/{resumeId}/archive")
    public ResumeResponse archiveResume(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long resumeId) {
        return resumeService.archiveResume(userId, resumeId);
    }

    @DeleteMapping("/{resumeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResume(@RequestHeader("X-User-Id") Long userId,
                             @PathVariable Long resumeId) {
        resumeService.deleteResume(userId, resumeId);
    }
}
