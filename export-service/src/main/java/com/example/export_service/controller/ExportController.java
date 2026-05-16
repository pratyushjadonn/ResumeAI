package com.example.export_service.controller;

import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportJob;
import com.example.export_service.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    @Value("${export.storage-path:./exports}")
    private String storagePath;

    @PostMapping("/pdf")
    public ExportJob exportPdf(@Valid @RequestBody ExportRequest request) {
        return exportService.exportPdf(request);
    }

    @PostMapping("/docx")
    public ExportJob exportDocx(@Valid @RequestBody ExportRequest request) {
        return exportService.exportDocx(request);
    }

    @PostMapping("/json")
    public ExportJob exportJson(@Valid @RequestBody ExportRequest request) {
        return exportService.exportJson(request);
    }

    @GetMapping("/{jobId}")
    public ExportJob getStatus(@PathVariable Long jobId) {
        return exportService.getJobStatus(jobId);
    }

    @GetMapping
    public List<ExportJob> getByUser(@RequestParam Long userId) {
        return exportService.getExportsByUser(userId);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> downloadExport(@PathVariable Long jobId) {
        ExportJob job = exportService.getJobStatus(jobId);
        if (job == null || job.filePath() == null || job.filePath().isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Path filePath = resolveDownloadPath(job.filePath());
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = new FileSystemResource(filePath);
        String fileName = sanitizeFileName((job.fileName() == null || job.fileName().isBlank())
                ? filePath.getFileName().toString()
                : job.fileName());
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        String contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    private Path resolveDownloadPath(String filePath) {
        Path storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        Path resolvedPath = Path.of(filePath).toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(storageRoot)) {
            return null;
        }
        return resolvedPath;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "")
                .replace("\n", "");
    }
}
