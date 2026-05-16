package com.example.export_service.controller;

import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportFormat;
import com.example.export_service.model.ExportJob;
import com.example.export_service.model.ExportJobStatus;
import com.example.export_service.service.ExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @TempDir
    Path tempDir;

    @Test
    void delegatesCreateAndReadOperations() {
        ExportController controller = new ExportController(exportService);
        ExportRequest request = new ExportRequest(1L, 2L, "classic");
        ExportJob job = job(10L, null, null);

        when(exportService.exportPdf(request)).thenReturn(job);
        when(exportService.exportDocx(request)).thenReturn(job);
        when(exportService.exportJson(request)).thenReturn(job);
        when(exportService.getJobStatus(10L)).thenReturn(job);
        when(exportService.getExportsByUser(1L)).thenReturn(List.of(job));

        assertEquals(job, controller.exportPdf(request));
        assertEquals(job, controller.exportDocx(request));
        assertEquals(job, controller.exportJson(request));
        assertEquals(job, controller.getStatus(10L));
        assertEquals(List.of(job), controller.getByUser(1L));
    }

    @Test
    void downloadReturnsFileResponseForStoredExport() throws Exception {
        ExportController controller = new ExportController(exportService);
        ReflectionTestUtils.setField(controller, "storagePath", tempDir.toString());
        Path file = Files.writeString(tempDir.resolve("resume.pdf"), "pdf-content");
        ExportJob job = job(11L, file.toString(), "resume\n.pdf");

        when(exportService.getJobStatus(11L)).thenReturn(job);

        var response = controller.downloadExport(11L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("resume.pdf"));
        Resource body = response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertInstanceOf(Resource.class, body);
    }

    @Test
    void downloadReturnsNotFoundWhenPathIsBlankOrMissing() {
        ExportController controller = new ExportController(exportService);
        ReflectionTestUtils.setField(controller, "storagePath", tempDir.toString());
        when(exportService.getJobStatus(12L)).thenReturn(job(12L, " ", "resume.pdf"));

        var response = controller.downloadExport(12L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadReturnsNotFoundForFilesOutsideStorageRoot() throws Exception {
        ExportController controller = new ExportController(exportService);
        ReflectionTestUtils.setField(controller, "storagePath", tempDir.toString());
        Path outsideFile = Files.writeString(tempDir.getParent().resolve("outside.pdf"), "bad");
        when(exportService.getJobStatus(13L)).thenReturn(job(13L, outsideFile.toString(), "outside.pdf"));

        var response = controller.downloadExport(13L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private ExportJob job(Long id, String filePath, String fileName) {
        Instant now = Instant.now();
        return new ExportJob(
                id,
                1L,
                2L,
                ExportFormat.PDF,
                ExportJobStatus.COMPLETED,
                "http://localhost/download/" + id,
                now,
                now.plusSeconds(3600),
                filePath,
                fileName
        );
    }
}
