package com.example.section_service;

import com.example.section_service.dto.request.CreateSectionRequest;
import com.example.section_service.dto.request.UpdateSectionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sectionFlowShouldCreateUpdateReorderListAndDelete() throws Exception {
        String firstSectionResponse = mockMvc.perform(post("/api/v1/resumes/{resumeId}/sections", 55)
                        .header("X-User-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateSectionRequest(
                                        "Professional Summary",
                                        "SUMMARY",
                                        "Experienced Java engineer.",
                                        true,
                                        false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long firstSectionId = objectMapper.readTree(firstSectionResponse).get("id").asLong();

        String secondSectionResponse = mockMvc.perform(post("/api/v1/resumes/{resumeId}/sections", 55)
                        .header("X-User-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateSectionRequest(
                                        "Work Experience",
                                        "EXPERIENCE",
                                        "Built distributed backend services.",
                                        true,
                                        false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode secondSection = objectMapper.readTree(secondSectionResponse);
        long secondSectionId = secondSection.get("id").asLong();

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/sections/{sectionId}", 55, firstSectionId)
                        .header("X-User-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Professional Summary"));

        mockMvc.perform(put("/api/v1/resumes/{resumeId}/sections/{sectionId}", 55, firstSectionId)
                        .header("X-User-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateSectionRequest(
                                        "Executive Summary",
                                        "SUMMARY",
                                        "Updated leadership-focused summary.",
                                        true,
                                        false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Executive Summary"));

        mockMvc.perform(patch("/api/v1/resumes/{resumeId}/sections/{sectionId}/reorder", 55, secondSectionId)
                        .header("X-User-Id", 101)
                        .param("position", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(1));

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/sections", 55)
                        .header("X-User-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Work Experience"))
                .andExpect(jsonPath("$[1].title").value("Executive Summary"));

        mockMvc.perform(delete("/api/v1/resumes/{resumeId}/sections/{sectionId}", 55, secondSectionId)
                        .header("X-User-Id", 101))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/sections/{sectionId}", 55, secondSectionId)
                        .header("X-User-Id", 101))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/resumes/{resumeId}/sections", 55)
                        .header("X-User-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayOrder").value(1));
    }
}
