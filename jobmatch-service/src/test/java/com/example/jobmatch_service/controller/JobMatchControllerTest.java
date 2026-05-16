package com.example.jobmatch_service.controller;

import com.example.jobmatch_service.dto.AnalyzeJobRequest;
import com.example.jobmatch_service.dto.FetchJobsRequest;
import com.example.jobmatch_service.model.JobMatch;
import com.example.jobmatch_service.model.JobMatchSource;
import com.example.jobmatch_service.service.JobMatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchControllerTest {

    @Mock
    private JobMatchService jobMatchService;

    @Test
    void delegatesAllEndpoints() {
        JobMatchController controller = new JobMatchController(jobMatchService);
        AnalyzeJobRequest analyzeRequest = new AnalyzeJobRequest(1L, 2L, "Java Developer", "job", "resume");
        FetchJobsRequest fetchJobsRequest = new FetchJobsRequest("Backend Engineer", "Remote");
        JobMatch jobMatch = new JobMatch(
                50L,
                2L,
                1L,
                "Java Developer",
                "job",
                82,
                List.of("aws"),
                "add aws",
                JobMatchSource.MANUAL,
                Instant.now(),
                false
        );
        List<JobMatch> matches = List.of(jobMatch);

        when(jobMatchService.analyze(analyzeRequest)).thenReturn(jobMatch);
        when(jobMatchService.getByResume(2L)).thenReturn(matches);
        when(jobMatchService.getByUser(1L)).thenReturn(matches);
        when(jobMatchService.bookmark(50L)).thenReturn(jobMatch);
        when(jobMatchService.fetchLinkedIn(fetchJobsRequest)).thenReturn(matches);
        when(jobMatchService.fetchNaukri(fetchJobsRequest)).thenReturn(matches);

        assertEquals(jobMatch, controller.analyze(analyzeRequest));
        assertEquals(matches, controller.getByResume(2L));
        assertEquals(matches, controller.getByUser(1L));
        assertEquals(jobMatch, controller.bookmark(50L));
        assertEquals(matches, controller.fetchLinkedIn(fetchJobsRequest));
        assertEquals(matches, controller.fetchNaukri(fetchJobsRequest));

        verify(jobMatchService).analyze(analyzeRequest);
        verify(jobMatchService).getByResume(2L);
        verify(jobMatchService).getByUser(1L);
        verify(jobMatchService).bookmark(50L);
        verify(jobMatchService).fetchLinkedIn(fetchJobsRequest);
        verify(jobMatchService).fetchNaukri(fetchJobsRequest);
    }
}
