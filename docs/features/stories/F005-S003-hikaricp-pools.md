# F005-S003: Configure HikariCP Connection Pools

## User Story

**As a** developer
**I want** connection pooling configured
**So that** database connections are efficiently managed

## Acceptance Criteria

- [ ] Given connection pool, then HikariCP used
- [ ] Given pool config, then max pool size configurable
- [ ] Given pool config, then connection timeout configurable
- [ ] Given pool config, then idle timeout configurable
- [ ] Given connection health, then validation query executed periodically

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/ConnectionRegistry.java`
- `src/main/resources/application.yml`

### HikariCP Configuration
```java
private DataSource buildDataSource(ConnectionConfig config, String username, String password) {
    HikariConfig hikariConfig = new HikariConfig();

    hikariConfig.setJdbcUrl(buildJdbcUrl(config));
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.setDriverClassName(config.getType().getDriverClass());

    // Pool settings
    hikariConfig.setMaximumPoolSize(10);
    hikariConfig.setMinimumIdle(2);
    hikariConfig.setIdleTimeout(300000); // 5 minutes
    hikariConfig.setConnectionTimeout(30000); // 30 seconds
    hikariConfig.setMaxLifetime(1800000); // 30 minutes

    // Validation
    hikariConfig.setConnectionTestQuery(config.getType().getValidationQuery());

    // Pool name for monitoring
    hikariConfig.setPoolName("sqlrunner-" + config.getName());

    return new HikariDataSource(hikariConfig);
}
```

### JDBC URL Building
```java
private String buildJdbcUrl(ConnectionConfig config) {
    return switch (config.getType()) {
        case SQLSERVER -> String.format(
            "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
            config.getHost(), config.getPort(), config.getDatabase());
        case DB2 -> String.format(
            "jdbc:db2://%s:%d/%s",
            config.getHost(), config.getPort(), config.getDatabase());
        case POSTGRES -> String.format(
            "jdbc:postgresql://%s:%d/%s",
            config.getHost(), config.getPort(), config.getDatabase());
    };
}
```

### Pool Configuration in application.yml
```yaml
sqlrunner:
  pool:
    max-size: 10
    min-idle: 2
    connection-timeout: 30000
    idle-timeout: 300000
```

## Test Plan

- [ ] Unit test: HikariConfig populated correctly
- [ ] Integration test: Pool creates connections
- [ ] Integration test: Validation query prevents stale connections

## Parent Feature

Relates to F005-database-connections
