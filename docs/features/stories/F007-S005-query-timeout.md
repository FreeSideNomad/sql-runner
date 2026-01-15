# F007-S005: Implement Query Timeout Handling

## User Story

**As a** user
**I want** long-running queries to timeout
**So that** the application remains responsive

## Acceptance Criteria

- [ ] Given query execution, then timeout enforced
- [ ] Given default timeout, then 60 seconds
- [ ] Given configurable timeout, then per-query override possible
- [ ] Given timeout exceeded, then TIMEOUT status logged
- [ ] Given timeout, then error message displayed to user

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/QueryExecutionService.java`
- `src/main/resources/application.yml`

### Configuration
```yaml
sqlrunner:
  execution:
    default-timeout-seconds: 60
```

### Timeout Implementation
```java
@Service
public class QueryExecutionService {
    @Value("${sqlrunner.execution.default-timeout-seconds:60}")
    private int defaultTimeoutSeconds;

    public ExecutionResult executeWithTimeout(String queryId, Map<String, String> params, String user) {
        QueryConfig config = getConfig(queryId);
        int timeout = config.getTimeoutSeconds() != null ?
            config.getTimeoutSeconds() : defaultTimeoutSeconds;

        DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());

        try (Connection conn = ds.getConnection()) {
            conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), timeout * 1000);

            try (PreparedStatement stmt = conn.prepareStatement(config.getSql())) {
                stmt.setQueryTimeout(timeout);
                // Execute query
            }
        } catch (SQLTimeoutException e) {
            logService.logExecution(query, params, 0, timeout * 1000L,
                ExecutionStatus.TIMEOUT, "Query exceeded " + timeout + " second timeout");
            return ExecutionResult.timeout(timeout);
        }
    }
}
```

### YAML Config Extension
```yaml
sql: |
  SELECT * FROM large_table
parameters: []
timeoutSeconds: 120  # Override default
```

### Result Extension
```java
public static ExecutionResult timeout(int timeoutSeconds) {
    return new ExecutionResult(false, List.of(), List.of(), 0,
        timeoutSeconds * 1000L, "Query timed out after " + timeoutSeconds + " seconds");
}
```

## Test Plan

- [ ] Unit test: Timeout applied to statement
- [ ] Integration test: Long query times out
- [ ] Integration test: TIMEOUT status logged

## Parent Feature

Relates to F007-select-execution
