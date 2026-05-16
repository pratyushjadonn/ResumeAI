package com.example.ai_service.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void buildsSummaryAndBulletPrompts() {
        String summaryPrompt = promptBuilder.buildSummaryPrompt("Java Developer", "5 years", "Java, Spring");
        String bulletPrompt = promptBuilder.buildBulletPrompt("Backend Engineer", "Built APIs");

        assertTrue(summaryPrompt.contains("Target role: Java Developer"));
        assertTrue(summaryPrompt.contains("Experience: 5 years"));
        assertTrue(summaryPrompt.contains("Skills: Java, Spring"));
        assertTrue(bulletPrompt.contains("Role: Backend Engineer"));
        assertTrue(bulletPrompt.contains("Context: Built APIs"));
    }

    @Test
    void buildsAnalysisAndTailoringPrompts() {
        String atsPrompt = promptBuilder.buildAtsPrompt("resume text", "job description");
        String tailorPrompt = promptBuilder.buildTailorPrompt("{\"summary\":\"Hi\"}", "Need Spring Boot");

        assertTrue(atsPrompt.contains("Resume:\nresume text"));
        assertTrue(atsPrompt.contains("Job Description:\njob description"));
        assertTrue(tailorPrompt.contains("Resume JSON:\n{\"summary\":\"Hi\"}"));
        assertTrue(tailorPrompt.contains("Job Description:\nNeed Spring Boot"));
    }

    @Test
    void buildsCoverLetterAndImproveSectionPromptsWithDefaults() {
        String coverLetterPrompt = promptBuilder.buildCoverLetterPrompt(
                "Aman",
                "Platform Engineer",
                "ResumeAI",
                "Own platform delivery",
                null
        );
        String improvePrompt = promptBuilder.buildImproveSectionPrompt("EXPERIENCE", "Built APIs", null);

        assertTrue(coverLetterPrompt.contains("Applicant: Aman"));
        assertTrue(coverLetterPrompt.contains("Target role: Platform Engineer"));
        assertTrue(coverLetterPrompt.contains("Highlights: "));
        assertTrue(improvePrompt.contains("Section type: EXPERIENCE"));
        assertTrue(improvePrompt.contains("Tone: professional"));
        assertTrue(improvePrompt.contains("Built APIs"));
    }

    @Test
    void buildsSectionSummarySkillAndTranslatePrompts() {
        String sectionSummaryPrompt = promptBuilder.buildSectionSummaryPrompt(
                "PROJECT",
                null,
                "Delivered AI features",
                null
        );
        String skillsPrompt = promptBuilder.buildSuggestSkillsPrompt("Data Engineer");
        String translatePrompt = promptBuilder.buildTranslatePrompt("Resume text", "Hindi");

        assertTrue(sectionSummaryPrompt.contains("Section type: PROJECT"));
        assertTrue(sectionSummaryPrompt.contains("Section title: "));
        assertTrue(sectionSummaryPrompt.contains("Target role: "));
        assertTrue(sectionSummaryPrompt.contains("Delivered AI features"));
        assertTrue(skillsPrompt.contains("this role: Data Engineer"));
        assertTrue(translatePrompt.contains("Translate the following resume text to Hindi."));
        assertTrue(translatePrompt.contains("Resume text:\nResume text"));
    }
}
