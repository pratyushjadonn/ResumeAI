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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class SectionClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SectionClient(RestTemplate restTemplate,
                         @Value("${section-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = trimTrailingSlashes(baseUrl);
    }

    public List<SectionData> getSections(Long userId, Long resumeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<SectionResponse[]> responseEntity = restTemplate.exchange(
                    baseUrl + "/api/v1/resumes/{id}/sections",
                    HttpMethod.GET,
                    entity,
                    SectionResponse[].class,
                    resumeId
            );
            SectionResponse[] responses = responseEntity.getBody();
            if (responses == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(responses)
                    .map(r -> new SectionData(r.type(), r.title(), r.content()))
                    .toList();
        } catch (RestClientException ex) {
            log.warn("Unable to fetch sections for userId={} resumeId={} from {}", userId, resumeId, baseUrl, ex);
            return Collections.emptyList();
        }
    }

    private record SectionResponse(
            Long id,
            Long resumeId,
            Long userId,
            String title,
            String type,
            String content,
            int displayOrder,
            boolean visible,
            boolean aiGenerated
    ) {
    }

    private String trimTrailingSlashes(String value) {
        int endIndex = value.length();
        while (endIndex > 0 && value.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return value.substring(0, endIndex);
    }
}
