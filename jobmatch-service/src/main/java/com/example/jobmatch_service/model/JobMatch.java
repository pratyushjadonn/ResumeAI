package com.example.jobmatch_service.model;

import java.time.Instant;
import java.util.List;

public record JobMatch(
        Long matchId,
        Long resumeId,
        Long userId,
        String jobTitle,
        String jobDescription,
        int matchScore,
        List<String> missingSkills,
        String recommendations,
        JobMatchSource source,
        Instant matchedAt,
        boolean bookmarked
) {
}
