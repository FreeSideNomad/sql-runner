package com.ivamare.service;

import com.ivamare.config.ConnectionProperties;
import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.config.ConnectionProperties.PoolConfig;
import com.ivamare.dto.ConnectionInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Service for managing database connections defined in configuration. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionRegistry {

  private final ConnectionProperties properties;
  private final Environment environment;
  private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

  /**
   * Get DataSource for a connection name. Creates the connection pool lazily on first access.
   *
   * @param connectionName The connection identifier from configuration
   * @return DataSource for the connection
   * @throws IllegalArgumentException if connection name is not configured
   */
  public DataSource getDataSource(String connectionName) {
    return dataSources.computeIfAbsent(connectionName, this::createDataSource);
  }

  /**
   * List all configured connections with their status.
   *
   * @return List of connection information DTOs
   */
  public List<ConnectionInfo> listConnections() {
    return properties.getDatabases().entrySet().stream()
        .map(
            entry -> {
              ConnectionInfo info = ConnectionInfo.from(entry.getKey(), entry.getValue());
              // Check if pool exists and is running
              HikariDataSource ds = dataSources.get(entry.getKey());
              if (ds != null && ds.isRunning()) {
                info.setConnected(true);
              }
              return info;
            })
        .toList();
  }

  /**
   * Test a connection by attempting to connect and run validation query.
   *
   * @param connectionName The connection to test
   * @return ConnectionInfo with test results
   */
  public ConnectionInfo testConnection(String connectionName) {
    ConnectionConfig config = properties.getDatabases().get(connectionName);
    if (config == null) {
      return ConnectionInfo.builder()
          .id(connectionName)
          .connected(false)
          .errorMessage("Unknown connection: " + connectionName)
          .build();
    }

    ConnectionInfo info = ConnectionInfo.from(connectionName, config);

    try {
      DataSource ds = getDataSource(connectionName);
      try (Connection conn = ds.getConnection()) {
        conn.createStatement().execute(config.getType().getValidationQuery());
        info.setConnected(true);
        log.info("Connection test successful: {}", connectionName);
      }
    } catch (SQLException e) {
      info.setConnected(false);
      info.setErrorMessage(e.getMessage());
      log.warn("Connection test failed for {}: {}", connectionName, e.getMessage());
    }

    return info;
  }

  /**
   * Check if a connection name is configured.
   *
   * @param connectionName The connection name to check
   * @return true if configured
   */
  public boolean hasConnection(String connectionName) {
    return properties.getDatabases().containsKey(connectionName);
  }

  /**
   * Get connection configuration by name.
   *
   * @param connectionName The connection name
   * @return ConnectionConfig or null if not found
   */
  public ConnectionConfig getConnectionConfig(String connectionName) {
    return properties.getDatabases().get(connectionName);
  }

  /** Close all connection pools on shutdown. */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down {} connection pools", dataSources.size());
    dataSources.values().forEach(HikariDataSource::close);
    dataSources.clear();
  }

  private HikariDataSource createDataSource(String name) {
    ConnectionConfig config = properties.getDatabases().get(name);
    if (config == null) {
      throw new IllegalArgumentException("Unknown connection: " + name);
    }

    log.info("Creating connection pool for: {} ({})", name, config.getType());

    String username = resolveCredential(config.getCredentialPrefix(), "USER");
    String password = resolveCredential(config.getCredentialPrefix(), "PASSWORD");

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setPoolName("sqlrunner-" + name);
    hikariConfig.setDriverClassName(config.getType().getDriverClass());
    hikariConfig.setJdbcUrl(
        config
            .getType()
            .buildJdbcUrl(
                config.getHost(),
                config.getEffectivePort(),
                config.getDatabase(),
                config.getSchema()));
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.setConnectionTestQuery(config.getType().getValidationQuery());

    // Apply pool settings
    PoolConfig pool = config.getPool();
    hikariConfig.setMaximumPoolSize(pool.getMaximumPoolSize());
    hikariConfig.setMinimumIdle(pool.getMinimumIdle());
    hikariConfig.setConnectionTimeout(pool.getConnectionTimeout());
    hikariConfig.setIdleTimeout(pool.getIdleTimeout());
    hikariConfig.setMaxLifetime(pool.getMaxLifetime());

    // Apply additional properties
    if (config.getProperties() != null) {
      config.getProperties().forEach(hikariConfig::addDataSourceProperty);
    }

    return new HikariDataSource(hikariConfig);
  }

  private String resolveCredential(String prefix, String suffix) {
    if (prefix == null || prefix.isEmpty()) {
      // Return empty string for databases that don't require credentials (like H2)
      return "";
    }
    String envVar = prefix + "_" + suffix;
    String value = environment.getProperty(envVar);
    if (value == null) {
      log.warn("Credential environment variable not set: {}", envVar);
      // Return empty string to avoid null in HikariConfig
      return "";
    }
    return value;
  }
}
