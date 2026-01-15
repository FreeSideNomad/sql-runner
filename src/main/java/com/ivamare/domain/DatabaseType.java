package com.ivamare.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Supported database types with their driver classes and validation queries. */
@Getter
@RequiredArgsConstructor
public enum DatabaseType {
  SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver", "SELECT 1"),
  DB2("com.ibm.db2.jcc.DB2Driver", "SELECT 1 FROM SYSIBM.SYSDUMMY1"),
  POSTGRES("org.postgresql.Driver", "SELECT 1"),
  H2("org.h2.Driver", "SELECT 1");

  private final String driverClass;
  private final String validationQuery;

  /** Build JDBC URL for this database type. */
  public String buildJdbcUrl(String host, int port, String database, String schema) {
    return switch (this) {
      case SQLSERVER ->
          String.format(
              "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
              host, port, database);
      case DB2 ->
          String.format(
              "jdbc:db2://%s:%d/%s%s",
              host, port, database, schema != null ? ":currentSchema=" + schema + ";" : "");
      case POSTGRES ->
          String.format(
              "jdbc:postgresql://%s:%d/%s%s",
              host, port, database, schema != null ? "?currentSchema=" + schema : "");
      case H2 -> String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", database);
    };
  }

  /** Get default port for this database type. */
  public int getDefaultPort() {
    return switch (this) {
      case SQLSERVER -> 1433;
      case DB2 -> 50000;
      case POSTGRES -> 5432;
      case H2 -> 0;
    };
  }
}
