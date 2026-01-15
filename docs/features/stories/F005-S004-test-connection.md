# F005-S004: Implement Test Connection Endpoint

## User Story

**As an** administrator
**I want** to test database connections
**So that** I can verify configuration is correct

## Acceptance Criteria

- [ ] Given test connection request, then connection attempted
- [ ] Given successful connection, then success response returned
- [ ] Given failed connection, then error message returned
- [ ] Given test endpoint, then Admin role required
- [ ] Given connection test, then timeout enforced (5 seconds)

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/AdminConnectionController.java`
- `src/main/java/com/ivamare/dto/TestConnectionResponse.java`

### Controller
```java
@Controller
@RequestMapping("/admin/connections")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminConnectionController {
    private final ConnectionRegistry connectionRegistry;

    @PostMapping("/{connectionId}/test")
    @ResponseBody
    public TestConnectionResponse testConnection(@PathVariable String connectionId) {
        try {
            DataSource ds = connectionRegistry.getDataSource(connectionId);
            try (Connection conn = ds.getConnection()) {
                // Execute validation query with 5 second timeout
                conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), 5000);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(connectionRegistry.getValidationQuery(connectionId));
                }
            }
            return TestConnectionResponse.success("Connection successful");
        } catch (Exception e) {
            return TestConnectionResponse.failure(e.getMessage());
        }
    }
}
```

### Response DTO
```java
@Data
@AllArgsConstructor
public class TestConnectionResponse {
    private boolean success;
    private String message;
    private long responseTimeMs;

    public static TestConnectionResponse success(String message) {
        return new TestConnectionResponse(true, message, 0);
    }

    public static TestConnectionResponse failure(String message) {
        return new TestConnectionResponse(false, message, 0);
    }
}
```

## Test Plan

- [ ] Integration test: Successful connection returns success
- [ ] Integration test: Failed connection returns error message
- [ ] Integration test: Non-admin cannot access endpoint

## Parent Feature

Relates to F005-database-connections
