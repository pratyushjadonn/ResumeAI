package com.example.export_service.service;

import com.example.export_service.client.ResumeData;
import com.example.export_service.client.SectionData;
import com.example.export_service.dto.ExportRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorServiceTest {

    private final PdfGeneratorService pdfGeneratorService = new PdfGeneratorService();

    @TempDir
    Path tempDir;

    @Test
    void generatesPdfWithSummaryAndSections() throws Exception {
        Path output = tempDir.resolve("resume.pdf");

        Path generated = pdfGeneratorService.generatePdf(
                new ExportRequest(1L, 10L, "modern-professional"),
                new ResumeData("Alice Johnson", "Senior Java Developer", "Builds resilient APIs", "modern-professional"),
                List.of(
                        new SectionData("EXPERIENCE", "Experience", "- Built scalable APIs\n- Improved latency"),
                        new SectionData("PROJECTS", "Projects", "Developed ResumeAI platform")
                ),
                output
        );

        assertEquals(output, generated);
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void generatesPdfWhenResumeAndSectionsAreMissing() throws Exception {
        Path output = tempDir.resolve("empty.pdf");

        pdfGeneratorService.generatePdf(null, null, null, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void generatesPdfForUnknownTemplateAndEmptySectionContent() throws Exception {
        Path output = tempDir.resolve("fallback.pdf");

        pdfGeneratorService.generatePdf(
                new ExportRequest(2L, 11L, "unknown-template"),
                new ResumeData(null, "Platform Engineer", null, "unknown-template"),
                List.of(new SectionData("CERTIFICATIONS", null, "")),
                output
        );

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }
}
