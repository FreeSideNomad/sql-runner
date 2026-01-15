package com.ivamare.testdata;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/**
 * Test runner for generating test data in local Docker databases.
 *
 * <p>Run via Maven: ./mvnw test -Dtest=TestDataGeneratorRunner#generatePostgres -Dscale=1
 *
 * <p>Environment variables required: - LOCAL_POSTGRES_USER / LOCAL_POSTGRES_PASSWORD -
 * LOCAL_SQLSERVER_USER / LOCAL_SQLSERVER_PASSWORD - LOCAL_DB2_USER / LOCAL_DB2_PASSWORD
 *
 * <p>Scale parameter = number of customers (default: 10000)
 *
 * <p>Not run automatically in CI - requires database environment variables.
 */
public class TestDataGeneratorRunner {

  private int getScale() {
    String scaleStr = System.getProperty("scale", System.getenv().getOrDefault("SCALE", "100"));
    return Integer.parseInt(scaleStr);
  }

  @Test
  void generatePostgres() throws Exception {
    String user = System.getenv("LOCAL_POSTGRES_USER");
    String password = System.getenv("LOCAL_POSTGRES_PASSWORD");

    assumeTrue(
        user != null && password != null,
        "Skipping: Set LOCAL_POSTGRES_USER and LOCAL_POSTGRES_PASSWORD environment variables");

    int scale = getScale();
    System.out.println("Generating PostgreSQL data with scale=" + scale);

    DataSource ds =
        createDataSource(
            "jdbc:postgresql://localhost:5432/sqlrunner", user, password, "org.postgresql.Driver");

    TestDataGenerator generator = new TestDataGenerator(ds, scale);
    generator.clearAll();
    generator.generateAll();

    ((HikariDataSource) ds).close();
  }

  @Test
  void generateSqlserver() throws Exception {
    String user = System.getenv("LOCAL_SQLSERVER_USER");
    String password = System.getenv("LOCAL_SQLSERVER_PASSWORD");

    assumeTrue(
        user != null && password != null,
        "Skipping: Set LOCAL_SQLSERVER_USER and LOCAL_SQLSERVER_PASSWORD environment variables");

    int scale = getScale();
    System.out.println("Generating SQL Server data with scale=" + scale);

    DataSource ds =
        createDataSource(
            "jdbc:sqlserver://localhost:1433;databaseName=sqlrunner;encrypt=true;trustServerCertificate=true",
            user,
            password,
            "com.microsoft.sqlserver.jdbc.SQLServerDriver");

    TestDataGenerator generator = new TestDataGenerator(ds, scale);
    generator.clearAll();
    generator.generateAll();

    ((HikariDataSource) ds).close();
  }

  @Test
  void generateDb2() throws Exception {
    String user = System.getenv("LOCAL_DB2_USER");
    String password = System.getenv("LOCAL_DB2_PASSWORD");

    assumeTrue(
        user != null && password != null,
        "Skipping: Set LOCAL_DB2_USER and LOCAL_DB2_PASSWORD environment variables");

    int scale = getScale();
    System.out.println("Generating DB2 data with scale=" + scale);

    DataSource ds =
        createDataSource(
            "jdbc:db2://localhost:50000/TESTDB", user, password, "com.ibm.db2.jcc.DB2Driver");

    TestDataGenerator generator = new TestDataGenerator(ds, scale);
    generator.clearAll();
    generator.generateAll();

    ((HikariDataSource) ds).close();
  }

  @Test
  void generateAll() throws Exception {
    System.out.println("Generating test data for all databases...");

    try {
      generatePostgres();
      System.out.println("PostgreSQL done.");
    } catch (Exception e) {
      System.err.println("PostgreSQL failed: " + e.getMessage());
    }

    try {
      generateSqlserver();
      System.out.println("SQL Server done.");
    } catch (Exception e) {
      System.err.println("SQL Server failed: " + e.getMessage());
    }

    try {
      generateDb2();
      System.out.println("DB2 done.");
    } catch (Exception e) {
      System.err.println("DB2 failed: " + e.getMessage());
    }

    System.out.println("All databases processed.");
  }

  private DataSource createDataSource(
      String url, String user, String password, String driverClass) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    config.setDriverClassName(driverClass);
    config.setMaximumPoolSize(5);
    config.setConnectionTimeout(30000);
    return new HikariDataSource(config);
  }
}
