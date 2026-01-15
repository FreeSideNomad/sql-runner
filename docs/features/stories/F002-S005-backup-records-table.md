# F002-S005: Create Backup Records Table Migration

## User Story

**As a** developer
**I want** the backup_records table created via Flyway migration
**So that** UPDATE workflow backups can be stored for rollback

## Acceptance Criteria

- [ ] Given migration V4__create_backup_records_table.sql, then table is created
- [ ] Given backup_records table, then foreign key to execution_logs exists
- [ ] Given backup_records table, then backup_data stores JSON blob
- [ ] Given backup_records table, then rollback tracking fields exist

## Technical Notes

### Files to Create
- `src/main/resources/db/migration/V4__create_backup_records_table.sql`

### Table Structure
```sql
CREATE TABLE sqlrunner.backup_records (
    id VARCHAR(36) PRIMARY KEY,
    execution_log_id VARCHAR(36) NOT NULL,
    backup_data TEXT NOT NULL,
    row_count INT NOT NULL,
    is_rolled_back BOOLEAN NOT NULL DEFAULT FALSE,
    rolled_back_at TIMESTAMP,
    rolled_back_by VARCHAR(100),
    rollback_execution_log_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_records_execution FOREIGN KEY (execution_log_id)
        REFERENCES sqlrunner.execution_logs(id)
);
```

### Backup Data Format
```json
{
  "columns": ["id", "name", "status", "modified_date"],
  "rows": [
    [1001, "Acme Corp", "A", "2024-01-15T10:30:00"],
    [1002, "Beta Inc", "A", "2024-01-15T11:45:00"]
  ]
}
```

## Test Plan

- [ ] Integration test: Table exists after migration
- [ ] Integration test: Can store and retrieve JSON backup
- [ ] Integration test: Rollback tracking works

## Parent Feature

Relates to F002-database-schema
