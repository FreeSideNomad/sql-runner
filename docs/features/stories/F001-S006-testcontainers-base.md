# F001-S006: Create TestContainers Base Configuration

## User Story

**As a** developer
**I want** a base test class that provides TestContainers for SQL Server, DB2, and PostgreSQL
**So that** integration tests can run against real databases without manual setup

## Acceptance Criteria

- [ ] Given pom.xml, then TestContainers dependencies are configured
- [ ] Given AbstractIntegrationTest, then SQL Server container is configured
- [ ] Given AbstractIntegrationTest, then PostgreSQL container is configured
- [ ] Given AbstractIntegrationTest, then DB2 container is configured
- [ ] Given an integration test extending AbstractIntegrationTest, then all containers start
- [ ] Given TestContainers, then connection properties are dynamically injected

## Technical Notes

### Files to Create
- `src/test/java/com/sqlrunner/AbstractIntegrationTest.java` - Base test class

### Files to Modify
- `pom.xml` - Add TestContainers dependencies

### Dependencies
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mssqlserver</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>db2</artifactId>
    <scope>test</scope>
</dependency>
```

### Container Configuration
```java
@Container
static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(
        "mcr.microsoft.com/mssql/server:2022-latest")
    .acceptLicense();

@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

@Container
static Db2Container db2 = new Db2Container("icr.io/db2_community/db2:latest")
    .acceptLicense();
```

### Dynamic Property Source
Use `@DynamicPropertySource` to inject connection URLs into Spring context.

## Test Plan

- [ ] Integration test: All three containers start successfully
- [ ] Integration test: Can connect to each database using provided properties
- [ ] Manual: Containers shut down after tests complete

## Parent Feature

Relates to F001-dev-setup
