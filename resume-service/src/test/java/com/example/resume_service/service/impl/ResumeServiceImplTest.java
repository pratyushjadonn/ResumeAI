package com.example.resume_service.service.impl;

import com.example.resume_service.dto.request.CreateResumeRequest;
import com.example.resume_service.dto.response.ResumeResponse;
import com.example.resume_service.entity.Resume;
import com.example.resume_service.entity.ResumeStatus;
import com.example.resume_service.messaging.ExportProducer;
import com.example.resume_service.messaging.NotificationProducer;
import com.example.resume_service.repository.ResumeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private ExportProducer exportProducer;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @Test
    void createResume_success() {
        CreateResumeRequest request = new CreateResumeRequest(
                "Java Resume",
                "Backend Developer",
                "classic-pro",
                1L,
                "Experienced Java developer",
                "en"
        );
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume savedResume = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedResume, "id", 10L);
            return savedResume;
        });

        ResumeResponse response = resumeService.createResume(1L, request);

        assertEquals(10L, response.id());
        assertEquals("Java Resume", response.title());
        assertEquals(ResumeStatus.DRAFT, response.status());
        verify(notificationProducer).sendNotification(
                1L,
                "RESUME_CREATED",
                "user1@example.com",
                "Resume created successfully",
                "Your resume \"Java Resume\" has been created."
        );
        verify(exportProducer).sendExportRequest(10L, "user1@example.com", "PDF");
    }

    @Test
    void getResumeById_success() {
        Resume resume = buildResume(11L, 1L, "Backend Resume");
        when(resumeRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(resume));

        ResumeResponse response = resumeService.getResume(1L, 11L);

        assertEquals(11L, response.id());
        assertEquals("Backend Resume", response.title());
    }

    @Test
    void getResumeById_missingResume() {
        when(resumeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> resumeService.getResume(1L, 99L));
    }

    @Test
    void deleteResume_success() {
        Resume resume = buildResume(12L, 1L, "Delete Me");
        when(resumeRepository.findByIdAndUserId(12L, 1L)).thenReturn(Optional.of(resume));

        resumeService.deleteResume(1L, 12L);

        verify(resumeRepository).delete(resume);
    }

    @Test
    void duplicateResume_success() {
        Resume existingResume = buildResume(13L, 1L, "Original Resume");
        when(resumeRepository.findByIdAndUserId(13L, 1L)).thenReturn(Optional.of(existingResume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume duplicatedResume = invocation.getArgument(0);
            ReflectionTestUtils.setField(duplicatedResume, "id", 20L);
            return duplicatedResume;
        });

        ResumeResponse response = resumeService.duplicateResume(1L, 13L);

        assertEquals(20L, response.id());
        assertEquals("Original Resume Copy", response.title());
        assertEquals(1, response.version());
    }

    @Test
    void publishResume_success() {
        Resume resume = buildResume(14L, 1L, "Publish Me");
        resume.setSummary("Ready to publish");
        when(resumeRepository.findByIdAndUserId(14L, 1L)).thenReturn(Optional.of(resume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeResponse response = resumeService.publishResume(1L, 14L);

        assertEquals(ResumeStatus.PUBLISHED, response.status());
        assertTrue(response.isPublic());
    }

    private Resume buildResume(Long id, Long userId, String title) {
        Resume resume = Resume.builder()
                .userId(userId)
                .title(title)
                .targetRole("Backend Developer")
                .templateKey("classic-pro")
                .templateId(1L)
                .summary("Resume summary")
                .language("en")
                .isPublic(false)
                .viewCount(0L)
                .status(ResumeStatus.DRAFT)
                .version(1)
                .build();
        ReflectionTestUtils.setField(resume, "id", id);
        return resume;
    }
}
