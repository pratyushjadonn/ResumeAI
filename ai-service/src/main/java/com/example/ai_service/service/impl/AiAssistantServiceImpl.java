package com.example.ai_service.service.impl;

import com.example.ai_service.client.ClaudeClient;
import com.example.ai_service.client.OpenAiClient;
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
import com.example.ai_service.entity.AiRequest;
import com.example.ai_service.prompt.PromptBuilder;
import com.example.ai_service.repository.AiRequestRepository;
import com.example.ai_service.service.AiAssistantService;
import com.example.ai_service.service.AiQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[^a-zA-Z0-9+#.]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "and", "the", "with", "for", "you", "your", "are", "from", "that", "this",
            "have", "has", "into", "using", "used", "will", "our", "their", "about",
            "who", "all", "any", "but", "can", "not", "job", "role", "team"
    );
    private static final Map<Long, List<AiHistoryItemResponse>> HISTORY = new ConcurrentHashMap<>();

    private final PromptBuilder promptBuilder;
    private final OpenAiClient openAiClient;
    private final ClaudeClient claudeClient;
    private final AiRequestRepository aiRequestRepository;
    private final AiQuotaService aiQuotaService;

    @Override
    public SummaryResponse generateSummary(Long userId, boolean premium, GenerateSummaryRequest request) {
        Long normalizedUserId = defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        List<String> skills = normalizeList(request.skills());
        String focusArea = normalizeOptional(request.focusArea());
        String fallbackSummary = "Results-driven " + request.targetRole().trim()
                + " with " + request.yearsOfExperience() + "+"
                + " years of experience delivering impact through "
                + String.join(", ", topItems(skills, 4))
                + (focusArea == null ? "." : " with a strong focus on " + focusArea + ".");
        String prompt = promptBuilder.buildSummaryPrompt(
                request.targetRole().trim(),
                request.yearsOfExperience() + " years",
                String.join(", ", skills)
        );
        AiCallResult aiResult = generateTextWithFallback(prompt, fallbackSummary);

        List<String> keywords = new ArrayList<>(topItems(skills, 6));
        if (focusArea != null) {
            keywords.add(focusArea);
        }
        saveAiRequest(normalizedUserId, null, "SUMMARY", prompt, aiResult.content(), aiResult.modelUsed(), aiResult.tokensUsed(), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, null, "GENERATE_SUMMARY", aiResult.modelUsed(), aiResult.tokensUsed());
        return new SummaryResponse(aiResult.content(), keywords);
    }

    @Override
    public BulletsResponse generateBullets(Long userId, boolean premium, GenerateBulletsRequest request) {
        Long normalizedUserId = defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        List<String> responsibilities = normalizeList(request.responsibilities());
        List<String> technologies = normalizeList(request.technologies());
        String techPhrase = technologies.isEmpty() ? "" : " using " + String.join(", ", topItems(technologies, 3));
        String impact = normalizeOptional(request.measurableImpact());

        List<String> fallbackBullets = responsibilities.stream()
                .limit(4)
                .map(item -> buildBullet(request.role().trim(), request.projectName().trim(), item, techPhrase, impact))
                .toList();
        String bulletContext = "Project: " + request.projectName().trim()
                + ". Responsibilities: " + String.join("; ", responsibilities)
                + (technologies.isEmpty() ? "" : ". Technologies: " + String.join(", ", technologies))
                + (impact == null ? "" : ". Measurable impact: " + impact);
        String prompt = promptBuilder.buildBulletPrompt(request.role().trim(), bulletContext);
        AiCallResult aiResult = generateTextWithFallback(prompt, String.join("\n", fallbackBullets));
        List<String> bullets = extractListFromText(aiResult.content(), 4, fallbackBullets);

        saveAiRequest(normalizedUserId, null, "BULLETS", prompt, String.join("\n", bullets), aiResult.modelUsed(), estimateTokens(String.join("\n", bullets)), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, null, "GENERATE_BULLETS", aiResult.modelUsed(), estimateTokens(String.join(" ", bullets)));
        return new BulletsResponse(bullets);
    }

    @Override
    public JobMatchResponse analyzeJobMatch(Long userId, boolean premium, JobMatchRequest request) {
        Long normalizedUserId = defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        MatchComputation match = computeMatch(request.resumeText(), request.jobDescription());
        List<String> fallbackRecommendations = defaultRecommendations(match.missingKeywords());
        String prompt = promptBuilder.buildAtsPrompt(request.resumeText(), request.jobDescription());
        AiCallResult aiResult = generateTextWithFallback(prompt, String.join("\n", fallbackRecommendations));
        List<String> recommendations = extractListFromText(aiResult.content(), 3, fallbackRecommendations);

        saveAiRequest(normalizedUserId, null, "MATCH_ANALYSIS", prompt, String.join("\n", recommendations), aiResult.modelUsed(), estimateTokens(String.join("\n", recommendations)), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, null, "ANALYZE_MATCH", aiResult.modelUsed(), estimateTokens(String.join(" ", recommendations)));
        return new JobMatchResponse(match.score(), match.matchedKeywords(), match.missingKeywords(), recommendations);
    }

    @Override
    public CoverLetterResponse generateCoverLetter(Long userId, boolean premium, CoverLetterRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        String fallbackContent = "Dear Hiring Team,\n\n"
                + "I am excited to apply for the " + request.targetRole().trim() + " role at " + request.companyName().trim() + ". "
                + request.applicantName().trim() + " brings relevant experience aligned with your needs"
                + optionalSentence(request.highlights())
                + "\n\nMy background matches the priorities highlighted in your job description, and I would welcome the opportunity to contribute with impact from day one."
                + "\n\nSincerely,\n" + request.applicantName().trim();
        String prompt = promptBuilder.buildCoverLetterPrompt(
                request.applicantName().trim(),
                request.targetRole().trim(),
                request.companyName().trim(),
                request.jobDescription().trim(),
                request.highlights()
        );
        AiCallResult aiResult = generateTextWithFallback(prompt, fallbackContent);
        saveAiRequest(normalizedUserId, request.resumeId(), "COVER_LETTER", prompt, aiResult.content(), aiResult.modelUsed(), aiResult.tokensUsed(), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "GENERATE_COVER_LETTER", aiResult.modelUsed(), aiResult.tokensUsed());
        return new CoverLetterResponse(aiResult.content());
    }

    @Override
    public ImproveSectionResponse improveSection(Long userId, boolean premium, ImproveSectionRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        String tone = normalizeOptional(request.tone());
        String fallback = "Improved " + request.sectionType().trim().toLowerCase(Locale.ROOT)
                + " section: " + request.content().trim()
                + (tone == null ? "" : " Refined for a " + tone + " tone.")
                + " Added clearer impact, stronger action verbs, and tighter phrasing.";
        String prompt = promptBuilder.buildImproveSectionPrompt(request.sectionType().trim(), request.content().trim(), tone);
        AiCallResult aiResult = generateTextWithFallback(prompt, fallback);
        saveAiRequest(normalizedUserId, request.resumeId(), "IMPROVE_SECTION", prompt, aiResult.content(), aiResult.modelUsed(), aiResult.tokensUsed(), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "IMPROVE_SECTION", aiResult.modelUsed(), aiResult.tokensUsed());
        return new ImproveSectionResponse(aiResult.content());
    }

    @Override
    public SectionSummaryResponse generateSectionSummary(Long userId, boolean premium, GenerateSectionSummaryRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        String cleanedContent = request.sectionContent().trim();
        String fallbackSummary = buildFallbackSectionSummary(request.sectionType().trim(), cleanedContent);
        String prompt = promptBuilder.buildSectionSummaryPrompt(
                request.sectionType().trim(),
                normalizeOptional(request.sectionTitle()),
                cleanedContent,
                normalizeOptional(request.targetRole())
        );
        AiCallResult aiResult = generateTextWithFallback(prompt, fallbackSummary);
        String normalizedSummary = normalizeSectionSummary(aiResult.content(), fallbackSummary);

        saveAiRequest(normalizedUserId, request.resumeId(), "SECTION_SUMMARY", prompt, normalizedSummary, aiResult.modelUsed(), estimateTokens(normalizedSummary), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "GENERATE_SECTION_SUMMARY", aiResult.modelUsed(), estimateTokens(normalizedSummary));
        return new SectionSummaryResponse(normalizedSummary);
    }

    @Override
    public AtsCheckResponse checkAts(Long userId, boolean premium, AtsCheckRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAtsQuota(normalizedUserId, toSubscriptionPlan(premium));
        MatchComputation match = computeMatch(request.resumeText(), request.jobDescription());
        List<String> fallbackRecommendations = defaultRecommendations(match.missingKeywords());
        String prompt = promptBuilder.buildAtsPrompt(request.resumeText(), request.jobDescription());
        AiCallResult aiResult = generateTextWithFallback(prompt, String.join("\n", fallbackRecommendations));
        List<String> recommendations = extractListFromText(aiResult.content(), 3, fallbackRecommendations);

        saveAiRequest(normalizedUserId, request.resumeId(), "ATS", prompt, String.join("\n", recommendations), aiResult.modelUsed(), estimateTokens(String.join("\n", recommendations)), "COMPLETED");
        aiQuotaService.incrementAtsUsage(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "CHECK_ATS", aiResult.modelUsed(), estimateTokens(String.join(" ", recommendations)));
        return new AtsCheckResponse(match.score(), match.matchedKeywords(), match.missingKeywords(), recommendations, true);
    }

    @Override
    public SkillSuggestionsResponse suggestSkills(Long userId, boolean premium, SkillSuggestionsRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        List<String> fallbackSkills = inferSkillsFromRole(request.targetRole());
        String prompt = promptBuilder.buildSuggestSkillsPrompt(request.targetRole().trim());
        AiCallResult aiResult = generateTextWithFallback(prompt, String.join(", ", fallbackSkills));
        List<String> skills = extractSkills(aiResult.content(), fallbackSkills);

        saveAiRequest(normalizedUserId, null, "SUGGEST_SKILLS", prompt, String.join(", ", skills), aiResult.modelUsed(), estimateTokens(String.join(", ", skills)), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, null, "SUGGEST_SKILLS", aiResult.modelUsed(), estimateTokens(String.join(", ", skills)));
        return new SkillSuggestionsResponse(skills);
    }

    @Override
    public TailorResumeResponse tailorResumeForJob(Long userId, boolean premium, TailorResumeRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        List<String> keywords = new ArrayList<>(extractKeywords(request.jobDescription()));
        String fallback = request.resumeJson().trim() + "\n\n/* Tailored keywords: "
                + String.join(", ", keywords.stream().limit(10).toList()) + " */";
        String prompt = promptBuilder.buildTailorPrompt(request.resumeJson().trim(), request.jobDescription().trim());
        AiCallResult aiResult = generateTextWithFallback(prompt, fallback);

        saveAiRequest(normalizedUserId, request.resumeId(), "TAILOR_RESUME", prompt, aiResult.content(), aiResult.modelUsed(), aiResult.tokensUsed(), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "TAILOR_RESUME", aiResult.modelUsed(), aiResult.tokensUsed());
        return new TailorResumeResponse(aiResult.content());
    }

    @Override
    public TranslationResponse translateResume(Long userId, boolean premium, TranslateResumeRequest request) {
        Long normalizedUserId = request.userId() != null && request.userId() > 0 ? request.userId() : defaultUser(userId);
        aiQuotaService.checkAiGenerationQuota(normalizedUserId, toSubscriptionPlan(premium));
        String fallback = "[" + request.targetLanguage().trim() + "] " + request.resumeText().trim();
        String prompt = promptBuilder.buildTranslatePrompt(request.resumeText().trim(), request.targetLanguage().trim());
        AiCallResult aiResult = generateTextWithFallback(prompt, fallback);

        saveAiRequest(normalizedUserId, request.resumeId(), "TRANSLATE_RESUME", prompt, aiResult.content(), aiResult.modelUsed(), aiResult.tokensUsed(), "COMPLETED");
        aiQuotaService.incrementAiGeneration(normalizedUserId);
        logHistory(normalizedUserId, request.resumeId(), "TRANSLATE_RESUME", aiResult.modelUsed(), aiResult.tokensUsed());
        return new TranslationResponse(aiResult.content(), request.targetLanguage().trim());
    }

    @Override
    public List<AiHistoryItemResponse> getHistory(Long userId) {
        Long normalizedUserId = defaultUser(userId);
        try {
            List<AiRequest> records = aiRequestRepository.findTop100ByUserIdOrderByCreatedAtDesc(normalizedUserId);
            if (!records.isEmpty()) {
                return records.stream()
                        .map(record -> new AiHistoryItemResponse(
                                record.getRequestType(),
                                record.getResumeId(),
                                record.getModelUsed(),
                                record.getTokensUsed(),
                                record.getStatus(),
                                record.getCreatedAt()
                        ))
                        .toList();
            }
        } catch (DataAccessException ex) {
            log.warn("Failed to fetch AI history from database, using in-memory history", ex);
        }
        return HISTORY.getOrDefault(normalizedUserId, List.of()).stream()
                .sorted(Comparator.comparing(AiHistoryItemResponse::createdAt).reversed())
                .toList();
    }

    @Override
    public QuotaResponse getQuota(Long userId, boolean premium) {
        Long normalizedUserId = defaultUser(userId);
        if (premium) {
            return new QuotaResponse(true, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        return new QuotaResponse(
                false,
                aiQuotaService.getRemainingAiCalls(normalizedUserId),
                aiQuotaService.getRemainingAtsChecks(normalizedUserId)
        );
    }

    private String buildBullet(String role, String projectName, String responsibility, String techPhrase, String impact) {
        StringBuilder bullet = new StringBuilder("Led ")
                .append(responsibility.trim().toLowerCase(Locale.ROOT))
                .append(" for ")
                .append(projectName)
                .append(" as ")
                .append(role);
        if (!techPhrase.isBlank()) {
            bullet.append(techPhrase);
        }
        if (impact != null) {
            bullet.append(", delivering ").append(impact);
        }
        bullet.append(".");
        return bullet.toString();
    }

    private String buildFallbackSectionSummary(String sectionType, String content) {
        List<String> lines = Arrays.stream(content.split("\\r?\\n"))
                .map(String::trim)
                .map(line -> line.replaceFirst("^[\\-\\*\\d\\.)\\s]+", "").trim())
                .filter(line -> !line.isBlank())
                .limit(4)
                .toList();

        if (lines.isEmpty()) {
            return "- Summarized " + sectionType.toLowerCase(Locale.ROOT) + " details in concise ATS-friendly points.";
        }
        return lines.stream()
                .map(line -> "- " + trimWords(line, 18))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("- Summarized key points.");
    }

    private String normalizeSectionSummary(String aiText, String fallback) {
        List<String> bullets = extractListFromText(aiText, 4, extractListFromText(fallback, 4, List.of("- Summarized key points.")));
        return bullets.stream()
                .map(bullet -> bullet.startsWith("- ") ? bullet : "- " + bullet)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(fallback);
    }

    private String trimWords(String text, int maxWords) {
        String[] words = text.split("\\s+");
        if (words.length <= maxWords) {
            return text;
        }
        return String.join(" ", Arrays.copyOf(words, maxWords));
    }

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : SPLIT_PATTERN.split(text.toLowerCase(Locale.ROOT))) {
            if (token.length() < 3 || STOP_WORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeOptional)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    private List<String> topItems(List<String> values, int limit) {
        return values.stream().limit(limit).toList();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> inferSkillsFromRole(String targetRole) {
        String role = targetRole.trim().toLowerCase(Locale.ROOT);
        Map<String, List<String>> suggestions = new HashMap<>();
        suggestions.put("java", List.of("Spring Boot", "REST APIs", "Microservices", "JUnit", "SQL", "Docker"));
        suggestions.put("frontend", List.of("React", "TypeScript", "Responsive Design", "State Management", "Testing"));
        suggestions.put("data", List.of("SQL", "Python", "ETL", "Analytics", "Data Modeling", "Visualization"));
        return suggestions.entrySet().stream()
                .filter(entry -> role.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of("Communication", "Problem Solving", "Stakeholder Management", "Documentation", "Delivery Ownership"));
    }

    private void logHistory(Long userId,
                            Long resumeId,
                            String requestType,
                            String modelUsed,
                            int tokensUsed) {
        HISTORY.computeIfAbsent(userId, ignored -> new ArrayList<>())
                .add(new AiHistoryItemResponse(
                        requestType,
                        resumeId,
                        modelUsed,
                        tokensUsed,
                        "COMPLETED",
                        Instant.now()
                ));
    }

    private int estimateTokens(String value) {
        return Math.max(1, value.length() / 4);
    }

    private Long defaultUser(Long userId) {
        return userId == null ? 0L : userId;
    }

    private String optionalSentence(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "." : ", especially in " + normalized + ".";
    }

    private MatchComputation computeMatch(String resumeText, String jobDescription) {
        Set<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> jobKeywords = extractKeywords(jobDescription);

        List<String> matched = jobKeywords.stream()
                .filter(resumeKeywords::contains)
                .limit(12)
                .toList();

        List<String> missing = jobKeywords.stream()
                .filter(keyword -> !resumeKeywords.contains(keyword))
                .limit(12)
                .toList();

        int score = jobKeywords.isEmpty()
                ? 0
                : Math.min(100, (int) Math.round((matched.size() * 100.0) / jobKeywords.size()));
        return new MatchComputation(score, matched, missing);
    }

    private List<String> defaultRecommendations(List<String> missing) {
        List<String> recommendations = new ArrayList<>();
        if (!missing.isEmpty()) {
            recommendations.add("Add evidence for: " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
        }
        recommendations.add("Align your summary with the target role and strongest matched keywords.");
        recommendations.add("Quantify impact with metrics for the most relevant projects and experience bullets.");
        return recommendations;
    }

    private List<String> extractListFromText(String text, int limit, List<String> fallback) {
        List<String> extracted = Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .map(line -> line.replaceFirst("^[\\-\\*\\d\\.)\\s]+", "").trim())
                .filter(line -> !line.isBlank())
                .distinct()
                .limit(limit)
                .toList();
        if (!extracted.isEmpty()) {
            return extracted;
        }
        return fallback.stream().limit(limit).toList();
    }

    private List<String> extractSkills(String text, List<String> fallback) {
        List<String> skills = Arrays.stream(text.split("[,\\r\\n]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(12)
                .toList();
        return skills.isEmpty() ? fallback : skills;
    }

    private AiCallResult generateTextWithFallback(String prompt, String fallbackContent) {
        try {
            String openAiResponse = openAiClient.generateText(prompt);
            return new AiCallResult(openAiResponse, "gpt-4o", estimateTokens(openAiResponse));
        } catch (RuntimeException ex) {
            log.warn("OpenAI request failed, switching to Claude fallback", ex);
        }
        try {
            String claudeResponse = claudeClient.generateText(prompt);
            return new AiCallResult(claudeResponse, "claude", estimateTokens(claudeResponse));
        } catch (RuntimeException ex) {
            log.warn("Claude request failed, switching to in-memory fallback", ex);
        }
        return new AiCallResult(fallbackContent, "mock-fallback", estimateTokens(fallbackContent));
    }

    private void saveAiRequest(Long userId,
                               Long resumeId,
                               String requestType,
                               String inputPrompt,
                               String aiResponse,
                               String modelUsed,
                               int tokensUsed,
                               String status) {
        try {
            aiRequestRepository.save(AiRequest.builder()
                    .userId(defaultUser(userId))
                    .resumeId(resumeId)
                    .requestType(requestType)
                    .inputPrompt(trimToMax(inputPrompt, 20000))
                    .aiResponse(trimToMax(aiResponse, 20000))
                    .modelUsed(modelUsed)
                    .tokensUsed(tokensUsed)
                    .status(status)
                    .build());
        } catch (DataAccessException ex) {
            log.warn("Failed to persist AI request, continuing with response", ex);
        }
    }

    private String toSubscriptionPlan(boolean premium) {
        return premium ? "PREMIUM" : "FREE";
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record AiCallResult(String content, String modelUsed, int tokensUsed) {
    }

    private record MatchComputation(int score, List<String> matchedKeywords, List<String> missingKeywords) {
    }
}
