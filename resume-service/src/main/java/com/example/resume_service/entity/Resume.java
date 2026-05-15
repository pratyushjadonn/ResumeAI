package com.example.resume_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "resumes",
        indexes = {
                @Index(name = "idx_resume_user_status", columnList = "user_id,status"),
                @Index(name = "idx_resume_user_title", columnList = "user_id,title")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "target_role", length = 120)
    private String targetRole;

    @Column(name = "template_key", length = 60)
    private String templateKey;

    @Column(name = "template_id")
    private Long templateId;

    @Column(length = 1000)
    private String summary;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(length = 30)
    private String language;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResumeStatus status;

    @Column(nullable = false)
    private int version;

    @Column(name = "published_at")
    private Instant publishedAt;
}
