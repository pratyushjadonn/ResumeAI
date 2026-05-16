package com.example.export_service.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResumeClientTest {

    @Test
    void returnsResumeDataFromDownstreamService() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ResumeClient client = new ResumeClient(restTemplate, "http://localhost:8081///");

        server.expect(requestTo("http://localhost:8081/api/v1/resumes/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "3"))
                .andRespond(withSuccess("""
                        {
                          "id": 7,
                          "userId": 3,
                          "title": "Senior Engineer",
                          "targetRole": "Backend Engineer",
                          "templateKey": "classic",
                          "templateId": 1,
                          "summary": "Experienced backend developer",
                          "atsScore": 85,
                          "language": "en",
                          "public": false,
                          "viewCount": 10,
                          "status": "DRAFT",
                          "version": 2
                        }
                        """, MediaType.APPLICATION_JSON));

        ResumeData data = client.getResume(3L, 7L);

        assertEquals("Senior Engineer", data.name());
        assertEquals("Senior Engineer", data.title());
        assertEquals("Experienced backend developer", data.summary());
        assertEquals("classic", data.template());
        server.verify();
    }

    @Test
    void returnsNullWhenDownstreamCallFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ResumeClient client = new ResumeClient(restTemplate, "http://localhost:8081");

        server.expect(requestTo("http://localhost:8081/api/v1/resumes/9"))
                .andRespond(withServerError());

        assertNull(client.getResume(1L, 9L));
        server.verify();
    }
}
