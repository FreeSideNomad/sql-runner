# F002-S001: Configure Flyway and Database Profiles

## User Story

**As a** developer
**I want** Flyway configured with H2 for testing and SQL Server for dev/prod
**So that** database migrations run automatically on application startup

## Acceptance Criteria

- [ ] Given pom.xml, then Flyway dependency is configured
- [ ] Given application.yml, then H2 is configured for test profile
- [ ] Given application.yml, then SQL Server is configured for dev profile
- [ ] Given Flyway, then migrations run on startup
- [ ] Given Flyway, then schema is set to `sqlrunner`
- [ ] Given `mvn test`, then H2 database is used

## Technical Notes

### Files to Modify
- `pom.xml` - Add H2 dependency (test scope)
- `src/main/resources/application.yml` - SQL Server config
- `src/test/resources/application-test.yml` - H2 config

### Flyway Configuration
```yaml
spring:
  flyway:
    enabled: true
    schemas: sqlrunner
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### H2 Test Configuration
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:sqlrunner;MODE=MSSQLServer;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
```

## Test Plan

- [ ] Unit test: Flyway runs migrations on H2
- [ ] Manual: Application starts with SQL Server in dev

## Parent Feature

Relates to F002-database-schema
