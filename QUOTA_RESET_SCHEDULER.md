# AI Service - Quota Reset Scheduling

## Overview

This guide explains the AI Service quota reset scheduling system powered by RabbitMQ events and Spring Scheduling.

## Architecture

### Monthly Quota Reset Flow

```
QuotaResetScheduler (1st of month at 00:00 UTC)
    ↓
Retrieve all active users
    ↓
For each user:
    ├─ Determine premium status
    ├─ Calculate new quota
    └─ PublishQuotaResetEvent
        ↓
    QuotaResetEventListener (Notification Service)
        ↓
    Create notification for user
```

## Key Components

### 1. QuotaResetPublisher
- **Location:** `ai-service/src/main/java/com/example/ai_service/messaging/QuotaResetPublisher.java`
- **Purpose:** Publishes quota reset events to RabbitMQ
- **Methods:**
  - `publishQuotaReset()` - Publish single user quota reset
  - `publishBatchQuotaReset()` - Publish batch quota resets

### 2. QuotaResetScheduler
- **Location:** `ai-service/src/main/java/com/example/ai_service/scheduler/QuotaResetScheduler.java`
- **Purpose:** Scheduled task for monthly quota resets
- **Features:**
  - Configurable schedule (default: 1st of month at 00:00 UTC)
  - Separate reset for GENERATION and ATS quotas
  - Configurable quota limits

### 3. QuotaResetEventListener
- **Location:** `notification-service/src/main/java/com/example/notification_service/messaging/QuotaResetEventListener.java`
- **Purpose:** Consumes quota reset events and creates notifications

## Configuration

### Application Properties

**File:** `ai-service/src/main/resources/application.properties`

```properties
# Enable/Disable quota reset
app.rabbit.quota-reset.enabled=true

# Quota Limits
app.ai.free-monthly-generation-limit=5
app.ai.free-monthly-ats-limit=3
app.ai.premium-monthly-generation-limit=100
app.ai.premium-monthly-ats-limit=50
```

### RabbitMQ Configuration

```properties
# RabbitMQ Connection
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Quota Reset Exchange
app.rabbit.quota-events.exchange=quota.events.exchange
app.rabbit.quota-reset.queue=quota.reset.queue
app.rabbit.quota-reset.routing-key=quota.reset
```

## Scheduling

### Default Schedule

The quota reset is scheduled using Spring's `@Scheduled` annotation:

```java
@Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
public void resetMonthlyQuotas()
```

**Cron Expression:** `0 0 0 1 * *`
- `0` - Second: 0
- `0` - Minute: 0
- `0` - Hour: 0 (UTC)
- `1` - Day of month: 1st
- `*` - Month: Every month
- `*` - Day of week: Ignored

### Custom Schedules

To change the schedule, update the `QuotaResetScheduler.resetMonthlyQuotas()` method:

```java
// Run on the 15th of each month at 2:00 AM UTC
@Scheduled(cron = "0 0 2 15 * *", zone = "UTC")

// Run every Monday at 12:00 noon UTC
@Scheduled(cron = "0 0 12 * * MON", zone = "UTC")

// Run every 6 hours
@Scheduled(fixedDelay = 21600000) // 6 hours in milliseconds

// Run once at startup, then every day
@Scheduled(initialDelay = 0, fixedDelay = 86400000) // 1 day in milliseconds
```

## Event Schema

### QuotaResetEvent

```json
{
  "user_id": 456,
  "quota_type": "GENERATION",
  "previous_quota": 5,
  "new_quota": 5,
  "reset_date": "2024-02",
  "is_premium": false,
  "timestamp": "2024-02-01T00:00:00Z",
  "event_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Quota Levels

### Free Tier (Default)
- **Generation Requests:** 5 per month
- **ATS Checks:** 3 per month

### Premium Tier
- **Generation Requests:** 100 per month
- **ATS Checks:** 50 per month

### Quota Types
- `GENERATION` - Text generation, summaries, bullets, cover letters, etc.
- `ATS` - ATS compatibility checks

## Usage Examples

### Manual Quota Reset (For Testing)

Create a test endpoint to trigger quota reset manually:

```java
@RestController
@RequestMapping("/api/v1/admin/quotas")
@RequiredArgsConstructor
public class QuotaAdminController {

