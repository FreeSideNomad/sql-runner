# F009-S001: Create ExecutionLog Entity and Repository

## User Story

**As a** developer
**I want** an ExecutionLog entity
**So that** query executions can be persisted

## Acceptance Criteria

- [ ] Given ExecutionLog entity, then all audit fields mapped
- [ ] Given ExecutionLog, then status enum (SUCCESS, FAILED, CANCELLED, TIMEOUT)
- [ ] Given ExecutionLog, then execution type enum (SELECT, UPDATE, ROLLBACK)
- [ ] Given ExecutionLog, then parameters stored as JSON
- [ ] Given repository, then filtering queries available

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/domain/ExecutionLog.java`
- `src/main/java/com/ivamare/domain/ExecutionStatus.java`
- `src/main/java/com/ivamare/domain/ExecutionType.java`
- `src/main/java/com/ivamare/repository/ExecutionLogRepository.java`

### Enums
```java
public enum ExecutionStatus {
    SUCCESS, FAILED, CANCELLED, TIMEOUT
}

public enum ExecutionType {
    SELECT, UPDATE, ROLLBACK
}
```

### Entity
```java
@Entity
@Table(name = "execution_logs", schema = "sqlrunner")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLog {
    @Id
    private String id;

    @Column(name = "query_id", nullable = false)
    private String queryId;

    @Column(name = "query_version", nullable = false)
    private Integer queryVersion;

    @Column(name = "connection_name", nullable = false)
    private String connectionName;

    @Column(name = "executed_by", nullable = false)
    private String executedBy;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters; // JSON

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false)
    private ExecutionType executionType;

    @Column(name = "backup_record_id")
    private String backupRecordId;

    // Transient for display
    @Transient
    private String queryName;
}
```

### Repository
```java
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, String> {

    Page<ExecutionLog> findByExecutedByOrderByExecutedAtDesc(String user, Pageable pageable);

    Page<ExecutionLog> findByQueryIdOrderByExecutedAtDesc(String queryId, Pageable pageable);

    @Query("SELECT e FROM ExecutionLog e WHERE " +
           "(:user IS NULL OR e.executedBy = :user) AND " +
           "(:queryId IS NULL OR e.queryId = :queryId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:startDate IS NULL OR e.executedAt >= :startDate) AND " +
           "(:endDate IS NULL OR e.executedAt <= :endDate) " +
           "ORDER BY e.executedAt DESC")
    Page<ExecutionLog> findWithFilters(
        @Param("user") String user,
        @Param("queryId") String queryId,
        @Param("status") ExecutionStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    List<ExecutionLog> findTop10ByExecutedByOrderByExecutedAtDesc(String user);
}
```

## Test Plan

- [ ] Integration test: CRUD operations work
- [ ] Integration test: Filter query works
- [ ] Integration test: Parameters stored as JSON

## Parent Feature

Relates to F009-execution-logging
