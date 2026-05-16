package com.example.ai_service.controller;

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
import com.example.ai_service.dto.response.BulletsResponse;
import com.example.ai_service.dto.response.CoverLetterResponse;
import com.example.ai_service.dto.response.ImproveSectionResponse;
import com.example.ai_service.dto.response.JobMatchResponse;
import com.example.ai_service.dto.response.QuotaResponse;
import com.example.ai_service.dto.response.SectionSummaryResponse;
import com.example.ai_service.dto.response.SkillSuggestionsResponse;
import com.example.ai_service.dto.response.SummaryResponse;
import com.example.ai_service.dto.response.TailorResumeResponse;
import com.example.ai_service.dto.response.TranslationResponse;
import com.example.ai_service.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private static final String PREMIUM_PLAN = "PREMIUM";

    private final AiAssistantService aiAssistantService;

    @PostMapping("/summary")
    public SummaryResponse generateSummary(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody GenerateSummaryRequest request) {
        return aiAssistantService.generateSummary(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/bullets")
    public BulletsResponse generateBullets(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody GenerateBulletsRequest request) {
        return aiAssistantService.generateBullets(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/match")
    public JobMatchResponse analyzeJobMatch(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody JobMatchRequest request) {
        return aiAssistantService.analyzeJobMatch(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/cover-letter")
    public CoverLetterResponse generateCoverLetter(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody CoverLetterRequest request) {
        return aiAssistantService.generateCoverLetter(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/improve-section")
    public ImproveSectionResponse improveSection(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody ImproveSectionRequest request) {
        return aiAssistantService.improveSection(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/section-summary")
    public SectionSummaryResponse generateSectionSummary(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody GenerateSectionSummaryRequest request) {
        return aiAssistantService.generateSectionSummary(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/ats")
    public AtsCheckResponse checkAts(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody AtsCheckRequest request) {
        return aiAssistantService.checkAts(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/skills")
    public SkillSuggestionsResponse suggestSkills(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody SkillSuggestionsRequest request) {
        return aiAssistantService.suggestSkills(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/tailor")
    public TailorResumeResponse tailorResumeForJob(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody TailorResumeRequest request) {
        return aiAssistantService.tailorResumeForJob(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @PostMapping("/translate")
    public TranslationResponse translateResume(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody TranslateResumeRequest request) {
        return aiAssistantService.translateResume(userId, PREMIUM_PLAN.equalsIgnoreCase(plan), request);
    }

    @GetMapping("/history")
    public List<AiHistoryItemResponse> getHistory(@RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId) {
        return aiAssistantService.getHistory(userId);
    }

    @GetMapping("/quota")
    public QuotaResponse getQuota(@RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
                                  @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan) {
        return aiAssistantService.getQuota(userId, PREMIUM_PLAN.equalsIgnoreCase(plan));
    }
}
