package com.example.export_service.service;

import com.example.export_service.client.ResumeClient;
import com.example.export_service.client.ResumeData;
import com.example.export_service.client.SectionClient;
import com.example.export_service.client.SectionData;
import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportFormat;
import com.example.export_service.model.ExportJob;
import com.example.export_service.model.ExportJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryExportServiceTest {

    @Mock
    private PdfGeneratorService pdfGeneratorService;

    @Mock
    private ResumeClient resumeClient;

    @Mock
    private SectionClient sectionClient;

    @TempDir
    Path tempDir;

    private InMemoryExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new InMemoryExportService(
                pdfGeneratorService,
                resumeClient,
                sectionClient,
                tempDir.toString(),
                "http://localhost:8080"
        );
    }

    @Test
    void exportPdf_success() throws Exception {
        ExportRequest request = new ExportRequest(1L, 101L, "classic-pro");
        when(resumeClient.getResume(1L, 101L)).thenReturn(new ResumeData("Alice", "Java Developer", "Summary", "classic-pro"));
        when(sectionClient.getSections(1L, 101L)).thenReturn(List.of(new SectionData("EXPERIENCE", "Experience", "Built APIs")));
        doAnswer(invocation -> {
            Path outputFile = invocation.getArgument(3);
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, "pdf-content");
            return outputFile;
        }).when(pdfGeneratorService).generatePdf(eq(request), any(ResumeData.class), any(List.class), any(Path.class));

        ExportJob job = exportService.exportPdf(request);

        assertEquals(ExportJobStatus.COMPLETED, job.status());
        assertEquals(ExportFormat.PDF, job.format());
        assertNotNull(job.filePath());
        assertTrue(Files.exists(Path.of(job.filePath())));
    }

    @Test
    void exportDocx_success() {
        ExportJob job = exportService.exportDocx(new ExportRequest(1L, 102L, "classic-pro"));

        assertEquals(ExportJobStatus.COMPLETED, job.status());
        assertEquals(ExportFormat.DOCX, job.format());
        assertEquals(1L, job.userId());
    }

    @Test
    void getJobStatus_success() {
        ExportJob createdJob = exportService.exportDocx(new ExportRequest(1L, 103L, "classic-pro"));

        ExportJob fetchedJob = exportService.getJobStatus(createdJob.jobId());

        assertEquals(createdJob, fetchedJob);
    }

    @Test
    void getExportsByUser_success() {
        exportService.exportDocx(new ExportRequest(1L, 201L, "classic-pro"));
        exportService.exportJson(new ExportRequest(1L, 202L, "classic-pro"));
        exportService.exportJson(new ExportRequest(2L, 301L, "classic-pro"));

        List<ExportJob> jobs = exportService.getExportsByUser(1L);

        assertEquals(2, jobs.size());
        assertTrue(jobs.stream().allMatch(job -> job.userId().equals(1L)));
    }

    @Test
    void exportPdf_failure_returnsFailedJob() {
        ExportRequest request = new ExportRequest(1L, 104L, "classic-pro");
        when(resumeClient.getResume(1L, 104L)).thenThrow(new IllegalArgumentException("Invalid export request"));

        ExportJob job = exportService.exportPdf(request);

        assertEquals(ExportJobStatus.FAILED, job.status());
        assertEquals(ExportFormat.PDF, job.format());
        verify(sectionClient, never()).getSections(any(), any());
    }
}
