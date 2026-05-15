package com.example.ai_service.service.impl;

import com.example.ai_service.client.ClaudeClient;
import com.example.ai_service.client.OpenAiClient;
import com.example.ai_service.dto.request.AtsCheckRequest;
import com.example.ai_service.dto.request.GenerateSummaryRequest;
import com.example.ai_service.dto.response.AtsCheckResponse;
import com.example.ai_service.dto.response.SummaryResponse;
import com.example.ai_service.entity.AiRequest;
import com.example.ai_service.exception.AiQuotaException;
import com.example.ai_service.prompt.PromptBuilder;
import com.example.ai_service.repository.AiRequestRepository;
import com.example.ai_service.service.AiQuotaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceImplTest {

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private ClaudeClient claudeClient;

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private AiQuotaService aiQuotaService;

    @InjectMocks
    private AiAssistantServiceImpl aiAssistantService;

    @Test
    void generateSummary_success() {
        GenerateSummaryRequest request = new GenerateSummaryRequest(
                "Java Developer",
                4,
                List.of("Spring Boot", "Java", "SQL"),
                "Microservices"
        );
        when(promptBuilder.buildSummaryPrompt("Java Developer", "4 years", "Spring Boot, Java, SQL"))
                .thenReturn("summary-prompt");
        when(openAiClient.generateText("summary-prompt")).thenReturn("Strong Java developer summary");
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SummaryResponse response = aiAssistantService.generateSummary(1L, false, request);

        assertEquals("Strong Java developer summary", response.summary());
        assertTrue(response.suggestedKeywords().contains("Spring Boot"));
        verify(aiQuotaService).checkAiGenerationQuota(1L, "FREE");
        verify(aiQuotaService).incrementAiGeneration(1L);
        verify(aiRequestRepository).save(any(AiRequest.class));
        verify(claudeClient, never()).generateText(any());
    }

    @Test
    void checkAtsCompatibility_success() {
        AtsCheckRequest request = new AtsCheckRequest(
                1L,
                20L,
                "Java Spring SQL communication",
                "Java Spring SQL Docker"
        );
        when(promptBuilder.buildAtsPrompt(request.resumeText(), request.jobDescription()))
                .thenReturn("ats-prompt");
        when(openAiClient.generateText("ats-prompt"))
                .thenReturn("- Add Docker experience\n- Quantify project impact\n- Improve summary alignment");
        when(aiRequestRepository.save(any(AiRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtsCheckResponse response = aiAssistantService.checkAts(1L, false, request);

        assertTrue(response.analyzed());
        assertTrue(response.score() > 0);
        assertFalse(response.matchedKeywords().isEmpty());
        assertEquals(3, response.recommendations().size());
        verify(aiQuotaService).checkAtsQuota(1L, "FREE");
        verify(aiQuotaService).incrementAtsUsage(1L);
    }

    @Test
    void aiQuotaExceeded() {
        GenerateSummaryRequest request = new GenerateSummaryRequest(
                "Java Developer",
                4,
                List.of("Spring Boot", "Java"),
                null
        );
        doThrow(new AiQuotaException("Monthly AI generation quota exceeded for FREE plan"))
                .when(aiQuotaService).checkAiGenerationQuota(1L, "FREE");

        assertThrows(AiQuotaException.class, () -> aiAssistantService.generateSummary(1L, false, request));

        verify(openAiClient, never()).generateText(any());
        verify(aiRequestRepository, never()).save(any());
    }
}
