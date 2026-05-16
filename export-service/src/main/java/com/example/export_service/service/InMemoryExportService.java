package com.example.export_service.service;

import com.example.export_service.client.ResumeClient;
import com.example.export_service.client.ResumeData;
import com.example.export_service.client.SectionClient;
import com.example.export_service.client.SectionData;
import com.example.export_service.dto.ExportRequest;
import com.example.export_service.model.ExportFormat;
import com.example.export_service.model.ExportJob;
import com.example.export_service.model.ExportJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class InMemoryExportService implements ExportService {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<Long, ExportJob> jobs = new ConcurrentHashMap<>();
    private final PdfGeneratorService pdfGeneratorService;
    private final ResumeClient resumeClient;
    private final SectionClient sectionClient;
    private final Path storageRoot;
    private final String downloadBaseUrl;

    public InMemoryExportService(
            PdfGeneratorService pdfGeneratorService,
            ResumeClient resumeClient,
            SectionClient sectionClient,
            @Value("${export.storage-path:./exports}") String storagePath,
            @Value("${export.download-base-url:http://localhost:8080}") String downloadBaseUrl
    ) {
        this.pdfGeneratorService = pdfGeneratorService;
        this.resumeClient = resumeClient;
        this.sectionClient = sectionClient;
        this.storageRoot = Path.of(Objects.requireNonNull(storagePath, "storagePath"))
                .toAbsolutePath()
                .normalize();
        this.downloadBaseUrl = downloadBaseUrl;
    }

    @Override
    public ExportJob exportPdf(ExportRequest request) {
        long id = ids.getAndIncrement();
        Instant createdAt = Instant.now();

        ExportJob processingJob = new ExportJob(
                id,
                request.userId(),
                request.resumeId(),
                ExportFormat.PDF,
                ExportJobStatus.PROCESSING,
                buildDownloadUrl(id),
                createdAt,
                createdAt.plusSeconds(7 * 24 * 60 * 60),
                null,
                null
        );
        jobs.put(id, processingJob);

        try {
            ResumeData resume = resumeClient.getResume(request.userId(), request.resumeId());
            List<SectionData> sections = sectionClient.getSections(request.userId(), request.resumeId());

            Path exportsDir = ensureStorageDirectory();
            String fileName = buildPdfFileName(id);
            Path outputFile = exportsDir.resolve(fileName);
            pdfGeneratorService.generatePdf(request, resume, sections, outputFile);

            ExportJob completed = new ExportJob(
                    id,
                    request.userId(),
                    request.resumeId(),
                    ExportFormat.PDF,
                    ExportJobStatus.COMPLETED,
                    buildDownloadUrl(id),
                    createdAt,
                    createdAt.plusSeconds(7 * 24 * 60 * 60),
                    outputFile.toAbsolutePath().toString(),
                    fileName
            );
            jobs.put(id, completed);
            return completed;
        } catch (IOException | RuntimeException ex) {
            log.error("PDF export failed for jobId={} userId={} resumeId={}", id, request.userId(), request.resumeId(), ex);
            ExportJob failed = new ExportJob(
                    id,
                    request.userId(),
                    request.resumeId(),
                    ExportFormat.PDF,
                    ExportJobStatus.FAILED,
                    buildDownloadUrl(id),
                    createdAt,
                    createdAt.plusSeconds(7 * 24 * 60 * 60),
                    null,
                    null
            );
            jobs.put(id, failed);
            return failed;
        }
    }

    @Override
    public ExportJob exportDocx(ExportRequest request) {
        return create(request, ExportFormat.DOCX);
    }

    @Override
    public ExportJob exportJson(ExportRequest request) {
        return create(request, ExportFormat.JSON);
    }

    @Override
    public ExportJob getJobStatus(Long jobId) {
        return jobs.get(jobId);
    }

    @Override
    public List<ExportJob> getExportsByUser(Long userId) {
        return jobs.values().stream()
                .filter(job -> job.userId().equals(userId))
                .sorted(Comparator.comparing(ExportJob::createdAt).reversed())
                .toList();
    }

    private ExportJob create(ExportRequest request, ExportFormat format) {
        long id = ids.getAndIncrement();
        Instant now = Instant.now();
        ExportJob job = new ExportJob(
                id,
                request.userId(),
                request.resumeId(),
                format,
                ExportJobStatus.COMPLETED,
                buildDownloadUrl(id),
                now,
                now.plusSeconds(7 * 24 * 60 * 60),
                null,
                null
        );
        jobs.put(id, job);
        return job;
    }

    private String buildDownloadUrl(Long jobId) {
        String normalizedBaseUrl = downloadBaseUrl.endsWith("/")
                ? downloadBaseUrl.substring(0, downloadBaseUrl.length() - 1)
                : downloadBaseUrl;
        return normalizedBaseUrl + "/api/v1/exports/download/" + jobId;
    }

    private Path ensureStorageDirectory() throws IOException {
        Files.createDirectories(storageRoot);
        return storageRoot;
    }

    private String buildPdfFileName(Long jobId) {
        return UUID.randomUUID() + "-" + jobId + ".pdf";
    }
}
