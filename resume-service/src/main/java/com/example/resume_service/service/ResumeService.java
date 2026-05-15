package com.example.resume_service.service;

import com.example.resume_service.dto.request.CreateResumeRequest;
import com.example.resume_service.dto.request.UpdateResumeRequest;
import com.example.resume_service.dto.response.ResumeResponse;
import org.springframework.data.domain.Page;

public interface ResumeService {

    ResumeResponse createResume(Long userId, CreateResumeRequest request);

    Page<ResumeResponse> getResumes(Long userId, int page, int size, String status, String search);

    ResumeResponse getResume(Long userId, Long resumeId);

    ResumeResponse updateResume(Long userId, Long resumeId, UpdateResumeRequest request);

    ResumeResponse duplicateResume(Long userId, Long resumeId);

    ResumeResponse publishResume(Long userId, Long resumeId);

    ResumeResponse unpublishResume(Long userId, Long resumeId);

    ResumeResponse archiveResume(Long userId, Long resumeId);

    ResumeResponse updateAtsScore(Long userId, Long resumeId, Integer atsScore);

    Page<ResumeResponse> getPublicResumes(int page, int size);

    Page<ResumeResponse> getResumesByTemplate(Long templateId, int page, int size);

    ResumeResponse incrementViewCount(Long resumeId);

    void deleteResume(Long userId, Long resumeId);
}