    private final QuotaResetScheduler quotaResetScheduler;
    private final QuotaResetPublisher quotaResetPublisher;

    /**
     * Trigger immediate quota reset (admin only)
     */
    @PostMapping("/reset-all")
    public ResponseEntity<Map<String, String>> resetAllQuotas() {
        quotaResetScheduler.resetMonthlyQuotas();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Quota reset triggered for all users"
        ));
    }

    /**
     * Reset quota for specific user
     */
    @PostMapping("/reset/{userId}")
    public ResponseEntity<Map<String, Object>> resetUserQuota(
            @PathVariable Long userId,
            @RequestParam String quotaType) {
        
        boolean isPremium = false; // Get from user service
        int newQuota = isPremium ? 100 : 5; // Adjust based on quota type

        quotaResetPublisher.publishQuotaReset(userId, quotaType, 0, newQuota, isPremium);

        return ResponseEntity.accepted().body(Map.of(
            "userId", userId,
            "quotaType", quotaType,
            "newQuota", newQuota,
            "message", "Quota reset event published"
        ));
    }

    /**
     * Reset quota for batch of users
     */
    @PostMapping("/reset-batch")
    public ResponseEntity<Map<String, Object>> resetBatchQuota(
            @RequestBody QuotaResetBatchRequest request) {
        
        quotaResetPublisher.publishBatchQuotaReset(
            request.getUserIds(),
            request.getQuotaType(),
            request.getNewQuota(),
            request.getUserPremiumStatus()
        );

        return ResponseEntity.accepted().body(Map.of(
            "userCount", request.getUserIds().size(),
            "quotaType", request.getQuotaType(),
            "message", "Batch quota reset triggered"
        ));
    }
}

// Request DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
class QuotaResetBatchRequest {
    private List<Long> userIds;
    private String quotaType; // GENERATION or ATS
    private Integer newQuota;
    private Map<Long, Boolean> userPremiumStatus;
}
```

### Checking Current Quota Usage

```java
@RestController
@RequestMapping("/api/v1/quotas")
@RequiredArgsConstructor
public class QuotaController {

    private final AiAssistantService aiAssistantService;

