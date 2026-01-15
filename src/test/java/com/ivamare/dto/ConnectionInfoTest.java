package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.domain.DatabaseType;
import org.junit.jupiter.api.Test;

/** Tests for ConnectionInfo. */
class ConnectionInfoTest {

  @Test
  void from_createsCorrectConnectionInfo() {
    ConnectionConfig config = new ConnectionConfig();
    config.setName("Test Connection");
    config.setType(DatabaseType.POSTGRES);
    config.setHost("localhost");
    config.setPort(5432);
    config.setDatabase("testdb");
    config.setSchema("public");

    ConnectionInfo info = ConnectionInfo.from("test-conn", config);

    assertThat(info.getId()).isEqualTo("test-conn");
    assertThat(info.getName()).isEqualTo("Test Connection");
    assertThat(info.getType()).isEqualTo(DatabaseType.POSTGRES);
    assertThat(info.getHost()).isEqualTo("localhost");
    assertThat(info.getPort()).isEqualTo(5432);
    assertThat(info.getDatabase()).isEqualTo("testdb");
    assertThat(info.getSchema()).isEqualTo("public");
    assertThat(info.isConnected()).isFalse();
  }

  @Test
  void from_usesDefaultPort_whenNotSpecified() {
    ConnectionConfig config = new ConnectionConfig();
    config.setName("Test Connection");
    config.setType(DatabaseType.SQLSERVER);
    config.setHost("localhost");
    config.setDatabase("testdb");
    // Port not set - should use default

    ConnectionInfo info = ConnectionInfo.from("test-conn", config);

    assertThat(info.getPort()).isEqualTo(1433); // SQLSERVER default port
  }
}
