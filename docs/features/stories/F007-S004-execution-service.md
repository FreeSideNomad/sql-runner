# F007-S004: Build Query Execution Service

## User Story

**As a** developer
**I want** a service for executing queries
**So that** execution logic is encapsulated

## Acceptance Criteria

- [ ] Given query and parameters, then SQL executed
- [ ] Given SELECT query, then results returned
- [ ] Given connection name, then correct DataSource used
- [ ] Given parameter types, then values converted correctly
- [ ] Given execution, then logged to audit trail

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/QueryExecutionService.java`
- `src/main/java/com/ivamare/dto/ExecutionResult.java`
- `src/main/java/com/ivamare/dto/RowData.java`

### Execution Service
```java
@Service
@RequiredArgsConstructor
public class QueryExecutionService {
    private final ConnectionRegistry connectionRegistry;
    private final ExecutionLogService logService;
    private final YamlConfigValidator configValidator;

    public ExecutionResult executeSelect(String queryId, Map<String, String> rawParams, String executedBy) {
        Query query = queryRepository.findById(queryId).orElseThrow();
        QueryConfig config = parseConfig(query);

        // Convert parameters
        Map<String, Object> params = convertParameters(rawParams, config.getParameters());

        // Get DataSource
        DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
        NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

        long startTime = System.currentTimeMillis();
        try {
            List<Map<String, Object>> results = jdbc.queryForList(config.getSql(), params);
            long duration = System.currentTimeMillis() - startTime;

            // Log execution
            logService.logExecution(query, params, results.size(), duration, ExecutionStatus.SUCCESS, null);

            return ExecutionResult.success(results, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logService.logExecution(query, params, 0, duration, ExecutionStatus.FAILED, e.getMessage());
            return ExecutionResult.failure(e.getMessage(), duration);
        }
    }

    private Map<String, Object> convertParameters(Map<String, String> raw, List<ParameterConfig> configs) {
        Map<String, Object> converted = new HashMap<>();
        for (ParameterConfig pc : configs) {
            String value = raw.get(pc.getName());
            converted.put(pc.getName(), convertValue(value, pc.getType()));
        }
        return converted;
    }
}
```

### Execution Result DTO
```java
@Data
@AllArgsConstructor
public class ExecutionResult {
    private boolean success;
    private List<Map<String, Object>> rows;
    private List<String> columns;
    private int rowCount;
    private long executionTimeMs;
    private String errorMessage;

    public static ExecutionResult success(List<Map<String, Object>> rows, long timeMs) {
        List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
        return new ExecutionResult(true, rows, columns, rows.size(), timeMs, null);
    }

    public static ExecutionResult failure(String error, long timeMs) {
        return new ExecutionResult(false, List.of(), List.of(), 0, timeMs, error);
    }
}
```

## Test Plan

- [ ] Unit test: Parameter conversion works
- [ ] Integration test: SELECT query executes
- [ ] Integration test: Execution logged

## Parent Feature

Relates to F007-select-execution
