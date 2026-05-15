# Export Service - Async Job Integration

## Overview

This guide shows how to integrate the RabbitMQ async export job dispatcher into the Export Service controller to enable non-blocking export processing.

## Key Components

### 1. ExportJobPublisher
- **Location:** `export-service/src/main/java/com/example/export_service/messaging/ExportJobPublisher.java`
- **Purpose:** Publishes export job events to RabbitMQ queues
- **Methods:**
  - `publishExportJob()` - Publish new export job for processing
  - `publishJobCompleted()` - Notify completion to notification service
  - `publishJobFailed()` - Notify failure to notification service

### 2. ExportJobConsumer
- **Location:** `export-service/src/main/java/com/example/export_service/messaging/ExportJobConsumer.java`
- **Purpose:** Consumes export job events and processes them asynchronously
- **Features:**
  - Automatic retry logic (max 3 retries by default)
  - Status tracking (PENDING → PROCESSING → COMPLETED/FAILED)
  - Event publishing for completion/failure

## Integration Steps

### Step 1: Update ExportController

Modify your `ExportController` to use async job dispatch:

```java
package com.example.export_service.controller;

import com.example.export_service.dto.ExportRequest;
import com.example.export_service.messaging.ExportJobPublisher;
import com.example.export_service.model.ExportJob;
import com.example.export_service.service.ExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final ExportJobPublisher exportJobPublisher;

    /**
     * Export to PDF - Async via RabbitMQ
     */
    @PostMapping("/pdf")
    public ResponseEntity<Map<String, Object>> exportPdf(@Valid @RequestBody ExportRequest request) {
        // Create job entry in database (status: PENDING)
        ExportJob job = exportService.createExportJob(request.getUserId(), 
                                                      request.getResumeId(), 
                                                      "PDF", 
                                                      "export.pdf");
        
        // Publish async event to RabbitMQ
        exportJobPublisher.publishExportJob(
            job.getJobId(),
            request.getUserId(),
            request.getResumeId(),
            "PDF",
            "export.pdf"
        );

        // Return job ID to client immediately (non-blocking)
        return ResponseEntity.accepted().body(Map.of(
            "jobId", job.getJobId(),
            "status", "PENDING",
            "message", "Export job queued. Check status with /api/v1/exports/{jobId}"
        ));
    }

    /**
     * Export to DOCX - Async via RabbitMQ
     */
    @PostMapping("/docx")
    public ResponseEntity<Map<String, Object>> exportDocx(@Valid @RequestBody ExportRequest request) {
        ExportJob job = exportService.createExportJob(request.getUserId(), 
                                                      request.getResumeId(), 
                                                      "DOCX", 
                                                      "export.docx");
        
        exportJobPublisher.publishExportJob(
            job.getJobId(),
            request.getUserId(),
            request.getResumeId(),
            "DOCX",
            "export.docx"
        );

        return ResponseEntity.accepted().body(Map.of(
            "jobId", job.getJobId(),
            "status", "PENDING",
            "message", "Export job queued. Check status with /api/v1/exports/{jobId}"
        ));
    }

    /**
     * Export to JSON - Async via RabbitMQ
     */
    @PostMapping("/json")
    public ResponseEntity<Map<String, Object>> exportJson(@Valid @RequestBody ExportRequest request) {
        ExportJob job = exportService.createExportJob(request.getUserId(), 
                                                      request.getResumeId(), 
                                                      "JSON", 
                                                      "export.json");
        
        exportJobPublisher.publishExportJob(
            job.getJobId(),
            request.getUserId(),
            request.getResumeId(),
            "JSON",
            "export.json"
        );

        return ResponseEntity.accepted().body(Map.of(
            "jobId", job.getJobId(),
            "status", "PENDING",
            "message", "Export job queued. Check status with /api/v1/exports/{jobId}"
        ));
    }

    /**
     * Get export job status
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ExportJob> getStatus(@PathVariable Long jobId) {
        ExportJob job = exportService.getJobStatus(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(job);
    }

    /**
     * List user's export jobs
     */
    @GetMapping
    public ResponseEntity<List<ExportJob>> getByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(exportService.getExportsByUser(userId));
    }

    /**
     * Download completed export
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadExport(@PathVariable Long jobId) {
        ExportJob job = exportService.getJobStatus(jobId);
        
        if (job == null || job.getFilePath() == null || job.getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        if (!"COMPLETED".equals(job.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(null); // Job not completed yet
        }

        java.io.File file = new java.io.File(job.getFilePath());
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource resource = 
            new org.springframework.core.io.FileSystemResource(file);
        
        String fileName = (job.getFileName() == null || job.getFileName().isBlank()) 
            ? file.getName() 
            : job.getFileName();

        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                   "attachment; filename=\"" + fileName + "\"")
            .body(resource);
    }
}
```

