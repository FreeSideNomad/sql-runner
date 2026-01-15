# F009-S002: Implement Logging Service

## User Story

**As a** developer
**I want** a logging service
**So that** all executions are consistently logged

## Acceptance Criteria

- [ ] Given execution, then log entry created
- [ ] Given log entry, then all fields populated
- [ ] Given parameters, then serialized to JSON
- [ ] Given error, then error message captured
- [ ] Given logging, then async to not block execution

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/ExecutionLogService.java`

### Service Implementation
```java
@Service
@RequiredArgsConstructor
public class ExecutionLogService {
    private final ExecutionLogRepository repository;
    private final ObjectMapper objectMapper;

    public ExecutionLog logExecution(Query query,
                                    Map<String, Object> parameters,
                                    int rowCount,
                                    long executionTimeMs,
                                    ExecutionStatus status,
                                    String errorMessage,
                                    ExecutionType executionType,
                                    String executedBy) {
        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            paramsJson = "{}";
        }

        ExecutionLog log = ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(query.getId())
            .queryVersion(query.getCurrentVersion())
            .connectionName(query.getConnectionName())
            .executedBy(executedBy)
            .executedAt(LocalDateTime.now())
            .parameters(paramsJson)
            .rowCount(rowCount)
            .executionTimeMs(executionTimeMs)
            .status(status)
            .errorMessage(errorMessage)
            .executionType(executionType)
            .build();

        return repository.save(log);
    }

    // Convenience methods
    public ExecutionLog logSelectSuccess(Query query, Map<String, Object> params,
                                        int rows, long timeMs, String user) {
        return logExecution(query, params, rows, timeMs,
            ExecutionStatus.SUCCESS, null, ExecutionType.SELECT, user);
    }

    public ExecutionLog logSelectFailure(Query query, Map<String, Object> params,
                                        long timeMs, String error, String user) {
        return logExecution(query, params, 0, timeMs,
            ExecutionStatus.FAILED, error, ExecutionType.SELECT, user);
    }

    public ExecutionLog logUpdateSuccess(Query query, Map<String, Object> params,
                                        int rows, long timeMs, String backupId, String user) {
        ExecutionLog log = logExecution(query, params, rows, timeMs,
            ExecutionStatus.SUCCESS, null, ExecutionType.UPDATE, user);
        log.setBackupRecordId(backupId);
        return repository.save(log);
    }
}
```

### Async Logging (Optional)
```java
@Async
public CompletableFuture<ExecutionLog> logExecutionAsync(...) {
    // Same as logExecution but async
    return CompletableFuture.completedFuture(logExecution(...));
}
```

### Integration with Execution Service
```java
// In QueryExecutionService
try {
    List<Map<String, Object>> results = jdbc.queryForList(sql, params);
    long duration = System.currentTimeMillis() - startTime;

    logService.logSelectSuccess(query, params, results.size(), duration, user);

    return ExecutionResult.success(results, duration);
} catch (Exception e) {
    long duration = System.currentTimeMillis() - startTime;
    logService.logSelectFailure(query, params, duration, e.getMessage(), user);
    return ExecutionResult.failure(e.getMessage(), duration);
}
```

## Test Plan

- [ ] Unit test: Log entry created with all fields
- [ ] Unit test: Parameters serialized correctly
- [ ] Integration test: Logs persisted to database

## Parent Feature

Relates to F009-execution-logging
