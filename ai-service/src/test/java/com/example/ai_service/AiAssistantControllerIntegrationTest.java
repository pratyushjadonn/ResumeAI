package com.example.ai_service;

import com.example.ai_service.dto.request.GenerateBulletsRequest;
import com.example.ai_service.dto.request.GenerateSectionSummaryRequest;
import com.example.ai_service.dto.request.GenerateSummaryRequest;
import com.example.ai_service.dto.request.JobMatchRequest;
import com.example.ai_service.service.AiQuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiAssistantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiQuotaService aiQuotaService;

    @Test
    void aiEndpointsShouldGenerateSummaryBulletsAndMatchAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/ai/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GenerateSummaryRequest(
                                        "Java Backend Developer",
                                        4,
                                        List.of("Java", "Spring Boot", "MySQL", "REST APIs"),
                                        "scalable backend systems"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("Java Backend Developer")))
                .andExpect(jsonPath("$.suggestedKeywords[0]").value("Java"));

        mockMvc.perform(post("/api/v1/ai/bullets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GenerateBulletsRequest(
                                        "Backend Engineer",
                                        "ResumeAI Platform",
                                        List.of("API design", "service integration", "performance optimization"),
                                        List.of("Java", "Spring Boot", "MySQL"),
                                        "30% faster response times"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bullets.length()").value(3));

        mockMvc.perform(post("/api/v1/ai/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JobMatchRequest(
                                        "Java Spring Boot MySQL REST APIs microservices performance optimization",
                                        "Looking for Java Spring Boot developer with MySQL, microservices, Docker and AWS experience"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchScore").isNumber())
                .andExpect(jsonPath("$.matchedKeywords").isArray())
                .andExpect(jsonPath("$.missingKeywords").isArray())
                .andExpect(jsonPath("$.recommendations.length()").value(3));

        mockMvc.perform(post("/api/v1/ai/section-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GenerateSectionSummaryRequest(
                                        1L,
                                        101L,
                                        "EXPERIENCE",
                                        "Backend Engineer",
                                        "Built REST APIs for resume generation, improved latency by 35%, and handled production incidents.",
                                        "Senior Java Developer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").isString());
    }
}
