package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivamare.dto.ConnectionInfo;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Tests for ConnectionRegistry service. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "sqlrunner.connections.databases.test-h2.name=Test H2 Database",
      "sqlrunner.connections.databases.test-h2.type=H2",
      "sqlrunner.connections.databases.test-h2.host=localhost",
      "sqlrunner.connections.databases.test-h2.database=connectiontest"
      // No credential-prefix for H2 - uses empty credentials
    })
class ConnectionRegistryTest {

  @Autowired private ConnectionRegistry connectionRegistry;

  @Test
  void getDataSource_returnsDataSourceForConfiguredConnection() throws Exception {
    DataSource dataSource = connectionRegistry.getDataSource("test-h2");

    assertThat(dataSource).isNotNull();

    // Verify we can get a connection
    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn).isNotNull();
      assertThat(conn.isClosed()).isFalse();
    }
  }

  @Test
  void getDataSource_throwsForUnknownConnection() {
    assertThatThrownBy(() -> connectionRegistry.getDataSource("unknown-connection"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown connection: unknown-connection");
  }

  @Test
  void getDataSource_returnsSameInstanceOnSubsequentCalls() {
    DataSource ds1 = connectionRegistry.getDataSource("test-h2");
    DataSource ds2 = connectionRegistry.getDataSource("test-h2");

    assertThat(ds1).isSameAs(ds2);
  }

  @Test
  void listConnections_returnsAllConfiguredConnections() {
    List<ConnectionInfo> connections = connectionRegistry.listConnections();

    assertThat(connections).isNotEmpty();
    assertThat(connections.stream().map(ConnectionInfo::getId)).contains("test-h2");
  }

  @Test
  void listConnections_includesConnectionDetails() {
    List<ConnectionInfo> connections = connectionRegistry.listConnections();
    ConnectionInfo testH2 =
        connections.stream().filter(c -> "test-h2".equals(c.getId())).findFirst().orElseThrow();

    assertThat(testH2.getName()).isEqualTo("Test H2 Database");
    assertThat(testH2.getHost()).isEqualTo("localhost");
    assertThat(testH2.getDatabase()).isEqualTo("connectiontest");
  }

  @Test
  void testConnection_returnsSuccessForValidConnection() {
    ConnectionInfo result = connectionRegistry.testConnection("test-h2");

    assertThat(result.isConnected()).isTrue();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  void testConnection_returnsErrorForUnknownConnection() {
    ConnectionInfo result = connectionRegistry.testConnection("unknown");

    assertThat(result.isConnected()).isFalse();
    assertThat(result.getErrorMessage()).contains("Unknown connection");
  }

  @Test
  void hasConnection_returnsTrueForConfiguredConnection() {
    assertThat(connectionRegistry.hasConnection("test-h2")).isTrue();
  }

  @Test
  void hasConnection_returnsFalseForUnconfiguredConnection() {
    assertThat(connectionRegistry.hasConnection("not-configured")).isFalse();
  }

  @Test
  void getConnectionConfig_returnsConfigForConfiguredConnection() {
    var config = connectionRegistry.getConnectionConfig("test-h2");

    assertThat(config).isNotNull();
    assertThat(config.getName()).isEqualTo("Test H2 Database");
  }

  @Test
  void getConnectionConfig_returnsNullForUnconfiguredConnection() {
    var config = connectionRegistry.getConnectionConfig("unknown");

    assertThat(config).isNull();
  }
}