### Step 2: Update ExportService

Add support for creating pending jobs:

```java
public interface ExportService {
    
    // New methods for async processing
    ExportJob createExportJob(Long userId, Long resumeId, String format, String fileName);
    
    // Existing methods
    ExportJob exportPdf(ExportRequest request);
    ExportJob exportDocx(ExportRequest request);
    ExportJob exportJson(ExportRequest request);
    ExportJob getJobStatus(Long jobId);
    List<ExportJob> getExportsByUser(Long userId);
}
```

### Step 3: API Client Usage Example

#### JavaScript/React
```javascript
// Request export
async function requestExport(resumeId, format) {
  const response = await fetch('/api/v1/exports/' + format.toLowerCase(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ resumeId, userId: 456 })
  });

  const data = await response.json();
  const jobId = data.jobId;

  // Poll for completion
  const maxAttempts = 60; // 5 minutes with 5-second intervals
  for (let i = 0; i < maxAttempts; i++) {
    const statusResponse = await fetch(`/api/v1/exports/${jobId}`);
    const statusData = await statusResponse.json();

    if (statusData.status === 'COMPLETED') {
      // Download the file
      window.location.href = `/api/v1/exports/download/${jobId}`;
      return;
    } else if (statusData.status === 'FAILED') {
      console.error('Export failed:', statusData.errorMessage);
      return;
    }

    // Wait 5 seconds before checking again
    await new Promise(resolve => setTimeout(resolve, 5000));
  }

  console.error('Export timeout');
}

// Usage
await requestExport(789, 'PDF');
```

#### cURL
```bash
# Request export
curl -X POST http://localhost:8086/api/v1/exports/pdf \
  -H "Content-Type: application/json" \
  -d '{"userId": 456, "resumeId": 789}'

# Response: {"jobId": 123, "status": "PENDING", "message": "Export job queued..."}

# Check status
curl http://localhost:8086/api/v1/exports/123

# Download when complete
curl -O http://localhost:8086/api/v1/exports/download/123
```

## Database Schema

Add these fields to your `ExportJob` entity:

```java
@Entity
@Table(name = "export_jobs")
public class ExportJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobId;

    private Long userId;
    private Long resumeId;
    private String format; // PDF, DOCX, JSON
    private String fileName;
    private String filePath;
    
    @Enumerated(EnumType.STRING)
    private ExportStatus status; // PENDING, PROCESSING, COMPLETED, FAILED
    
    private Integer retryCount;
    private String errorMessage;
    
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;
}

enum ExportStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
```

## Monitoring Export Jobs

### Check Queue Status

Access RabbitMQ Management UI:
- URL: http://localhost:15672
- Username: guest
- Password: guest

Navigate to **Queues** tab to see:
- `export.job.queue` - Pending export jobs
- `export.completed.queue` - Completed exports
- `export.failed.queue` - Failed exports

### View Logs

**Export Service:**
```
INFO ExportJobPublisher - Published export job event - jobId: 123
INFO ExportJobConsumer - Processing export job - jobId: 123
INFO ExportJobConsumer - Export job completed successfully - jobId: 123
```

**Notification Service:**
```
INFO ExportJobEventListener - Received export job completed event - jobId: 123
```

## Performance Tips

1. **Consumer Concurrency:**
   ```properties
   spring.rabbitmq.listener.simple.concurrency=3
   spring.rabbitmq.listener.simple.max-concurrency=10
   ```

2. **Prefetch Count:**
   ```properties
   spring.rabbitmq.listener.simple.prefetch=1
   ```

3. **Connection Pooling:**
   ```properties
   spring.rabbitmq.cache.connection.size=3
   ```

## Troubleshooting

### Export Job Not Processing

1. Check if consumer is listening:
   ```
   Look for: "Successfully declared consumer"
   ```

2. Verify queue exists:
   ```bash
   # In RabbitMQ Management UI, check Queues tab
   ```

3. Check logs for errors:
   ```
   ERROR ExportJobConsumer - Error processing export job
   ```

### Messages Piling Up

1. Check if consumer is active
2. Increase concurrency settings
3. Monitor consumer logs for processing errors
4. Check file system disk space

## Next Steps

- [ ] Implement dead letter queue for failed exports
- [ ] Add WebSocket support for real-time status updates
- [ ] Implement export job priority queue
- [ ] Add batch export capability
