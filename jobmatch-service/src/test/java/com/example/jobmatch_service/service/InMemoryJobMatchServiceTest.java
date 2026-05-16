package com.example.jobmatch_service.service;

import com.example.jobmatch_service.dto.AnalyzeJobRequest;
import com.example.jobmatch_service.dto.FetchJobsRequest;
import com.example.jobmatch_service.model.JobMatch;
import com.example.jobmatch_service.model.JobMatchSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryJobMatchServiceTest {

    private final InMemoryJobMatchService service = new InMemoryJobMatchService();

    @Test
    void analyzeStoresMatchAndTracksMissingSkills() {
        AnalyzeJobRequest request = new AnalyzeJobRequest(
                1L,
                10L,
                "Java Developer",
                "Need Java, metrics, and domain-fit",
                "Java experience with solid communication"
        );

        JobMatch match = service.analyze(request);
        List<JobMatch> matchesByResume = service.getByResume(10L);

        assertEquals(10L, match.resumeId());
        assertEquals(1L, match.userId());
        assertEquals(JobMatchSource.MANUAL, match.source());
        assertTrue(match.matchScore() >= 35);
        assertFalse(match.missingSkills().isEmpty());
        assertEquals(List.of(match), matchesByResume);
    }

    @Test
    void getByUserSortsHigherScoresFirstAndBookmarkUpdatesMatch() {
        JobMatch lowerScore = service.analyze(new AnalyzeJobRequest(
                2L,
                21L,
                "Platform Engineer",
                "Need metrics and domain-fit",
                "General backend profile"
        ));
        JobMatch higherScore = service.analyze(new AnalyzeJobRequest(
                2L,
                22L,
                "Platform Engineer",
                "Need keywords, metrics and domain-fit",
                "keywords metrics domain-fit"
        ));

        List<JobMatch> matches = service.getByUser(2L);
        JobMatch bookmarked = service.bookmark(lowerScore.matchId());

        assertEquals(higherScore.matchId(), matches.getFirst().matchId());
        assertTrue(bookmarked.bookmarked());
        assertEquals(lowerScore.matchId(), bookmarked.matchId());
    }

    @Test
    void fetchLinkedInAndNaukriSeedSourceSpecificMatches() {
        FetchJobsRequest request = new FetchJobsRequest("Backend Engineer", "Remote");

        List<JobMatch> linkedInMatches = service.fetchLinkedIn(request);
        List<JobMatch> naukriMatches = service.fetchNaukri(request);

        assertEquals(2, linkedInMatches.size());
        assertEquals(JobMatchSource.LINKEDIN, linkedInMatches.getFirst().source());
        assertEquals(JobMatchSource.NAUKRI, naukriMatches.getFirst().source());
        assertTrue(linkedInMatches.getFirst().jobTitle().startsWith("Backend Engineer"));
    }
}
