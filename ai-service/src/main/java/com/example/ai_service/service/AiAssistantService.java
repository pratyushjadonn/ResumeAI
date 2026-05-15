package com.example.ai_service.service;

import com.example.ai_service.dto.request.AtsCheckRequest;
import com.example.ai_service.dto.request.CoverLetterRequest;
import com.example.ai_service.dto.request.GenerateBulletsRequest;
import com.example.ai_service.dto.request.GenerateSectionSummaryRequest;
import com.example.ai_service.dto.request.GenerateSummaryRequest;
import com.example.ai_service.dto.request.ImproveSectionRequest;
import com.example.ai_service.dto.request.JobMatchRequest;
import com.example.ai_service.dto.request.SkillSuggestionsRequest;
import com.example.ai_service.dto.request.TailorResumeRequest;
import com.example.ai_service.dto.request.TranslateResumeRequest;
import com.example.ai_service.dto.response.AiHistoryItemResponse;
import com.example.ai_service.dto.response.AtsCheckResponse;
import com.example.ai_service.dto.response.CoverLetterResponse;
import com.example.ai_service.dto.response.BulletsResponse;
import com.example.ai_service.dto.response.ImproveSectionResponse;
import com.example.ai_service.dto.response.JobMatchResponse;
import com.example.ai_service.dto.response.QuotaResponse;
import com.example.ai_service.dto.response.SectionSummaryResponse;
import com.example.ai_service.dto.response.SkillSuggestionsResponse;
import com.example.ai_service.dto.response.SummaryResponse;
import com.example.ai_service.dto.response.TailorResumeResponse;
import com.example.ai_service.dto.response.TranslationResponse;

import java.util.List;

public interface AiAssistantService {

    SummaryResponse generateSummary(Long userId, boolean premium, GenerateSummaryRequest request);

    BulletsResponse generateBullets(Long userId, boolean premium, GenerateBulletsRequest request);

    JobMatchResponse analyzeJobMatch(Long userId, boolean premium, JobMatchRequest request);

    CoverLetterResponse generateCoverLetter(Long userId, boolean premium, CoverLetterRequest request);

    ImproveSectionResponse improveSection(Long userId, boolean premium, ImproveSectionRequest request);

    SectionSummaryResponse generateSectionSummary(Long userId, boolean premium, GenerateSectionSummaryRequest request);

    AtsCheckResponse checkAts(Long userId, boolean premium, AtsCheckRequest request);

    SkillSuggestionsResponse suggestSkills(Long userId, boolean premium, SkillSuggestionsRequest request);

    TailorResumeResponse tailorResumeForJob(Long userId, boolean premium, TailorResumeRequest request);

    TranslationResponse translateResume(Long userId, boolean premium, TranslateResumeRequest request);

    List<AiHistoryItemResponse> getHistory(Long userId);

    QuotaResponse getQuota(Long userId, boolean premium);
}
