# F007-S010: Implement Query Cancellation

## User Story

**As a** user
**I want** to cancel a running query
**So that** I can stop long-running operations

## Acceptance Criteria

- [ ] Given running query, then Cancel button shown
- [ ] Given Cancel click, then query terminated
- [ ] Given cancellation, then CANCELLED status logged
- [ ] Given cancellation, then user sees confirmation
- [ ] Given cancel, then connection returned to pool

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/QueryExecutionService.java`
- `src/main/java/com/ivamare/controller/QueryExecutionController.java`
- `src/main/resources/templates/queries/execute.html`

### Execution Tracking
```java
@Service
public class QueryExecutionService {
    private final Map<String, Statement> runningStatements = new ConcurrentHashMap<>();

    public ExecutionResult execute(String executionId, String queryId, Map<String, String> params, String user) {
        // ... setup ...

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            runningStatements.put(executionId, stmt);

            try {
                ResultSet rs = stmt.executeQuery();
                // ... process results ...
            } finally {
                runningStatements.remove(executionId);
            }
        }
    }

    public boolean cancelExecution(String executionId) {
        Statement stmt = runningStatements.get(executionId);
        if (stmt != null) {
            try {
                stmt.cancel();
                return true;
            } catch (SQLException e) {
                log.warn("Failed to cancel statement: {}", e.getMessage());
            }
        }
        return false;
    }
}
```

### Cancel Endpoint
```java
@PostMapping("/executions/{executionId}/cancel")
@ResponseBody
public Map<String, Object> cancelExecution(@PathVariable String executionId) {
    boolean cancelled = queryExecutionService.cancelExecution(executionId);
    return Map.of("success", cancelled);
}
```

### UI Cancel Button
```html
<button id="cancel-btn"
        onclick="cancelExecution()"
        class="hidden px-4 py-2 bg-red-600 text-white rounded-lg">
    Cancel
</button>

<script>
let currentExecutionId;

function executeQuery(form) {
    currentExecutionId = crypto.randomUUID();
    document.getElementById('cancel-btn').classList.remove('hidden');
    // ... rest of execution ...
}

async function cancelExecution() {
    const response = await fetch(`/executions/${currentExecutionId}/cancel`, {
        method: 'POST'
    });
    const result = await response.json();
    if (result.success) {
        document.getElementById('execution-status').innerHTML =
            '<div class="bg-yellow-50 p-4 rounded">Query cancelled</div>';
    }
}
</script>
```

## Test Plan

- [ ] Integration test: Running query can be cancelled
- [ ] Integration test: CANCELLED status logged
- [ ] Integration test: Cancel button appears during execution

## Parent Feature

Relates to F007-select-execution
