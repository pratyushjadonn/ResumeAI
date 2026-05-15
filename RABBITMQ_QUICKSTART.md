# RabbitMQ Implementation - Quick Start Guide

## Summary of Changes

This implementation adds RabbitMQ integration to the ResumeAI platform for three key features:

### 1. ✅ Async Export Job Dispatch
- Non-blocking file export processing (PDF, DOCX, JSON)
- Automatic retry logic (max 3 retries)
- Status tracking and user notifications

### 2. ✅ AI Quota Reset Scheduling
- Monthly quota resets (1st of each month, 00:00 UTC)
- Separate quotas for GENERATION and ATS checks
- Configurable limits per user tier (Free/Premium)

### 3. ✅ Notification Event Streaming
- Real-time notifications for export completion/failure
- Quota reset notifications
- Event-driven architecture

## Files Created/Modified

### RabbitMQ Configuration Files
- ✅ [export-service/src/main/java/com/example/export_service/config/RabbitMqConfig.java](export-service/src/main/java/com/example/export_service/config/RabbitMqConfig.java)
- ✅ [ai-service/src/main/java/com/example/ai_service/config/RabbitMqConfig.java](ai-service/src/main/java/com/example/ai_service/config/RabbitMqConfig.java)
- ✅ [notification-service/src/main/java/com/example/notification_service/config/RabbitMqConfig.java](notification-service/src/main/java/com/example/notification_service/config/RabbitMqConfig.java) (Updated)

### Event DTOs
**Export Service:**
- ✅ [ExportJobEvent.java](export-service/src/main/java/com/example/export_service/messaging/ExportJobEvent.java)

**AI Service:**
- ✅ [QuotaResetEvent.java](ai-service/src/main/java/com/example/ai_service/messaging/QuotaResetEvent.java)

**Notification Service:**
- ✅ [ExportJobCompletedEvent.java](notification-service/src/main/java/com/example/notification_service/messaging/ExportJobCompletedEvent.java)
- ✅ [QuotaResetNotificationEvent.java](notification-service/src/main/java/com/example/notification_service/messaging/QuotaResetNotificationEvent.java)

### Message Producers/Consumers
**Export Service:**
- ✅ [ExportJobPublisher.java](export-service/src/main/java/com/example/export_service/messaging/ExportJobPublisher.java)
- ✅ [ExportJobConsumer.java](export-service/src/main/java/com/example/export_service/messaging/ExportJobConsumer.java)

**AI Service:**
- ✅ [QuotaResetPublisher.java](ai-service/src/main/java/com/example/ai_service/messaging/QuotaResetPublisher.java)
- ✅ [QuotaResetScheduler.java](ai-service/src/main/java/com/example/ai_service/scheduler/QuotaResetScheduler.java)

**Notification Service:**
- ✅ [ExportJobEventListener.java](notification-service/src/main/java/com/example/notification_service/messaging/ExportJobEventListener.java)
- ✅ [QuotaResetEventListener.java](notification-service/src/main/java/com/example/notification_service/messaging/QuotaResetEventListener.java)

### Configuration Updates
- ✅ [export-service/pom.xml](export-service/pom.xml) - Added spring-boot-starter-amqp
- ✅ [ai-service/pom.xml](ai-service/pom.xml) - Added spring-boot-starter-amqp, spring-boot-starter-scheduling
- ✅ [export-service/src/main/resources/application.properties](export-service/src/main/resources/application.properties) - Added RabbitMQ config
- ✅ [ai-service/src/main/resources/application.properties](ai-service/src/main/resources/application.properties) - Added RabbitMQ and quota config
- ✅ [notification-service/src/main/resources/application.properties](notification-service/src/main/resources/application.properties) - Updated with all exchanges

