package com.example.resume_service.service.impl;

import com.example.resume_service.dto.request.CreateResumeRequest;
import com.example.resume_service.dto.request.UpdateResumeRequest;
import com.example.resume_service.dto.response.ResumeResponse;
import com.example.resume_service.entity.Resume;
import com.example.resume_service.entity.ResumeStatus;
import com.example.resume_service.exception.BadRequestException;
import com.example.resume_service.messaging.ExportProducer;
import com.example.resume_service.messaging.NotificationProducer;
import com.example.resume_service.repository.ResumeRepository;
import com.example.resume_service.service.ResumeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final NotificationProducer notificationProducer;
    private final ExportProducer exportProducer;

    @Override
    @Transactional
    public ResumeResponse createResume(Long userId, CreateResumeRequest request) {
        Resume resume = Resume.builder()
                .userId(userId)
                .title(request.title().trim())
                .targetRole(normalize(request.targetRole()))
                .templateKey(defaultTemplate(request.templateKey()))
                .templateId(request.templateId())
                .summary(normalize(request.summary()))
                .atsScore(null)
                .language(defaultLanguage(request.language()))
                .isPublic(false)
                .viewCount(0L)
                .status(ResumeStatus.DRAFT)
                .version(1)
                .build();
        Resume savedResume = resumeRepository.save(resume);
        publishResumeCreatedEvents(savedResume);
        return toResponse(savedResume);
    }

    @Override
    public Page<ResumeResponse> getResumes(Long userId, int page, int size, String status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        String normalizedSearch = normalize(search);

        Page<Resume> resumes;
        if (status != null && !status.isBlank()) {
            ResumeStatus resumeStatus = parseStatus(status);
            resumes = normalizedSearch == null
                    ? resumeRepository.findByUserIdAndStatus(userId, resumeStatus, pageable)
                    : resumeRepository.findByUserIdAndStatusAndTitleContainingIgnoreCase(
                    userId, resumeStatus, normalizedSearch, pageable);
        } else {
            resumes = normalizedSearch == null
                    ? resumeRepository.findByUserId(userId, pageable)
                    : resumeRepository.findByUserIdAndTitleContainingIgnoreCase(userId, normalizedSearch, pageable);
        }

        return resumes.map(this::toResponse);
    }

    @Override
    public ResumeResponse getResume(Long userId, Long resumeId) {
        return toResponse(getOwnedResume(userId, resumeId));
    }

    @Override
    @Transactional
    public ResumeResponse updateResume(Long userId, Long resumeId, UpdateResumeRequest request) {
        Resume resume = getOwnedResume(userId, resumeId);
        resume.setTitle(request.title().trim());
        resume.setTargetRole(normalize(request.targetRole()));
        resume.setTemplateKey(defaultTemplate(request.templateKey()));
        resume.setTemplateId(request.templateId());
        resume.setSummary(normalize(request.summary()));
        resume.setLanguage(defaultLanguage(request.language()));
        resume.setVersion(resume.getVersion() + 1);
        if (resume.getStatus() == ResumeStatus.ARCHIVED) {
            resume.setStatus(ResumeStatus.DRAFT);
        }
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse duplicateResume(Long userId, Long resumeId) {
        Resume existing = getOwnedResume(userId, resumeId);
        Resume duplicated = Resume.builder()
                .userId(existing.getUserId())
                .title(existing.getTitle() + " Copy")
                .targetRole(existing.getTargetRole())
                .templateKey(existing.getTemplateKey())
                .templateId(existing.getTemplateId())
                .summary(existing.getSummary())
                .atsScore(existing.getAtsScore())
                .language(existing.getLanguage())
                .isPublic(false)
                .viewCount(0L)
                .status(ResumeStatus.DRAFT)
                .version(1)
                .build();
        return toResponse(resumeRepository.save(duplicated));
    }

    @Override
    @Transactional
    public ResumeResponse publishResume(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        if (resume.getSummary() == null || resume.getSummary().isBlank()) {
            throw new BadRequestException("Resume summary is required before publishing");
        }
        resume.setStatus(ResumeStatus.PUBLISHED);
        resume.setPublic(true);
        resume.setPublishedAt(Instant.now());
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse unpublishResume(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        resume.setPublic(false);
        if (resume.getStatus() == ResumeStatus.PUBLISHED) {
            resume.setStatus(ResumeStatus.COMPLETE);
        }
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse archiveResume(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        resume.setPublic(false);
        resume.setStatus(ResumeStatus.ARCHIVED);
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public ResumeResponse updateAtsScore(Long userId, Long resumeId, Integer atsScore) {
        Resume resume = getOwnedResume(userId, resumeId);
        resume.setAtsScore(atsScore);
        if (resume.getStatus() == ResumeStatus.DRAFT && atsScore != null) {
            resume.setStatus(ResumeStatus.COMPLETE);
        }
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    public Page<ResumeResponse> getPublicResumes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return resumeRepository.findByIsPublicTrue(pageable).map(this::toResponse);
    }

    @Override
    public Page<ResumeResponse> getResumesByTemplate(Long templateId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return resumeRepository.findByTemplateId(templateId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ResumeResponse incrementViewCount(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found"));
        resume.setViewCount(resume.getViewCount() + 1);
        return toResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        resumeRepository.delete(resume);
    }

    private Resume getOwnedResume(Long userId, Long resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found for this user"));
    }

    // This keeps the async demo easy to follow after a resume is created.
    private void publishResumeCreatedEvents(Resume resume) {
        String userEmail = buildDemoEmail(resume.getUserId());

        notificationProducer.sendNotification(
                resume.getUserId(),
                "RESUME_CREATED",
                userEmail,
                "Resume created successfully",
                "Your resume \"" + resume.getTitle() + "\" has been created."
        );

        exportProducer.sendExportRequest(
                resume.getId(),
                userEmail,
                "PDF"
        );
    }

    private ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getTitle(),
                resume.getTargetRole(),
                resume.getTemplateKey(),
                resume.getTemplateId(),
                resume.getSummary(),
                resume.getAtsScore(),
                resume.getLanguage(),
                resume.isPublic(),
                resume.getViewCount(),
                resume.getStatus(),
                resume.getVersion(),
                resume.getCreatedAt(),
                resume.getUpdatedAt(),
                resume.getPublishedAt()
        );
    }

    private ResumeStatus parseStatus(String status) {
        try {
            return ResumeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status. Allowed values: DRAFT, PUBLISHED, ARCHIVED");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultTemplate(String templateKey) {
        String normalized = normalize(templateKey);
        return normalized == null ? "classic-pro" : normalized;
    }

    private String defaultLanguage(String language) {
        String normalized = normalize(language);
        return normalized == null ? "en" : normalized;
    }

    private String buildDemoEmail(Long userId) {
        return "user" + userId + "@example.com";
    }
}
