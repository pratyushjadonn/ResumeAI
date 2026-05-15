package com.example.ai_service.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildSummaryPrompt(String jobTitle, String experience, String skills) {
        return """
                Create a professional resume summary.
                Target role: %s
                Experience: %s
                Skills: %s

                Constraints:
                - 2 to 3 concise sentences.
                - Use strong action-oriented language.
                - Keep it ATS-friendly and factual.
                - Do not use markdown.
                """.formatted(jobTitle, experience, skills);
    }

    public String buildBulletPrompt(String role, String description) {
        return """
                Generate 4 achievement-focused resume bullet points.
                Role: %s
                Context: %s

                Constraints:
                - Start each bullet with a strong action verb.
                - Include impact/results where possible.
                - Keep each bullet under 24 words.
                - Return plain text bullets only.
                """.formatted(role, description);
    }

    public String buildAtsPrompt(String resumeText, String jobDescription) {
        return """
                Analyze ATS compatibility between the resume and job description.
                Resume:
                %s

                Job Description:
                %s

                Output:
                - 3 practical recommendations to improve match quality.
                - Keep recommendations concise and actionable.
                - Plain text only.
                """.formatted(resumeText, jobDescription);
    }

    public String buildTailorPrompt(String resumeJson, String jobDescription) {
        return """
                Tailor this resume JSON for the provided job description without changing its JSON structure.

                Resume JSON:
                %s

                Job Description:
                %s

                Constraints:
                - Preserve valid JSON format.
                - Improve wording for role relevance.
                - Keep statements truthful and concise.
                """.formatted(resumeJson, jobDescription);
    }

    public String buildCoverLetterPrompt(String applicantName,
                                         String targetRole,
                                         String companyName,
                                         String jobDescription,
                                         String highlights) {
        return """
                Write a professional cover letter.
                Applicant: %s
                Target role: %s
                Company: %s
                Job description: %s
                Highlights: %s

                Constraints:
                - 3 to 5 short paragraphs.
                - Tone: confident and professional.
                - Do not use placeholders.
                """.formatted(applicantName, targetRole, companyName, jobDescription, highlights == null ? "" : highlights);
    }

    public String buildImproveSectionPrompt(String sectionType, String content, String tone) {
        return """
                Improve this resume section.
                Section type: %s
                Tone: %s
                Current content:
                %s

                Constraints:
                - Keep core facts unchanged.
                - Improve clarity, impact, and ATS readability.
                - Return plain text only.
                """.formatted(sectionType, tone == null ? "professional" : tone, content);
    }

    public String buildSectionSummaryPrompt(String sectionType, String sectionTitle, String sectionContent, String targetRole) {
        return """
                Write a concise, ATS-friendly resume section summary.
                Section type: %s
                Section title: %s
                Target role: %s
                Section content:
                %s

                Constraints:
                - Keep factual meaning unchanged.
                - Return 2 to 4 bullet points.
                - Each bullet should be under 22 words.
                - Start each bullet with an action-oriented phrase.
                - Return plain text only.
                """.formatted(
                sectionType,
                sectionTitle == null ? "" : sectionTitle,
                targetRole == null ? "" : targetRole,
                sectionContent
        );
    }

    public String buildSuggestSkillsPrompt(String targetRole) {
        return """
                Suggest 8 to 12 highly relevant resume skills for this role: %s
                Constraints:
                - Include technical and practical role skills.
                - Return as comma-separated plain text.
                """.formatted(targetRole);
    }

    public String buildTranslatePrompt(String resumeText, String targetLanguage) {
        return """
                Translate the following resume text to %s.
                Preserve professional tone and formatting intent.
                Return plain text only.

                Resume text:
                %s
                """.formatted(targetLanguage, resumeText);
    }
}
