# F002: Database Schema & Migrations

## Description

Set up Flyway database migrations and create the application schema for SQL Runner. The schema includes tables for queries, query versions, execution logs, and backup records. Use H2 for testing and SQL Server for development/production.

## Acceptance Criteria

- [ ] Flyway configured for schema migrations
- [ ] H2 database configured for testing
- [ ] SQL Server connection configured for dev/prod
- [ ] `queries` table created with all required columns
- [ ] `query_versions` table created with YAML config storage
- [ ] `execution_logs` table created for audit trail
- [ ] `backup_records` table created for UPDATE rollback data
- [ ] All indexes created per spec
- [ ] Schema is `sqlrunner`

## User Stories

- [ ] F002-S001: Configure Flyway and database profiles
- [ ] F002-S002: Create queries table migration
- [ ] F002-S003: Create query_versions table migration
- [ ] F002-S004: Create execution_logs table migration
- [ ] F002-S005: Create backup_records table migration
- [ ] F002-S006: Create JPA entities for all tables

## Technical Notes

- Use H2 in-memory for `@DataJpaTest` and unit tests
- Use SQL Server for integration tests and dev profile
- Flyway scripts in `src/main/resources/db/migration/`
- Schema prefix: `sqlrunner.`

## Dependencies

- F001 (dev environment)
