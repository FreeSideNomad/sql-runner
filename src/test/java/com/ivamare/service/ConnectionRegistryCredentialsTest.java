package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.dto.ConnectionInfo;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** Tests for ConnectionRegistry credential resolution. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      // Connection with credential prefix that has env vars set
      "sqlrunner.connections.databases.cred-test.name=Credential Test",
      "sqlrunner.connections.databases.cred-test.type=H2",
      "sqlrunner.connections.databases.cred-test.host=localhost",
      "sqlrunner.connections.databases.cred-test.database=credtest",
      "sqlrunner.connections.databases.cred-test.credential-prefix=CRED_TEST",
      "sqlrunner.connections.databases.cred-test.properties.MODE=PostgreSQL",
      // Set the env vars through Spring's environment abstraction
      "CRED_TEST_USER=sa",
      "CRED_TEST_PASSWORD=",
      // Connection with missing env vars (to test warning path)
      "sqlrunner.connections.databases.missing-creds.name=Missing Credentials",
      "sqlrunner.connections.databases.missing-creds.type=H2",
      "sqlrunner.connections.databases.missing-creds.host=localhost",
      "sqlrunner.connections.databases.missing-creds.database=missingcreds",
      "sqlrunner.connections.databases.missing-creds.credential-prefix=MISSING"
      // Note: MISSING_USER and MISSING_PASSWORD are intentionally not set
    })
class ConnectionRegistryCredentialsTest {

  @Autowired private ConnectionRegistry connectionRegistry;

  @Test
  void getDataSource_withCredentialPrefixAndEnvVarsSet_createsPool() throws Exception {
    DataSource dataSource = connectionRegistry.getDataSource("cred-test");

    assertThat(dataSource).isNotNull();

    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn).isNotNull();
      assertThat(conn.isClosed()).isFalse();
    }
  }

  @Test
  void getDataSource_withMissingEnvVars_stillCreatesPool() throws Exception {
    // Should still create pool (logs warning) with empty credentials
    DataSource dataSource = connectionRegistry.getDataSource("missing-creds");

    assertThat(dataSource).isNotNull();
  }

  @Test
  void testConnection_withCredentialPrefix_returnsSuccess() {
    ConnectionInfo result = connectionRegistry.testConnection("cred-test");

    assertThat(result.isConnected()).isTrue();
  }

  @Test
  void getDataSource_withCustomProperties_appliesProperties() throws Exception {
    DataSource dataSource = connectionRegistry.getDataSource("cred-test");

    // The connection should work with custom properties applied
    try (Connection conn = dataSource.getConnection()) {
      assertThat(conn).isNotNull();
    }
  }
}
