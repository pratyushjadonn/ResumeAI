package com.example.jobmatch_service.controller;

import com.example.jobmatch_service.dto.AnalyzeJobRequest;
import com.example.jobmatch_service.dto.FetchJobsRequest;
import com.example.jobmatch_service.model.JobMatch;
import com.example.jobmatch_service.service.JobMatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-matches")
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping("/analyze")
    public JobMatch analyze(@Valid @RequestBody AnalyzeJobRequest request) {
        return jobMatchService.analyze(request);
    }

    @GetMapping("/resume/{resumeId}")
    public List<JobMatch> getByResume(@PathVariable Long resumeId) {
        return jobMatchService.getByResume(resumeId);
    }

    @GetMapping
    public List<JobMatch> getByUser(@RequestParam Long userId) {
        return jobMatchService.getByUser(userId);
    }

    @PostMapping("/{matchId}/bookmark")
    public JobMatch bookmark(@PathVariable Long matchId) {
        return jobMatchService.bookmark(matchId);
    }

    @PostMapping("/fetch/linkedin")
    public List<JobMatch> fetchLinkedIn(@Valid @RequestBody FetchJobsRequest request) {
        return jobMatchService.fetchLinkedIn(request);
    }

    @PostMapping("/fetch/naukri")
    public List<JobMatch> fetchNaukri(@Valid @RequestBody FetchJobsRequest request) {
        return jobMatchService.fetchNaukri(request);
    }
}
