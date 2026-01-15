# F005-S002: Implement Connection Registry Service

## User Story

**As a** developer
**I want** a service to manage database connections
**So that** connections can be retrieved by name

## Acceptance Criteria

- [ ] Given connection name, then return corresponding DataSource
- [ ] Given invalid connection name, then throw descriptive error
- [ ] Given connection list request, then return all configured connections
- [ ] Given lazy initialization, then connection pools created on first use
- [ ] Given environment variables, then credentials resolved at runtime

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/ConnectionRegistry.java`

### Connection Registry Service
```java
@Service
@RequiredArgsConstructor
public class ConnectionRegistry {
    private final ConnectionProperties properties;
    private final Environment environment;
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    public DataSource getDataSource(String connectionName) {
        return dataSources.computeIfAbsent(connectionName, this::createDataSource);
    }

    public List<ConnectionInfo> listConnections() {
        return properties.getDatabases().entrySet().stream()
            .map(e -> new ConnectionInfo(e.getKey(), e.getValue()))
            .toList();
    }

    private DataSource createDataSource(String name) {
        ConnectionConfig config = properties.getDatabases().get(name);
        if (config == null) {
            throw new IllegalArgumentException("Unknown connection: " + name);
        }

        String username = environment.getProperty(config.getCredentialPrefix() + "_USER");
        String password = environment.getProperty(config.getCredentialPrefix() + "_PASSWORD");

        // Build HikariDataSource
        return buildDataSource(config, username, password);
    }
}
```

### Connection Info DTO
```java
@Data
@AllArgsConstructor
public class ConnectionInfo {
    private String id;
    private String name;
    private DatabaseType type;
    private String host;
    private int port;
    private String database;
    private boolean connected; // From health check
}
```

## Test Plan

- [ ] Unit test: DataSource created with correct properties
- [ ] Unit test: Invalid connection throws exception
- [ ] Integration test: Connection retrieved by name

## Parent Feature

Relates to F005-database-connections
