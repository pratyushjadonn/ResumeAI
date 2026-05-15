# RabbitMQ Integration Guide

## Overview

This document describes the RabbitMQ integration for the ResumeAI platform, enabling:
- **Async Export Job Dispatch**: Non-blocking file export processing with retry logic
- **AI Quota Reset Scheduling**: Automatic monthly quota resets with event notifications
- **Notification Event Streaming**: Real-time notifications for export completion and quota resets

## Architecture

### Message Queues

#### 1. Export Service
```
Exchange: export.events.exchange (Topic Exchange)
├── Queue: export.job.queue
│   └── Routing Key: export.job.dispatch
├── Queue: export.completed.queue
│   └── Routing Key: export.job.completed
└── Queue: export.failed.queue
    └── Routing Key: export.job.failed
```

**Flow:**
1. User requests export (PDF/DOCX/JSON) via Export Service API
2. Export Service publishes `ExportJobEvent` to `export.job.queue`
3. Export Job Consumer processes the job asynchronously
4. On completion, publishes `ExportJobCompletedEvent` to notification queue
5. Notification Service creates user notification

#### 2. AI Service - Quota Reset
```
Exchange: quota.events.exchange (Topic Exchange)
└── Queue: quota.reset.queue
    └── Routing Key: quota.reset
```

**Flow:**
1. Scheduler runs monthly at 00:00 UTC on 1st of each month
2. Publishes `QuotaResetEvent` for each user
3. Notification Service receives and creates notifications

#### 3. Notification Service
```
Exchange: resume.events.exchange (Topic Exchange)
└── Queue: resume.created.queue (existing)

Exchange: export.events.exchange (Topic Exchange)
├── Queue: export.completed.queue
└── Queue: export.failed.queue

Exchange: quota.events.exchange (Topic Exchange)
└── Queue: quota.reset.notification.queue
```

## Configuration

### Docker Compose

RabbitMQ is configured in `docker-compose.rabbitmq.yml`:

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: resumeai-rabbitmq
    ports:
      - "5672:5672"      # AMQP port
      - "15672:15672"    # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
```

**Start RabbitMQ:**
```bash
docker-compose -f docker-compose.rabbitmq.yml up -d
```

**Access Management UI:**
- URL: http://localhost:15672
- Username: guest
- Password: guest

### Environment Variables

#### Export Service (`export-service/src/main/resources/application.properties`)

```properties
# RabbitMQ Connection
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Export Job Queues
RABBITMQ_EXPORT_EVENTS_EXCHANGE=export.events.exchange
RABBITMQ_EXPORT_JOB_QUEUE=export.job.queue
RABBITMQ_EXPORT_JOB_ROUTING_KEY=export.job.dispatch
RABBITMQ_EXPORT_COMPLETED_QUEUE=export.completed.queue
RABBITMQ_EXPORT_COMPLETED_ROUTING_KEY=export.job.completed
RABBITMQ_EXPORT_FAILED_QUEUE=export.failed.queue
RABBITMQ_EXPORT_FAILED_ROUTING_KEY=export.job.failed
```

#### AI Service (`ai-service/src/main/resources/application.properties`)

```properties
# RabbitMQ Connection
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Quota Reset Configuration
RABBITMQ_QUOTA_EVENTS_EXCHANGE=quota.events.exchange
RABBITMQ_QUOTA_RESET_QUEUE=quota.reset.queue
RABBITMQ_QUOTA_RESET_ROUTING_KEY=quota.reset
QUOTA_RESET_ENABLED=true

# Quota Limits
AI_FREE_GENERATION_LIMIT=5
AI_FREE_ATS_LIMIT=3
AI_PREMIUM_GENERATION_LIMIT=100
AI_PREMIUM_ATS_LIMIT=50
```

#### Notification Service (`notification-service/src/main/resources/application.properties`)

```properties
# RabbitMQ Connection
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# All Event Exchanges
RABBITMQ_RESUME_EVENTS_EXCHANGE=resume.events.exchange
RABBITMQ_EXPORT_EVENTS_EXCHANGE=export.events.exchange
RABBITMQ_QUOTA_EVENTS_EXCHANGE=quota.events.exchange

