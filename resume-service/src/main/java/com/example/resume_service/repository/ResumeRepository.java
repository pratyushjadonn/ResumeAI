package com.example.resume_service.repository;

import com.example.resume_service.entity.Resume;
import com.example.resume_service.entity.ResumeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    Page<Resume> findByUserId(Long userId, Pageable pageable);

    Page<Resume> findByUserIdAndStatus(Long userId, ResumeStatus status, Pageable pageable);

    Page<Resume> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title, Pageable pageable);

    Page<Resume> findByUserIdAndStatusAndTitleContainingIgnoreCase(Long userId,
                                                                   ResumeStatus status,
                                                                   String title,
                                                                   Pageable pageable);

    Page<Resume> findByTemplateId(Long templateId, Pageable pageable);

    Page<Resume> findByIsPublicTrue(Pageable pageable);
}
