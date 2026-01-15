package com.ivamare.dto;

import com.ivamare.domain.ParameterType;
import com.ivamare.domain.UpdateBindingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for structured query configuration form input. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryConfigFormDto {

  private String sql;
  private String selectSql;
  private String updateSql;
  private String updateBindingMode;
  private String primaryKeyColumn;
  private String backupColumns;
  private String rollbackColumns;
  private Integer timeoutSeconds;
  private Integer maxRows;

  @Builder.Default private List<ParameterFormDto> parameters = new ArrayList<>();

  /** Parameter configuration form DTO. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ParameterFormDto {
    private String name;
    private String label;
    private ParameterType dataType;
    private boolean required;
    private String defaultValue;
    private String validation;
    private String listSeparator;
    private String enumValues;
  }

  /** Create from QueryConfig parsed from YAML. */
  public static QueryConfigFormDto from(QueryConfig config) {
    if (config == null) {
      return QueryConfigFormDto.builder().build();
    }

    List<ParameterFormDto> params = new ArrayList<>();
    if (config.getParameters() != null) {
      for (QueryConfig.ParameterConfig p : config.getParameters()) {
        StringBuilder enumVals = new StringBuilder();
        if (p.getEnumValues() != null) {
          for (QueryConfig.EnumValue ev : p.getEnumValues()) {
            if (enumVals.length() > 0) {
              enumVals.append("\n");
            }
            if (ev.getDescription() != null && !ev.getDescription().isEmpty()) {
              enumVals.append(ev.getValue()).append(":").append(ev.getDescription());
            } else {
              enumVals.append(ev.getValue());
            }
          }
        }

        params.add(
            ParameterFormDto.builder()
                .name(p.getName())
                .label(p.getLabel())
                .dataType(parseDataType(p.getDataType()))
                .required(p.isRequired())
                .defaultValue(p.getDefaultValue())
                .validation(p.getValidation())
                .listSeparator(p.getListSeparator())
                .enumValues(enumVals.toString())
                .build());
      }
    }

    String backupCols =
        config.getBackupColumns() != null ? String.join(", ", config.getBackupColumns()) : null;
    String rollbackCols =
        config.getRollbackColumns() != null ? String.join(", ", config.getRollbackColumns()) : null;

    return QueryConfigFormDto.builder()
        .sql(config.getSql())
        .selectSql(config.getSelectSql())
        .updateSql(config.getUpdateSql())
        .updateBindingMode(
            config.getUpdateBindingMode() != null ? config.getUpdateBindingMode().name() : null)
        .primaryKeyColumn(config.getPrimaryKeyColumn())
        .backupColumns(backupCols)
        .rollbackColumns(rollbackCols)
        .timeoutSeconds(config.getTimeoutSeconds())
        .maxRows(config.getMaxRows())
        .parameters(params)
        .build();
  }

  private static ParameterType parseDataType(String dataType) {
    if (dataType == null || dataType.isEmpty()) {
      return ParameterType.STRING;
    }
    try {
      return ParameterType.valueOf(dataType.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ParameterType.STRING;
    }
  }

  /** Convert to QueryConfig for YAML serialization. */
  public QueryConfig toQueryConfig() {
    List<QueryConfig.ParameterConfig> configParams = new ArrayList<>();
    if (parameters != null) {
      for (ParameterFormDto p : parameters) {
        if (p.getName() == null || p.getName().isBlank()) {
          continue;
        }

        List<QueryConfig.EnumValue> enumVals = null;
        if (p.getEnumValues() != null && !p.getEnumValues().isBlank()) {
          enumVals = new ArrayList<>();
          for (String line : p.getEnumValues().split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
              enumVals.add(
                  QueryConfig.EnumValue.builder()
                      .value(line.substring(0, colonIdx).trim())
                      .description(line.substring(colonIdx + 1).trim())
                      .build());
            } else {
              enumVals.add(QueryConfig.EnumValue.builder().value(line).build());
            }
          }
        }

        configParams.add(
            QueryConfig.ParameterConfig.builder()
                .name(p.getName())
                .label(p.getLabel())
                .dataType(p.getDataType() != null ? p.getDataType().name() : "STRING")
                .required(p.isRequired())
                .defaultValue(p.getDefaultValue())
                .validation(p.getValidation())
                .listSeparator(p.getListSeparator())
                .enumValues(enumVals)
                .build());
      }
    }

    List<String> backupColList = parseColumnList(backupColumns);
    List<String> rollbackColList = parseColumnList(rollbackColumns);

    return QueryConfig.builder()
        .sql(sql)
        .selectSql(selectSql)
        .updateSql(updateSql)
        .updateBindingMode(parseUpdateBindingMode(updateBindingMode))
        .primaryKeyColumn(primaryKeyColumn)
        .backupColumns(backupColList)
        .rollbackColumns(rollbackColList)
        .timeoutSeconds(timeoutSeconds)
        .maxRows(maxRows)
        .parameters(configParams.isEmpty() ? null : configParams)
        .build();
  }

  private UpdateBindingMode parseUpdateBindingMode(String mode) {
    if (mode == null || mode.isBlank()) {
      return null;
    }
    try {
      return UpdateBindingMode.valueOf(mode);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private List<String> parseColumnList(String columns) {
    if (columns == null || columns.isBlank()) {
      return null;
    }
    List<String> result = new ArrayList<>();
    for (String col : columns.split(",")) {
      col = col.trim();
      if (!col.isEmpty()) {
        result.add(col);
      }
    }
    return result.isEmpty() ? null : result;
  }
}
