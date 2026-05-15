package com.example.export_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ResumeClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ResumeClient(RestTemplate restTemplate,
                        @Value("${resume-service.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public ResumeData getResume(Long userId, Long resumeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ResumeResponse> responseEntity = restTemplate.exchange(
                    baseUrl + "/api/v1/resumes/{id}",
                    HttpMethod.GET,
                    entity,
                    ResumeResponse.class,
                    resumeId
            );
            ResumeResponse response = responseEntity.getBody();
            if (response == null) {
                return null;
            }
            return new ResumeData(
                    response.title(), // use title as name
                    response.title(),
                    response.summary(),
                    response.templateKey()
            );
        } catch (RestClientException ex) {
            log.warn("Unable to fetch resume data for userId={} resumeId={} from {}", userId, resumeId, baseUrl, ex);
            return null;
        }
    }

    private record ResumeResponse(
            Long id,
            Long userId,
            String title,
            String targetRole,
            String templateKey,
            Long templateId,
            String summary,
            Integer atsScore,
            String language,
            boolean isPublic,
            long viewCount,
            String status,
            int version
    ) {
    }
}
