package com.ivamare.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.config.ConnectionProperties.PoolConfig;
import com.ivamare.domain.DatabaseType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Tests for ConnectionProperties configuration binding. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "sqlrunner.connections.databases.test-h2.name=Test H2 Database",
      "sqlrunner.connections.databases.test-h2.type=H2",
      "sqlrunner.connections.databases.test-h2.host=localhost",
      "sqlrunner.connections.databases.test-h2.database=testdb",
      "sqlrunner.connections.databases.test-h2.pool.maximum-pool-size=5",
      "sqlrunner.connections.databases.test-h2.pool.minimum-idle=1"
    })
class ConnectionPropertiesTest {

  @Autowired private ConnectionProperties connectionProperties;

  @Test
  void connectionProperties_loadsFromYaml() {
    assertThat(connectionProperties).isNotNull();
    assertThat(connectionProperties.getDatabases()).isNotNull();
  }

  @Test
  void connectionConfig_hasCorrectValues() {
    ConnectionConfig config = connectionProperties.getDatabases().get("test-h2");

    assertThat(config).isNotNull();
    assertThat(config.getName()).isEqualTo("Test H2 Database");
    assertThat(config.getType()).isEqualTo(DatabaseType.H2);
    assertThat(config.getHost()).isEqualTo("localhost");
    assertThat(config.getDatabase()).isEqualTo("testdb");
  }

  @Test
  void poolConfig_hasCorrectValues() {
    ConnectionConfig config = connectionProperties.getDatabases().get("test-h2");
    PoolConfig pool = config.getPool();

    assertThat(pool).isNotNull();
    assertThat(pool.getMaximumPoolSize()).isEqualTo(5);
    assertThat(pool.getMinimumIdle()).isEqualTo(1);
  }

  @Test
  void effectivePort_usesDefaultWhenNotSpecified() {
    ConnectionConfig config = connectionProperties.getDatabases().get("test-h2");

    // H2 default port is 0
    assertThat(config.getEffectivePort()).isEqualTo(0);
  }

  @Test
  void poolConfig_hasDefaults() {
    PoolConfig pool = new PoolConfig();

    assertThat(pool.getMaximumPoolSize()).isEqualTo(10);
    assertThat(pool.getMinimumIdle()).isEqualTo(2);
    assertThat(pool.getConnectionTimeout()).isEqualTo(30000);
    assertThat(pool.getIdleTimeout()).isEqualTo(600000);
    assertThat(pool.getMaxLifetime()).isEqualTo(1800000);
  }
}
