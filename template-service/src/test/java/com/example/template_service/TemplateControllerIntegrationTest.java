package com.example.template_service;

import com.example.template_service.dto.request.CreateTemplateRequest;
import com.example.template_service.dto.request.UpdateTemplateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
class TemplateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void templateFlowShouldCreateUpdateListToggleAndDelete() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTemplateRequest(
                                        "modern-blue",
                                        "Modern Blue",
                                        "MODERN",
                                        "Modern template for software professionals",
                                        "https://cdn.example.com/templates/modern-blue.png",
                                        "<div>Modern layout</div>",
                                        ".template { color: #2563EB; }",
                                        "#2563EB",
                                        "single-column",
                                        false,
                                        true,
                                        true,
                                        List.of("clean", "ats-friendly")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateKey").value("modern-blue"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        long templateId = created.get("id").asLong();

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateKey").value("modern-blue"));

        mockMvc.perform(get("/api/v1/templates/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featured").value(true));

        mockMvc.perform(put("/api/v1/templates/{id}", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTemplateRequest(
                                        "Modern Blue Pro",
                                        "PROFESSIONAL",
                                        "Updated premium-ready professional layout",
                                        "https://cdn.example.com/templates/modern-blue-pro.png",
                                        "<div>Premium layout</div>",
                                        ".template-pro { color: #1D4ED8; }",
                                        "#1D4ED8",
                                        "two-column",
                                        true,
                                        true,
                                        true,
                                        List.of("professional", "executive")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Modern Blue Pro"))
                .andExpect(jsonPath("$.category").value("PROFESSIONAL"));

        mockMvc.perform(patch("/api/v1/templates/{id}/deactivate", templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/v1/templates/{id}/activate", templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/v1/templates/{id}", templateId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/templates/{id}", templateId))
                .andExpect(status().isNotFound());
    }
}
