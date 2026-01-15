package com.ivamare.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests for DatabaseType enum. */
class DatabaseTypeTest {

  @Test
  void sqlServer_hasCorrectDriverAndValidationQuery() {
    assertThat(DatabaseType.SQLSERVER.getDriverClass())
        .isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    assertThat(DatabaseType.SQLSERVER.getValidationQuery()).isEqualTo("SELECT 1");
    assertThat(DatabaseType.SQLSERVER.getDefaultPort()).isEqualTo(1433);
  }

  @Test
  void db2_hasCorrectDriverAndValidationQuery() {
    assertThat(DatabaseType.DB2.getDriverClass()).isEqualTo("com.ibm.db2.jcc.DB2Driver");
    assertThat(DatabaseType.DB2.getValidationQuery()).isEqualTo("SELECT 1 FROM SYSIBM.SYSDUMMY1");
    assertThat(DatabaseType.DB2.getDefaultPort()).isEqualTo(50000);
  }

  @Test
  void postgres_hasCorrectDriverAndValidationQuery() {
    assertThat(DatabaseType.POSTGRES.getDriverClass()).isEqualTo("org.postgresql.Driver");
    assertThat(DatabaseType.POSTGRES.getValidationQuery()).isEqualTo("SELECT 1");
    assertThat(DatabaseType.POSTGRES.getDefaultPort()).isEqualTo(5432);
  }

  @Test
  void h2_hasCorrectDriverAndValidationQuery() {
    assertThat(DatabaseType.H2.getDriverClass()).isEqualTo("org.h2.Driver");
    assertThat(DatabaseType.H2.getValidationQuery()).isEqualTo("SELECT 1");
    assertThat(DatabaseType.H2.getDefaultPort()).isEqualTo(0);
  }

  @Test
  void buildJdbcUrl_sqlServer_buildsCorrectUrl() {
    String url = DatabaseType.SQLSERVER.buildJdbcUrl("server.example.com", 1433, "mydb", null);
    assertThat(url)
        .isEqualTo(
            "jdbc:sqlserver://server.example.com:1433;databaseName=mydb;encrypt=true;trustServerCertificate=true");
  }

  @Test
  void buildJdbcUrl_db2_buildsCorrectUrl() {
    String url = DatabaseType.DB2.buildJdbcUrl("db2.example.com", 50000, "mydb", null);
    assertThat(url).isEqualTo("jdbc:db2://db2.example.com:50000/mydb");
  }

  @Test
  void buildJdbcUrl_db2_withSchema_includesSchema() {
    String url = DatabaseType.DB2.buildJdbcUrl("db2.example.com", 50000, "mydb", "MYSCHEMA");
    assertThat(url).isEqualTo("jdbc:db2://db2.example.com:50000/mydb:currentSchema=MYSCHEMA;");
  }

  @Test
  void buildJdbcUrl_postgres_buildsCorrectUrl() {
    String url = DatabaseType.POSTGRES.buildJdbcUrl("pg.example.com", 5432, "mydb", null);
    assertThat(url).isEqualTo("jdbc:postgresql://pg.example.com:5432/mydb");
  }

  @Test
  void buildJdbcUrl_postgres_withSchema_includesSchema() {
    String url = DatabaseType.POSTGRES.buildJdbcUrl("pg.example.com", 5432, "mydb", "public");
    assertThat(url).isEqualTo("jdbc:postgresql://pg.example.com:5432/mydb?currentSchema=public");
  }

  @Test
  void buildJdbcUrl_h2_buildsCorrectUrl() {
    String url = DatabaseType.H2.buildJdbcUrl("localhost", 0, "testdb", null);
    assertThat(url).isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
  }

  @ParameterizedTest
  @EnumSource(DatabaseType.class)
  void allTypes_haveNonNullDriverClass(DatabaseType type) {
    assertThat(type.getDriverClass()).isNotNull().isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(DatabaseType.class)
  void allTypes_haveNonNullValidationQuery(DatabaseType type) {
    assertThat(type.getValidationQuery()).isNotNull().isNotEmpty();
  }
}
