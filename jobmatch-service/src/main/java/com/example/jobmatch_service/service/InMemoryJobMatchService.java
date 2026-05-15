package com.example.jobmatch_service.service;

import com.example.jobmatch_service.dto.AnalyzeJobRequest;
import com.example.jobmatch_service.dto.FetchJobsRequest;
import com.example.jobmatch_service.model.JobMatch;
import com.example.jobmatch_service.model.JobMatchSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryJobMatchService implements JobMatchService {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, JobMatch> matches = new ConcurrentHashMap<>();

    @Override
    public JobMatch analyze(AnalyzeJobRequest request) {
        List<String> missingSkills = List.of("keywords", "metrics", "domain-fit").stream()
                .filter(item -> !request.resumeText().toLowerCase().contains(item))
                .toList();
        int score = Math.max(35, 100 - (missingSkills.size() * 15));
        JobMatch match = new JobMatch(
                ids.getAndIncrement(),
                request.resumeId(),
                request.userId(),
                request.jobTitle(),
                request.jobDescription(),
                score,
                missingSkills,
                "Add missing keywords, align summary to the role, and quantify impact in experience bullets.",
                JobMatchSource.MANUAL,
                Instant.now(),
                false
        );
        matches.put(match.matchId(), match);
        return match;
    }

    @Override
    public List<JobMatch> getByResume(Long resumeId) {
        return matches.values().stream()
                .filter(match -> match.resumeId().equals(resumeId))
                .sorted(Comparator.comparing(JobMatch::matchedAt).reversed())
                .toList();
    }

    @Override
    public List<JobMatch> getByUser(Long userId) {
        return matches.values().stream()
                .filter(match -> match.userId().equals(userId))
                .sorted(Comparator.comparing(JobMatch::matchScore).reversed())
                .toList();
    }

    @Override
    public JobMatch bookmark(Long matchId) {
        JobMatch current = matches.get(matchId);
        JobMatch updated = new JobMatch(
                current.matchId(),
                current.resumeId(),
                current.userId(),
                current.jobTitle(),
                current.jobDescription(),
                current.matchScore(),
                current.missingSkills(),
                current.recommendations(),
                current.source(),
                current.matchedAt(),
                true
        );
        matches.put(matchId, updated);
        return updated;
    }

    @Override
    public List<JobMatch> fetchLinkedIn(FetchJobsRequest request) {
        return seed(request, JobMatchSource.LINKEDIN);
    }

    @Override
    public List<JobMatch> fetchNaukri(FetchJobsRequest request) {
        return seed(request, JobMatchSource.NAUKRI);
    }

    private List<JobMatch> seed(FetchJobsRequest request, JobMatchSource source) {
        return List.of(
                new JobMatch(ids.getAndIncrement(), 0L, 0L, request.jobTitle() + " I", request.location(), 78,
                        List.of("leadership"), "Good alignment; add leadership examples.", source, Instant.now(), false),
                new JobMatch(ids.getAndIncrement(), 0L, 0L, request.jobTitle() + " II", request.location(), 72,
                        List.of("ats"), "Strong fit; tune keywords and ATS phrasing.", source, Instant.now(), false)
        );
    }
}