# Queue Configurations
RABBITMQ_RESUME_CREATED_QUEUE=resume.created.queue
RABBITMQ_EXPORT_COMPLETED_QUEUE=export.completed.queue
RABBITMQ_EXPORT_FAILED_QUEUE=export.failed.queue
RABBITMQ_QUOTA_RESET_NOTIF_QUEUE=quota.reset.notification.queue
```

## Event Schemas

### 1. ExportJobEvent

Published by: **Export Service**
Consumed by: **Export Job Consumer**

```json
{
  "job_id": 123,
  "user_id": 456,
  "resume_id": 789,
  "format": "PDF",
  "file_name": "Resume_2024.pdf",
  "status": "PENDING",
  "retry_count": 0,
  "max_retries": 3,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z",
  "error_message": null
}
```

### 2. ExportJobCompletedEvent

Published by: **Export Service** (on completion)
Consumed by: **Notification Service**

```json
{
  "job_id": 123,
  "user_id": 456,
  "resume_id": 789,
  "format": "PDF",
  "file_name": "Resume_2024.pdf",
  "file_path": "/exports/Resume_2024.pdf",
  "status": "COMPLETED",
  "error_message": null,
  "download_url": "http://localhost:8086/api/v1/exports/download/123",
  "completed_at": "2024-01-15T10:35:45Z",
  "execution_time_ms": 5450
}
```

### 3. QuotaResetEvent

Published by: **AI Service Scheduler**
Consumed by: **Notification Service**

```json
{
  "user_id": 456,
  "quota_type": "GENERATION",
  "previous_quota": 5,
  "new_quota": 5,
  "reset_date": "2024-02",
  "is_premium": false,
  "timestamp": "2024-02-01T00:00:00Z",
  "event_id": "uuid-string-here"
}
```

## Usage Examples

### 1. Trigger Export Job Asynchronously

**Request:**
```bash
curl -X POST http://localhost:8086/api/v1/exports/pdf \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 456,
    "resumeId": 789,
    "fileName": "My_Resume.pdf"
  }'
```

**Response:**
```json
{
  "jobId": 123,
  "userId": 456,
  "status": "PENDING",
  "message": "Export job queued for processing"
}
```

### 2. Check Export Job Status

**Request:**
```bash
curl http://localhost:8086/api/v1/exports/123
```

**Response:**
```json
{
  "jobId": 123,
  "userId": 456,
  "status": "COMPLETED",
  "filePath": "/exports/Resume_2024.pdf",
  "fileName": "My_Resume.pdf"
}
```

### 3. Download Completed Export

**Request:**
```bash
curl -O http://localhost:8086/api/v1/exports/download/123
```

### 4. Retrieve Notifications

**Request:**
```bash
curl "http://localhost:8087/api/v1/notifications?userId=456"
```

**Response:**
```json
{
  "notifications": [
    {
      "id": 1,
      "userId": 456,
      "title": "Export Complete",
      "message": "Your PDF export is ready! File: My_Resume.pdf",
      "type": "EXPORT_COMPLETED",
      "timestamp": "2024-01-15T10:35:45Z",
      "read": false
    },
    {
      "id": 2,
      "userId": 456,
      "title": "Generation Quota Reset - Free",
      "message": "Your generation quota has been reset to 5 for 2024-02",
      "type": "QUOTA_RESET",
      "timestamp": "2024-02-01T00:00:00Z",
      "read": false
    }
  ]
}
```

## Scheduling

### Monthly Quota Reset Schedule

**Default:** 1st of every month at 00:00 UTC

The scheduler is configured in `QuotaResetScheduler`:
```java
@Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
public void resetMonthlyQuotas()
```

**Cron Expression Breakdown:**
- `0` - Second: 0
- `0` - Minute: 0
- `0` - Hour: 0 (UTC)
- `1` - Day of month: 1st
- `*` - Month: Every month
- `*` - Day of week: Every day (ignored when day of month is specified)

**To disable quota reset:**
Set environment variable: `QUOTA_RESET_ENABLED=false`

## Monitoring

### RabbitMQ Management UI

Access at: http://localhost:15672

**Key Metrics to Monitor:**
1. **Queues Tab:**
   - Message count in each queue
   - Consumer count
   - Acknowledgment status

2. **Connections Tab:**
   - Active connections from services
   - Channel status

3. **Nodes Tab:**
   - Memory usage
   - Disk space

### Logging

Services log all message operations:

**Export Service:**
```
INFO ExportJobPublisher - Published export job event - jobId: 123, userId: 456, format: PDF
INFO ExportJobConsumer - Processing export job - jobId: 123, userId: 456, format: PDF
INFO ExportJobConsumer - Export job completed successfully - jobId: 123, format: PDF
```

**AI Service:**
```
INFO QuotaResetPublisher - Published quota reset event - userId: 456, quotaType: GENERATION, newQuota: 5
INFO QuotaResetScheduler - Resetting generation quotas...
INFO QuotaResetScheduler - Generation quota reset published for 1000 users
```

**Notification Service:**
```
INFO ExportJobEventListener - Received export job completed event - jobId: 123, userId: 456, format: PDF
INFO QuotaResetEventListener - Received quota reset event - userId: 456, quotaType: GENERATION, newQuota: 5
```

## Error Handling

### Retry Logic

**Export Jobs:**
- Max retries: 3 (configurable)
- Retry on: Network failure, processing error
- Backoff: Immediate (can be enhanced)
- Status: Marked as FAILED after max retries

**Event Publishing:**
- Failed events are logged
- Non-critical events don't halt service
- Consider implementing dead-letter queues for failed events

### Dead Letter Queue (Optional Enhancement)

To implement DLQ for failed messages:

```java
@Bean
public Queue exportJobDLQ() {
    return new Queue("export.job.dlq", true);
}