    /**
     * Get current quota for user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserQuota(@PathVariable Long userId) {
        QuotaResponse quota = aiAssistantService.checkQuota(userId);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "generationQuotaUsed", quota.getGenerationUsed(),
            "generationQuotaRemaining", quota.getGenerationRemaining(),
            "atsCheckQuotaUsed", quota.getAtsUsed(),
            "atsCheckQuotaRemaining", quota.getAtsRemaining(),
            "resetDate", quota.getResetDate()
        ));
    }
}
```

## Monitoring

### Check Scheduled Tasks

**Verify scheduler is running:**
```
Look for logs:
- "Starting monthly quota reset process..."
- "Monthly quota reset completed successfully"
```

### Monitor Events Published

**RabbitMQ Management UI:**
1. Go to http://localhost:15672
2. Click **Exchanges** tab
3. Select `quota.events.exchange`
4. Check message counts

### View Notifications Created

```bash
# Get all notifications for a user
curl "http://localhost:8087/api/v1/notifications?userId=456"

# Get quota reset notifications
curl "http://localhost:8087/api/v1/notifications?userId=456&type=QUOTA_RESET"
```

## Troubleshooting

### Scheduler Not Running

1. **Check if scheduling is enabled:**
   ```java
   @EnableScheduling // Must be present on a @Configuration or @SpringBootApplication class
   ```

2. **Verify in logs:**
   ```
   Look for: "Scheduling initialized..."
   ```

3. **Check timezone:**
   ```
   Ensure zone = "UTC" matches your server timezone
   ```

### Events Not Being Published

1. **Check RabbitMQ connection:**
   ```bash
   telnet localhost 5672
   ```

2. **Verify queue exists:**
   - Check RabbitMQ Management UI
   - Queue should be: `quota.reset.queue`

3. **Check logs for errors:**
   ```
   ERROR QuotaResetPublisher - Failed to publish quota reset event
   ```

### Notifications Not Created

1. **Verify listener is active:**
   ```
   Look for: "QuotaResetEventListener: Successfully declared listener"
   ```

2. **Check notification service logs:**
   ```
   INFO QuotaResetEventListener - Received quota reset event
   ```

3. **Verify database connection:**
   - Check if notifications table exists
   - Verify INSERT permissions

## Performance Optimization

### Batch Processing

Instead of publishing events one-by-one:

```java
// Before (slow for many users)
for (Long userId : allUserIds) {
    quotaResetPublisher.publishQuotaReset(userId, "GENERATION", 0, 5, false);
}

// After (optimized)
quotaResetPublisher.publishBatchQuotaReset(
    allUserIds, 
    "GENERATION", 
    5, 
    userPremiumStatusMap
);
```

### Concurrent Event Processing

Configure consumer concurrency:

```properties
spring.rabbitmq.listener.simple.concurrency=5
spring.rabbitmq.listener.simple.max-concurrency=10
```

### Database Optimization

Add indexes for better notification creation performance:

```sql
CREATE INDEX idx_quota_reset_events ON notifications(type, created_at);
```

## Integration with User Service

To fetch all active users and their premium status:

```java
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private final UserServiceClient userServiceClient;
    private final QuotaResetPublisher quotaResetPublisher;

    private List<Long> getAllUserIds() {
        return userServiceClient.getAllActiveUserIds();
    }

    private Map<Long, Boolean> getUserPremiumStatus(List<Long> userIds) {
        return userServiceClient.getPremiumStatusBatch(userIds);
    }
}

// Create Feign client
@FeignClient(name = "user-service", url = "http://localhost:8084")
public interface UserServiceClient {
    
    @GetMapping("/api/v1/users/active")
    List<Long> getAllActiveUserIds();

    @PostMapping("/api/v1/users/premium-status")
    Map<Long, Boolean> getPremiumStatusBatch(@RequestBody List<Long> userIds);
}
```

## Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class QuotaResetSchedulerTest {

    @Mock
    private QuotaResetPublisher quotaResetPublisher;

    @Mock
    private AiRequestRepository aiRequestRepository;

    @InjectMocks
    private QuotaResetScheduler quotaResetScheduler;

    @Test
    void shouldResetGenerationQuotasMonthly() {
        // Given
        List<Long> userIds = List.of(1L, 2L, 3L);
        when(quotaResetScheduler.getAllUserIds()).thenReturn(userIds);

        // When
        quotaResetScheduler.resetMonthlyQuotas();

        // Then
        verify(quotaResetPublisher, times(3)).publishQuotaReset(
            anyLong(),
            eq("GENERATION"),
            anyInt(),
            anyInt(),
            anyBoolean()
        );
    }
}
```

### Integration Test

```java
@SpringBootTest
@EnableRabbit
class QuotaResetIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationService notificationService;

    @Test
    void shouldCreateNotificationOnQuotaReset() {
        // Given
        QuotaResetEvent event = QuotaResetEvent.builder()
            .userId(1L)
            .quotaType("GENERATION")
            .newQuota(5)
            .isPremium(false)
            .build();

        // When
        rabbitTemplate.convertAndSend("quota.events.exchange", "quota.reset", event);

        // Then - Wait for async processing
        Thread.sleep(1000);
        List<Notification> notifications = notificationService.getByUser(1L);
        assertThat(notifications).hasSize(1);
    }
}
```

## Future Enhancements

1. **Dynamic Quota Adjustment:**
   - Adjust quotas based on usage patterns
   - Offer temporary quota increases

2. **Usage Analytics:**
   - Track quota usage trends
   - Identify power users

3. **Promotional Quotas:**
   - Grant temporary bonus quotas for events
   - Rollback to normal quotas

4. **Quota Marketplace:**
   - Allow users to purchase additional quotas
   - Support quota gifting between users

5. **Advanced Scheduling:**
   - Custom reset dates per user
   - Different quota cycles (weekly, yearly)
