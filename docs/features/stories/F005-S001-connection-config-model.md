# F005-S001: Create Connection Configuration Model

## User Story

**As a** developer
**I want** a model for database connection configuration
**So that** connections can be defined in application.yml

## Acceptance Criteria

- [ ] Given connection config, then name is unique identifier
- [ ] Given connection config, then type specifies database (SQLSERVER, DB2, POSTGRES)
- [ ] Given connection config, then host, port, database defined
- [ ] Given connection config, then credential prefix for env vars defined
- [ ] Given connection config, then validation query per type

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/config/ConnectionProperties.java`
- `src/main/java/com/ivamare/domain/DatabaseType.java` (enum)

### Configuration Model
```java
@ConfigurationProperties(prefix = "sqlrunner.connections")
@Data
public class ConnectionProperties {
    private Map<String, ConnectionConfig> databases = new HashMap<>();

    @Data
    public static class ConnectionConfig {
        private String name;
        private DatabaseType type;
        private String host;
        private int port;
        private String database;
        private String credentialPrefix;
        private String schema;
        private Map<String, String> properties = new HashMap<>();
    }
}
```

### Database Type Enum
```java
public enum DatabaseType {
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "SELECT 1"),
    DB2("com.ibm.db2.jcc.DB2Driver", "SELECT 1 FROM SYSIBM.SYSDUMMY1"),
    POSTGRES("org.postgresql.Driver", "SELECT 1");

    private final String driverClass;
    private final String validationQuery;
    // Constructor, getters
}
```

### Application.yml Example
```yaml
sqlrunner:
  connections:
    databases:
      main-sqlserver:
        name: Main SQL Server
        type: SQLSERVER
        host: sqlserver.company.com
        port: 1433
        database: production
        credential-prefix: MAIN_DB
        schema: dbo
      warehouse-db2:
        name: Data Warehouse
        type: DB2
        host: db2.company.com
        port: 50000
        database: warehouse
        credential-prefix: DW_DB
```

## Test Plan

- [ ] Unit test: Configuration binds correctly
- [ ] Unit test: Validation query per type correct
- [ ] Integration test: Properties loaded from application.yml

## Parent Feature

Relates to F005-database-connections
