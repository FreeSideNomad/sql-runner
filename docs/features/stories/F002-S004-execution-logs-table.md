# F002-S004: Create Execution Logs Table Migration

## User Story

**As a** developer
**I want** the execution_logs table created via Flyway migration
**So that** query executions can be audited

## Acceptance Criteria

- [ ] Given migration V3__create_execution_logs_table.sql, then table is created
- [ ] Given execution_logs table, then all audit fields exist
- [ ] Given execution_logs table, then parameters stored as JSON text
- [ ] Given execution_logs table, then indexes for common queries exist

## Technical Notes

### Files to Create
- `src/main/resources/db/migration/V3__create_execution_logs_table.sql`

### Table Structure
```sql
CREATE TABLE sqlrunner.execution_logs (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL,
    query_version INT NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    executed_by VARCHAR(100) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parameters TEXT,
    row_count INT,
    execution_time_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    execution_type VARCHAR(20) NOT NULL,
    backup_record_id VARCHAR(36),
    CONSTRAINT fk_execution_logs_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id)
);

CREATE INDEX idx_execution_logs_query ON sqlrunner.execution_logs(query_id);
CREATE INDEX idx_execution_logs_user ON sqlrunner.execution_logs(executed_by);
CREATE INDEX idx_execution_logs_date ON sqlrunner.execution_logs(executed_at);
```

## Test Plan

- [ ] Integration test: Table exists after migration
- [ ] Integration test: Can store JSON parameters
- [ ] Integration test: Indexes created

## Parent Feature

Relates to F002-database-schema
