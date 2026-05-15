package com.example.jobmatch_service.service;

import com.example.jobmatch_service.dto.AnalyzeJobRequest;
import com.example.jobmatch_service.dto.FetchJobsRequest;
import com.example.jobmatch_service.model.JobMatch;

import java.util.List;

public interface JobMatchService {

    JobMatch analyze(AnalyzeJobRequest request);

    List<JobMatch> getByResume(Long resumeId);

    List<JobMatch> getByUser(Long userId);

    JobMatch bookmark(Long matchId);

    List<JobMatch> fetchLinkedIn(FetchJobsRequest request);

    List<JobMatch> fetchNaukri(FetchJobsRequest request);
}
