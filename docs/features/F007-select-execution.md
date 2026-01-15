# F007: Simple SELECT Execution

## Description

Implement the SELECT query execution workflow including dynamic parameter forms, query execution with timeout, paginated results display, and CSV export functionality.

## Acceptance Criteria

- [ ] Query selection from dashboard or query list
- [ ] Dynamic parameter form based on query config
- [ ] Support all parameter types (STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, ENUM, LIST_STRING, LIST_INTEGER)
- [ ] Parameter validation (required, regex, type)
- [ ] Query execution with configurable timeout
- [ ] Server-side pagination (25, 50, 100 rows per page)
- [ ] Sortable columns
- [ ] Execution feedback (spinner, elapsed time, cancel)
- [ ] CSV export (streaming, UTF-8 with BOM)
- [ ] Execution logged to audit trail

## User Stories

- [ ] F007-S001: Create parameter form component (all types)
- [ ] F007-S002: Implement ENUM parameter dropdown
- [ ] F007-S003: Implement LIST parameter textarea
- [ ] F007-S004: Build query execution service
- [ ] F007-S005: Implement query timeout handling
- [ ] F007-S006: Create results table with pagination
- [ ] F007-S007: Add column sorting functionality
- [ ] F007-S008: Implement CSV export (streaming)
- [ ] F007-S009: Add execution progress UI (spinner, timer)
- [ ] F007-S010: Implement query cancellation

## Technical Notes

- Use NamedParameterJdbcTemplate
- LIST types expand to IN clause
- Streaming CSV with `StreamingResponseBody`
- Cancel via `Statement.cancel()`

## Dependencies

- F006 (query templates)
- F009 (execution logging)
