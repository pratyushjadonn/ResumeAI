package com.example.export_service.controller;

import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportJob;
import com.example.export_service.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

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

        File file = new File(job.filePath());
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = new FileSystemResource(file);
        String fileName = (job.fileName() == null || job.fileName().isBlank()) ? file.getName() : job.fileName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
