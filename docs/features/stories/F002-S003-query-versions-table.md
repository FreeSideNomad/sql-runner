# F002-S003: Create Query Versions Table Migration

## User Story

**As a** developer
**I want** the query_versions table created via Flyway migration
**So that** versioned query configurations can be stored

## Acceptance Criteria

- [ ] Given migration V2__create_query_versions_table.sql, then table is created
- [ ] Given query_versions table, then foreign key to queries exists
- [ ] Given query_versions table, then unique constraint on (query_id, version)
- [ ] Given query_versions table, then config_yaml column stores YAML text

## Technical Notes

### Files to Create
- `src/main/resources/db/migration/V2__create_query_versions_table.sql`

### Table Structure
```sql
CREATE TABLE sqlrunner.query_versions (
    id VARCHAR(36) PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL,
    version INT NOT NULL,
    config_yaml TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_query_versions_query FOREIGN KEY (query_id)
        REFERENCES sqlrunner.queries(id),
    CONSTRAINT uq_query_version UNIQUE (query_id, version)
);
```

## Test Plan

- [ ] Integration test: Table exists after migration
- [ ] Integration test: Foreign key constraint enforced
- [ ] Integration test: Can store and retrieve YAML config

## Parent Feature

Relates to F002-database-schema