### Documentation
- ✅ [RABBITMQ_INTEGRATION.md](RABBITMQ_INTEGRATION.md) - Complete RabbitMQ integration guide
- ✅ [EXPORT_ASYNC_INTEGRATION.md](EXPORT_ASYNC_INTEGRATION.md) - Export async job integration guide
- ✅ [QUOTA_RESET_SCHEDULER.md](QUOTA_RESET_SCHEDULER.md) - Quota reset scheduling guide

## Quick Start

### 1. Start RabbitMQ
```bash
cd e:\resumeai-parent
docker-compose -f docker-compose.rabbitmq.yml up -d
```

### 2. Verify RabbitMQ
- Management UI: http://localhost:15672
- Username: guest
- Password: guest

### 3. Build Services
```bash
mvn clean install -DskipTests
```

### 4. Run Services
```bash
# Terminal 1 - Export Service
cd export-service
mvn spring-boot:run

# Terminal 2 - AI Service
cd ai-service
mvn spring-boot:run

# Terminal 3 - Notification Service
cd notification-service
mvn spring-boot:run
```

### 5. Test Export Job Dispatch
```bash
# Create export job
curl -X POST http://localhost:8086/api/v1/exports/pdf \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "resumeId": 1,
    "fileName": "resume.pdf"
  }'

# Response: {"jobId": 123, "status": "PENDING"}

# Check status
curl http://localhost:8086/api/v1/exports/123
```

### 6. Test Quota Reset (Manual)
```bash
# Manually trigger quota reset (create admin endpoint)
curl -X POST http://localhost:8085/api/v1/admin/quotas/reset-all

# Check notifications
curl "http://localhost:8087/api/v1/notifications?userId=1"
```

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    RabbitMQ Message Broker                   │
│                    (localhost:5672)                           │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  [export.events.exchange]        [quota.events.exchange]     │
│  ├─ export.job.queue             ├─ quota.reset.queue       │
│  ├─ export.completed.queue       └─ (consumers: notif-svc)  │
│  ├─ export.failed.queue          [resume.events.exchange]   │
│  └─ (consumers: export-svc)       ├─ resume.created.queue   │
│                                   └─ (consumers: notif-svc)  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
         ↑           ↑                      ↑
         │           │                      │
    [EXPORT-SVC]  [AI-SVC]          [NOTIFICATION-SVC]
    - Publish     - Publish            - Listen & Create
      export      quota reset            notifications
      jobs        events
    - Consume
      & process
      export
      jobs
```

## Data Flows

### Export Job Processing
```
User Request
    ↓
ExportController.exportPdf()
    ↓
ExportService.createExportJob() → Store PENDING job in DB
    ↓
ExportJobPublisher.publishExportJob()
    ↓
RabbitMQ: export.job.queue
    ↓
ExportJobConsumer.processExportJob()
    ↓
Generate File (PDF/DOCX/JSON)
    ↓
Update Job Status to COMPLETED
    ↓
ExportJobPublisher.publishJobCompleted()
    ↓
RabbitMQ: export.completed.queue
    ↓
NotificationService: Create notification for user
    ↓
User receives notification with download link
```

### Quota Reset Flow
```
Monthly Schedule (00:00 UTC on 1st)
    ↓
QuotaResetScheduler.resetMonthlyQuotas()
    ↓
Retrieve all active users
    ↓
For each user:
  ├─ Determine tier (Free/Premium)
  ├─ Set appropriate quota limit
  └─ QuotaResetPublisher.publishQuotaReset()
    ↓
RabbitMQ: quota.reset.queue
    ↓
QuotaResetEventListener (Notification Service)
    ↓
Create notification: "Quota reset to X for [YYYY-MM]"
    ↓
User receives notification
```

## Configuration Properties

All properties have sensible defaults but can be overridden:

```properties
# RabbitMQ Connection (all services)
spring.rabbitmq.host=localhost          # Default: localhost
spring.rabbitmq.port=5672               # Default: 5672
spring.rabbitmq.username=guest          # Default: guest
spring.rabbitmq.password=guest          # Default: guest

