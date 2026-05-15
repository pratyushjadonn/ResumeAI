package com.example.ai_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
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
        name = "ai_requests",
        indexes = {
                @Index(name = "idx_ai_request_user_created", columnList = "user_id,created_at"),
                @Index(name = "idx_ai_request_type_created", columnList = "request_type,created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long requestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "request_type", nullable = false, length = 40)
    private String requestType;

    @Column(name = "input_prompt", nullable = false, length = 20000)
    private String inputPrompt;

    @Column(name = "ai_response", nullable = false, length = 20000)
    private String aiResponse;

    @Column(name = "model_used", nullable = false, length = 80)
    private String modelUsed;

    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
