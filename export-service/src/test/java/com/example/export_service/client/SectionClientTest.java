package com.example.export_service.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SectionClientTest {

    @Test
    void returnsSectionsFromDownstreamService() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SectionClient client = new SectionClient(restTemplate, "http://localhost:8082///");

        server.expect(requestTo("http://localhost:8082/api/v1/resumes/12/sections"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "5"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1,
                            "resumeId": 12,
                            "userId": 5,
                            "title": "Experience",
                            "type": "EXPERIENCE",
                            "content": "Built APIs",
                            "displayOrder": 1,
                            "visible": true,
                            "aiGenerated": false
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<SectionData> sections = client.getSections(5L, 12L);

        assertEquals(1, sections.size());
        assertEquals("EXPERIENCE", sections.get(0).type());
        assertEquals("Experience", sections.get(0).title());
        assertEquals("Built APIs", sections.get(0).content());
        server.verify();
    }

    @Test
    void returnsEmptyListWhenDownstreamCallFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SectionClient client = new SectionClient(restTemplate, "http://localhost:8082");

        server.expect(requestTo("http://localhost:8082/api/v1/resumes/14/sections"))
                .andRespond(withServerError());

        assertTrue(client.getSections(2L, 14L).isEmpty());
        server.verify();
    }
}
