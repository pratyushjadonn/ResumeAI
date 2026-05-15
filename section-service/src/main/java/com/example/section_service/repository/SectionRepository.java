package com.example.section_service.repository;

import com.example.section_service.entity.Section;
import com.example.section_service.entity.SectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    Optional<Section> findByIdAndResumeIdAndUserId(Long id, Long resumeId, Long userId);

    List<Section> findByResumeIdAndUserIdOrderByDisplayOrderAsc(Long resumeId, Long userId);

    boolean existsByResumeIdAndUserIdAndTitleIgnoreCase(Long resumeId, Long userId, String title);

    boolean existsByResumeIdAndUserIdAndType(Long resumeId, Long userId, SectionType type);

    long countByResumeIdAndUserId(Long resumeId, Long userId);
}
