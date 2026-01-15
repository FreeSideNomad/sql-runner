package com.ivamare.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Parsed query configuration from YAML. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryConfig {

  private String sql;
  private String selectSql;
  private String updateSql;
  private List<ParameterConfig> parameters;
  private String primaryKeyColumn;
  private List<String> backupColumns;
  private List<String> rollbackColumns;
  private Integer timeoutSeconds;
  private Integer maxRows;

  /** Get the SQL to execute for SELECT queries. */
  public String getExecutableSql() {
    return sql != null ? sql : selectSql;
  }

  /** Parameter configuration. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ParameterConfig {
    private String name;
    private String label;
    private String dataType;
    private boolean required;
    private String defaultValue;
    private String validation;
    private String listSeparator;
    private List<EnumValue> enumValues;

    /** Get display label or fall back to name. */
    public String getDisplayLabel() {
      return label != null && !label.isEmpty() ? label : name;
    }
  }

  /** Enum value configuration. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class EnumValue {
    private String value;
    private String description;
  }
}
