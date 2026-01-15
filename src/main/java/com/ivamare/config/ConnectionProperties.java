package com.ivamare.config;

import com.ivamare.domain.DatabaseType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration properties for database connections defined in application.yml. */
@Data
@Component
@ConfigurationProperties(prefix = "sqlrunner.connections")
public class ConnectionProperties {

  private Map<String, ConnectionConfig> databases = new HashMap<>();

  /** Configuration for a single database connection. */
  @Data
  public static class ConnectionConfig {
    /** Display name for the connection. */
    private String name;

    /** Database type (SQLSERVER, DB2, POSTGRES, H2). */
    private DatabaseType type;

    /** Database host. */
    private String host;

    /** Database port (uses default if not specified). */
    private Integer port;

    /** Database name. */
    private String database;

    /**
     * Environment variable prefix for credentials (e.g., MAIN_DB for MAIN_DB_USER,
     * MAIN_DB_PASSWORD).
     */
    private String credentialPrefix;

    /** Database schema (optional). */
    private String schema;

    /** Additional JDBC properties. */
    private Map<String, String> properties = new HashMap<>();

    /** HikariCP pool configuration. */
    private PoolConfig pool = new PoolConfig();

    /** Get the effective port, using database type default if not specified. */
    public int getEffectivePort() {
      return port != null ? port : (type != null ? type.getDefaultPort() : 0);
    }
  }

  /** HikariCP pool configuration. */
  @Data
  public static class PoolConfig {
    /** Maximum pool size. */
    private int maximumPoolSize = 10;

    /** Minimum idle connections. */
    private int minimumIdle = 2;

    /** Connection timeout in milliseconds. */
    private long connectionTimeout = 30000;

    /** Idle timeout in milliseconds. */
    private long idleTimeout = 600000;

    /** Max lifetime in milliseconds. */
    private long maxLifetime = 1800000;
  }
}
