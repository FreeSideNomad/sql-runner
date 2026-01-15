package com.ivamare;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using TestContainers. Provides SQL Server, PostgreSQL, and DB2
 * containers with test schemas.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  static MSSQLServerContainer<?> sqlServer =
      new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
          .acceptLicense()
          .withInitScript("test-data/sqlserver-init.sql");

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16").withInitScript("test-data/postgres-init.sql");

  @Container
  static Db2Container db2 =
      new Db2Container("icr.io/db2_community/db2:latest")
          .acceptLicense()
          .withInitScript("test-data/db2-init.sql");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // SQL Server properties
    registry.add("sqlserver.jdbc-url", sqlServer::getJdbcUrl);
    registry.add("sqlserver.username", sqlServer::getUsername);
    registry.add("sqlserver.password", sqlServer::getPassword);

    // PostgreSQL properties
    registry.add("postgres.jdbc-url", postgres::getJdbcUrl);
    registry.add("postgres.username", postgres::getUsername);
    registry.add("postgres.password", postgres::getPassword);

    // DB2 properties
    registry.add("db2.jdbc-url", db2::getJdbcUrl);
    registry.add("db2.username", db2::getUsername);
    registry.add("db2.password", db2::getPassword);
  }
}