@Bean
public Binding exportJobDLQBinding() {
    return BindingBuilder.bind(exportJobDLQ())
        .to(exportEventsExchange())
        .with("export.job.*");
}
```

## Performance Considerations

1. **Concurrency:**
   - Configure consumer prefetch: `spring.rabbitmq.listener.simple.prefetch`
   - Tune based on export processing time and server resources

2. **Message Persistence:**
   - Queues are durable (survives RabbitMQ restart)
   - Enable persistent delivery mode for reliability

3. **Batch Processing:**
   - Consider batch quota resets for large user bases
   - Use `publishBatchQuotaReset()` for efficiency

4. **Message Size:**
   - Events are JSON-serialized
   - Typical event size: 200-500 bytes
   - Consider compression for high throughput

## Troubleshooting

### Common Issues

1. **Connection Refused**
   ```
   Error: java.net.ConnectException: Connection refused
   Solution: Ensure RabbitMQ is running (docker-compose up -d)
   ```

2. **Queue Not Found**
   ```
   Error: spring.rabbitmq.listener.simple.missing-queues-fatal=true
   Solution: Set to false or ensure queues exist before consuming
   ```

3. **Message Not Being Processed**
   ```
   Check:
   - Consumer is listening (@RabbitListener active)
   - Exchange and routing key are correctly bound
   - Message converter is configured
   - Check logs for deserialization errors
   ```

4. **High Memory Usage**
   ```
   Solution:
   - Reduce prefetch count
   - Increase consumer concurrency
   - Monitor queue depths
   - Purge DLQ if needed
   ```

## Future Enhancements

1. **Dead Letter Queue (DLQ):**
   - Capture failed events for analysis and reprocessing

2. **Message Encryption:**
   - Add SSL/TLS for RabbitMQ connections

3. **Circuit Breaker Pattern:**
   - Implement Resilience4j circuit breaker for export jobs

4. **Audit Logging:**
   - Track all quota changes and export requests

5. **Event Versioning:**
   - Support multiple event schema versions for backward compatibility

6. **Performance Optimization:**
   - Implement batch event processing
   - Add message compression

7. **Priority Queues:**
   - Prioritize export jobs from premium users

8. **Webhooks:**
   - Send export completion events to external systems

## References

- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP Documentation](https://spring.io/projects/spring-amqp)
- [Spring Scheduling Documentation](https://spring.io/guides/gs/scheduling-tasks/)
