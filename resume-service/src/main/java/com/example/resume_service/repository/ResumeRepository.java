package com.example.resume_service.repository;

import com.example.resume_service.entity.Resume;
import com.example.resume_service.entity.ResumeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    Page<Resume> findByUserId(Long userId, Pageable pageable);

    Page<Resume> findByUserIdAndStatus(Long userId, ResumeStatus status, Pageable pageable);

    @Query("""
            select r from Resume r
            where r.userId = :userId
              and lower(r.title) like lower(concat('%', :title, '%'))
            """)
    Page<Resume> findByUserIdAndTitleContainingIgnoreCase(@Param("userId") Long userId,
                                                          @Param("title") String title,
                                                          Pageable pageable);

    @Query("""
            select r from Resume r
            where r.userId = :userId
              and r.status = :status
              and lower(r.title) like lower(concat('%', :title, '%'))
            """)
    Page<Resume> findByUserIdAndStatusAndTitleContainingIgnoreCase(@Param("userId") Long userId,
                                                                   @Param("status") ResumeStatus status,
                                                                   @Param("title") String title,
                                                                   Pageable pageable);

    Page<Resume> findByTemplateId(Long templateId, Pageable pageable);

    Page<Resume> findByIsPublicTrue(Pageable pageable);
}
