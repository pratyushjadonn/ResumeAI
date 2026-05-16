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
class AiAssistantControllerTest {

    @Mock
    private AiAssistantService aiAssistantService;

    @Test
    void delegatesAllGenerationEndpoints() {
        AiAssistantController controller = new AiAssistantController(aiAssistantService);

        GenerateSummaryRequest summaryRequest = new GenerateSummaryRequest("Java Developer", 4, List.of("Java", "Spring"), "backend");
        GenerateBulletsRequest bulletsRequest = new GenerateBulletsRequest(
                "Backend Engineer", "ResumeAI", List.of("API design"), List.of("Java"), "20% faster"
        );
        JobMatchRequest jobMatchRequest = new JobMatchRequest("resume", "job");
        CoverLetterRequest coverLetterRequest = new CoverLetterRequest(1L, 2L, "Aman", "Engineer", "ResumeAI", "JD", "Highlights");
        ImproveSectionRequest improveSectionRequest = new ImproveSectionRequest(1L, 2L, "EXPERIENCE", "Built APIs", "friendly");
        GenerateSectionSummaryRequest sectionSummaryRequest =
                new GenerateSectionSummaryRequest(1L, 2L, "EXPERIENCE", "Backend Engineer", "Delivered APIs", "Senior Engineer");
        AtsCheckRequest atsCheckRequest = new AtsCheckRequest(1L, 2L, "resume", "job");
        SkillSuggestionsRequest skillSuggestionsRequest = new SkillSuggestionsRequest(1L, "Platform Engineer");
        TailorResumeRequest tailorResumeRequest = new TailorResumeRequest(1L, 2L, "{\"summary\":\"Hi\"}", "Need Java");
        TranslateResumeRequest translateResumeRequest = new TranslateResumeRequest(1L, 2L, "Resume text", "Hindi");

        SummaryResponse summaryResponse = new SummaryResponse("summary", List.of("Java"));
        BulletsResponse bulletsResponse = new BulletsResponse(List.of("bullet"));
        JobMatchResponse jobMatchResponse = new JobMatchResponse(85, List.of("java"), List.of("aws"), List.of("add aws"));
        CoverLetterResponse coverLetterResponse = new CoverLetterResponse("cover-letter");
        ImproveSectionResponse improveSectionResponse = new ImproveSectionResponse("improved");
        SectionSummaryResponse sectionSummaryResponse = new SectionSummaryResponse("section-summary");
        AtsCheckResponse atsCheckResponse = new AtsCheckResponse(80, List.of("java"), List.of("docker"), List.of("add docker"), true);
        SkillSuggestionsResponse skillSuggestionsResponse = new SkillSuggestionsResponse(List.of("Java"));
        TailorResumeResponse tailorResumeResponse = new TailorResumeResponse("{\"summary\":\"Tailored\"}");
        TranslationResponse translationResponse = new TranslationResponse("अनुवाद", "Hindi");

        when(aiAssistantService.generateSummary(7L, true, summaryRequest)).thenReturn(summaryResponse);
        when(aiAssistantService.generateBullets(7L, true, bulletsRequest)).thenReturn(bulletsResponse);
        when(aiAssistantService.analyzeJobMatch(7L, true, jobMatchRequest)).thenReturn(jobMatchResponse);
        when(aiAssistantService.generateCoverLetter(7L, true, coverLetterRequest)).thenReturn(coverLetterResponse);
        when(aiAssistantService.improveSection(7L, true, improveSectionRequest)).thenReturn(improveSectionResponse);
        when(aiAssistantService.generateSectionSummary(7L, true, sectionSummaryRequest)).thenReturn(sectionSummaryResponse);
        when(aiAssistantService.checkAts(7L, true, atsCheckRequest)).thenReturn(atsCheckResponse);
        when(aiAssistantService.suggestSkills(7L, true, skillSuggestionsRequest)).thenReturn(skillSuggestionsResponse);
        when(aiAssistantService.tailorResumeForJob(7L, true, tailorResumeRequest)).thenReturn(tailorResumeResponse);
        when(aiAssistantService.translateResume(7L, true, translateResumeRequest)).thenReturn(translationResponse);

        assertEquals(summaryResponse, controller.generateSummary(7L, "PREMIUM", summaryRequest));
        assertEquals(bulletsResponse, controller.generateBullets(7L, "premium", bulletsRequest));
        assertEquals(jobMatchResponse, controller.analyzeJobMatch(7L, "PREMIUM", jobMatchRequest));
        assertEquals(coverLetterResponse, controller.generateCoverLetter(7L, "PREMIUM", coverLetterRequest));
        assertEquals(improveSectionResponse, controller.improveSection(7L, "PREMIUM", improveSectionRequest));
        assertEquals(sectionSummaryResponse, controller.generateSectionSummary(7L, "PREMIUM", sectionSummaryRequest));
        assertEquals(atsCheckResponse, controller.checkAts(7L, "PREMIUM", atsCheckRequest));
        assertEquals(skillSuggestionsResponse, controller.suggestSkills(7L, "PREMIUM", skillSuggestionsRequest));
        assertEquals(tailorResumeResponse, controller.tailorResumeForJob(7L, "PREMIUM", tailorResumeRequest));
        assertEquals(translationResponse, controller.translateResume(7L, "PREMIUM", translateResumeRequest));
    }

    @Test
    void delegatesHistoryAndQuotaEndpoints() {
        AiAssistantController controller = new AiAssistantController(aiAssistantService);
        List<AiHistoryItemResponse> history = List.of(new AiHistoryItemResponse("SUMMARY", 5L, "gpt-4o", 123, "SUCCESS", Instant.now()));
        QuotaResponse quotaResponse = new QuotaResponse(false, 2, 1);

        when(aiAssistantService.getHistory(11L)).thenReturn(history);
        when(aiAssistantService.getQuota(11L, false)).thenReturn(quotaResponse);

        assertEquals(history, controller.getHistory(11L));
        assertEquals(quotaResponse, controller.getQuota(11L, "FREE"));

        verify(aiAssistantService).getHistory(11L);
        verify(aiAssistantService).getQuota(11L, false);
    }
}
