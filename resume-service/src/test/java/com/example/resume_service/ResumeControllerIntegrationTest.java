package com.example.resume_service;

import com.example.resume_service.dto.request.CreateResumeRequest;
import com.example.resume_service.dto.request.UpdateResumeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class ResumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void resumeFlowShouldCreateUpdatePublishDuplicateListAndDelete() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/resumes")
                        .header("X-User-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateResumeRequest(
                                        "Java Backend Resume",
                                        "Java Developer",
                                        "modern-blue",
                                        1L,
                                        "Experienced backend engineer",
                                        "English"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long resumeId = created.get("id").asLong();

        mockMvc.perform(get("/api/v1/resumes/{id}", resumeId)
                        .header("X-User-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Backend Resume"));

        mockMvc.perform(put("/api/v1/resumes/{id}", resumeId)
                        .header("X-User-Id", 101)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateResumeRequest(
                                        "Senior Java Backend Resume",
                                        "Senior Java Developer",
                                        "modern-blue",
                                        1L,
                                        "Updated professional summary",
                                        "English"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Senior Java Backend Resume"));

        mockMvc.perform(patch("/api/v1/resumes/{id}/publish", resumeId)
                        .header("X-User-Id", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/resumes/{id}/duplicate", resumeId)
                        .header("X-User-Id", 101))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("Senior Java Backend Resume Copy"));

        mockMvc.perform(get("/api/v1/resumes")
                        .header("X-User-Id", 101)
                        .param("search", "Senior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(delete("/api/v1/resumes/{id}", resumeId)
                        .header("X-User-Id", 101))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/{id}", resumeId)
                        .header("X-User-Id", 101))
                .andExpect(status().isNotFound());
    }
}