# Export Service Specific
RABBITMQ_EXPORT_EVENTS_EXCHANGE=export.events.exchange
RABBITMQ_EXPORT_JOB_QUEUE=export.job.queue
RABBITMQ_EXPORT_JOB_ROUTING_KEY=export.job.dispatch

# AI Service Specific
RABBITMQ_QUOTA_EVENTS_EXCHANGE=quota.events.exchange
QUOTA_RESET_ENABLED=true

# Quota Limits
AI_FREE_GENERATION_LIMIT=5
AI_FREE_ATS_LIMIT=3
AI_PREMIUM_GENERATION_LIMIT=100
AI_PREMIUM_ATS_LIMIT=50
```

## Key Features

### Async Export Processing
- ✅ Non-blocking API responses (HTTP 202 Accepted)
- ✅ Automatic retry logic (configurable: default 3 retries)
- ✅ Status tracking (PENDING → PROCESSING → COMPLETED/FAILED)
- ✅ User notifications on completion
- ✅ Error handling and logging

### Monthly Quota Reset
- ✅ Automatic scheduling (configurable cron expression)
- ✅ Per-user tier support (Free/Premium)
- ✅ Separate quotas for GENERATION and ATS
- ✅ User notifications
- ✅ Event-driven architecture

### Event-Driven Notifications
- ✅ Real-time notifications for export events
- ✅ Real-time notifications for quota resets
- ✅ Extensible notification system
- ✅ JSON-based event serialization
- ✅ Topic-based routing with wildcards

## Monitoring & Troubleshooting

### Check RabbitMQ Status
```bash
# Connection test
telnet localhost 5672

# Docker logs
docker logs resumeai-rabbitmq

# RabbitMQ Management UI
http://localhost:15672
```

### Check Service Logs
```bash
# Export Service
curl http://localhost:8086/actuator/health

# AI Service
curl http://localhost:8085/actuator/health

# Notification Service
curl http://localhost:8087/actuator/health
```

### Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Connection refused | Start RabbitMQ: `docker-compose -f docker-compose.rabbitmq.yml up -d` |
| Messages not processing | Check consumer logs, verify queue bindings in UI |
| High memory usage | Reduce prefetch count, increase consumer concurrency |
| Events not flowing | Verify exchange/queue/binding exist in RabbitMQ UI |

## Next Steps (Optional Enhancements)

1. **Dead Letter Queue (DLQ)**
   - Capture failed messages for analysis
   - Implement retry mechanism with exponential backoff

2. **WebSocket Support**
   - Real-time export status updates to UI
   - Live notification streaming

3. **Audit Logging**
   - Track all quota changes
   - Log all export requests

4. **Performance Optimization**
   - Batch quota resets for large user bases
   - Message compression for high throughput

5. **Advanced Scheduling**
   - Custom reset dates per user
   - Promotional quota grants

6. **Security Enhancement**
   - SSL/TLS for RabbitMQ connections
   - Role-based access control

## Resources

- 📚 [RabbitMQ Official Docs](https://www.rabbitmq.com/documentation.html)
- 📚 [Spring AMQP Documentation](https://spring.io/projects/spring-amqp)
- 📚 [Spring Scheduling Guide](https://spring.io/guides/gs/scheduling-tasks/)
- 📄 [RABBITMQ_INTEGRATION.md](RABBITMQ_INTEGRATION.md) - Detailed integration guide
- 📄 [EXPORT_ASYNC_INTEGRATION.md](EXPORT_ASYNC_INTEGRATION.md) - Export setup guide
- 📄 [QUOTA_RESET_SCHEDULER.md](QUOTA_RESET_SCHEDULER.md) - Quota setup guide

## Support

For issues or questions:
1. Check the relevant documentation file (links above)
2. Review logs in service terminals
3. Check RabbitMQ Management UI (http://localhost:15672)
4. Verify configuration properties in application.properties
