package com.example.ai_service.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.Instant;

/**
 * Event message for AI quota reset scheduling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaResetEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("quota_type")
    private String quotaType; // GENERATION, ATS

    @JsonProperty("previous_quota")
    private Integer previousQuota;

    @JsonProperty("new_quota")
    private Integer newQuota;

    @JsonProperty("reset_date")
    private String resetDate; // YYYY-MM format

    @JsonProperty("is_premium")
    private Boolean isPremium;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("event_id")
    private String eventId;
}
