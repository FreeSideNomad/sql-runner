# F002-S002: Create Queries Table Migration

## User Story

**As a** developer
**I want** the queries table created via Flyway migration
**So that** query templates can be stored in the database

## Acceptance Criteria

- [ ] Given migration V1__create_queries_table.sql, then queries table is created
- [ ] Given queries table, then all columns per spec exist
- [ ] Given queries table, then category index exists
- [ ] Given H2 test, then migration runs successfully

## Technical Notes

### Files to Create
- `src/main/resources/db/migration/V1__create_queries_table.sql`

### Table Structure
```sql
CREATE TABLE sqlrunner.queries (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100) NOT NULL,
    connection_name VARCHAR(100) NOT NULL,
    query_type VARCHAR(20) NOT NULL,
    current_version INT NOT NULL DEFAULT 1,
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    created_by VARCHAR(100) NOT NULL,
    updated_at DATETIME2,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_queries_category ON sqlrunner.queries(category);
```

### H2 Compatibility
- Use `BOOLEAN` instead of `BIT` for H2
- Use `TIMESTAMP` instead of `DATETIME2` for H2
- Consider using Flyway callbacks or vendor-specific scripts

## Test Plan

- [ ] Integration test: Table exists after migration
- [ ] Integration test: Can insert and query records

## Parent Feature

Relates to F002-database-schema
