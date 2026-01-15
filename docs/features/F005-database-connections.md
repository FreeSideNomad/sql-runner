# F005: Database Connections

## Description

Implement multi-database connection management supporting SQL Server, DB2, and PostgreSQL. Connections are defined in application configuration with credentials from environment variables.

## Acceptance Criteria

- [ ] Connection configuration via application.yml
- [ ] Support for SQL Server, DB2, PostgreSQL connection types
- [ ] Credentials loaded from environment variables
- [ ] Connection pooling (HikariCP)
- [ ] Admin "Test Connection" functionality
- [ ] Connection list displayed in admin UI
- [ ] Connections available in query editor dropdown

## User Stories

- [ ] F005-S001: Create connection configuration model
- [ ] F005-S002: Implement connection registry service
- [ ] F005-S003: Configure HikariCP connection pools
- [ ] F005-S004: Implement test connection endpoint
- [ ] F005-S005: Create admin connections list page
- [ ] F005-S006: Add connection dropdown to query editor

## Technical Notes

- Use `@ConfigurationProperties` for connection config
- Lazy initialization of connection pools
- Credentials pattern: `{PREFIX}_USER`, `{PREFIX}_PASSWORD`
- Validation query per database type

## Dependencies

- F002 (database schema)
- F004 (UI layout for admin pages)
