package com.ivamare.service;

import com.ivamare.domain.DatabaseType;
import com.ivamare.domain.UpdateBindingMode;
import com.ivamare.dto.QueryConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service for generating transaction-controlled SQL scripts for manual execution. */
@Service
@RequiredArgsConstructor
public class SqlScriptGenerator {

  private final UpdateParameterAnalyzer parameterAnalyzer;

  /**
   * Generate a full SQL script with transaction control.
   *
   * @param config Query configuration
   * @param params User-provided parameters
   * @param previewData Preview result rows
   * @param dbType Target database type
   * @return The complete SQL script
   */
  public String generateScript(
      QueryConfig config,
      Map<String, Object> params,
      List<Map<String, Object>> previewData,
      DatabaseType dbType) {

    StringBuilder script = new StringBuilder();
    addTransactionStart(script, dbType);

    UpdateBindingMode mode =
        config.getUpdateBindingMode() != null
            ? config.getUpdateBindingMode()
            : UpdateBindingMode.STANDARD;

    try {
      switch (mode) {
        case BATCH -> generateBatchScript(script, config, params, previewData, dbType);
        case ROW_BY_ROW -> generateRowByRowScript(script, config, params, previewData, dbType);
        case STANDARD -> generateStandardScript(script, config, params, dbType);
      }
      addTransactionCommit(script, dbType);
    } catch (Exception e) {
      // In case of error during generation, we don't want to return a broken script
      // However, the transaction block is already started.
      // For now, we propagate the exception.
      throw new RuntimeException("Failed to generate SQL script: " + e.getMessage(), e);
    }

    return script.toString();
  }

  private void addTransactionStart(StringBuilder script, DatabaseType dbType) {
    switch (dbType) {
      case SQLSERVER -> {
        script.append("BEGIN TRY\n");
        script.append("    BEGIN TRANSACTION;\n\n");
      }
      case POSTGRES, DB2 -> script.append("BEGIN;\n\n");
      case H2 -> script.append("-- Transaction start (H2 handles this implicitly in sessions)\n\n");
    }
  }

  private void addTransactionCommit(StringBuilder script, DatabaseType dbType) {

    switch (dbType) {
      case SQLSERVER -> {
        script.append("\n    COMMIT TRANSACTION;\n");

        script.append("END TRY\n");

        script.append("BEGIN CATCH\n");

        script.append("    IF @@TRANCOUNT > 0\n");

        script.append("        ROLLBACK TRANSACTION;\n");

        script.append("    \n");

        script.append("    DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();\n");

        script.append("    DECLARE @ErrorSeverity INT = ERROR_SEVERITY();\n");

        script.append("    DECLARE @ErrorState INT = ERROR_STATE();\n");

        script.append("    \n");

        script.append("    RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);\n");

        script.append("END CATCH\n");
      }
      case POSTGRES, DB2 -> {
        script.append("\nCOMMIT;\n");
        script.append("-- Note: In case of error, execute ROLLBACK;\n");
      }
      case H2 -> script.append("\nCOMMIT;\n");
    }
  }

  private void generateStandardScript(
      StringBuilder script, QueryConfig config, Map<String, Object> params, DatabaseType dbType) {
    script.append("-- Standard Update (Single Execution)\n");
    String resolvedSql = resolveParams(config.getUpdateSql(), params, dbType);
    script.append(resolvedSql).append(";\n");
  }

  private void generateBatchScript(
      StringBuilder script,
      QueryConfig config,
      Map<String, Object> params,
      List<Map<String, Object>> previewData,
      DatabaseType dbType) {

    script.append("-- Batch Update\n");

    String pkColumn = config.getPrimaryKeyColumn();
    List<Object> idList =
        previewData.stream()
            .map(row -> getColumnValueCaseInsensitive(row, pkColumn))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (idList.isEmpty()) {
      script.append("-- No rows to update (empty preview)\n");
      return;
    }

    // Special handling for :id_list
    Map<String, Object> scriptParams = new java.util.HashMap<>(params);
    scriptParams.put("id_list", idList);

    String resolvedSql = resolveParams(config.getUpdateSql(), scriptParams, dbType);
    script.append(resolvedSql).append(";\n");
  }

  private void generateRowByRowScript(
      StringBuilder script,
      QueryConfig config,
      Map<String, Object> params,
      List<Map<String, Object>> previewData,
      DatabaseType dbType) {

    script.append("-- Row-by-Row Update (").append(previewData.size()).append(" rows)\n");

    Set<String> selectColumns = previewData.isEmpty() ? Set.of() : previewData.get(0).keySet();
    UpdateParameterAnalyzer.ParameterBindings bindings =
        parameterAnalyzer.analyzeBindings(
            config.getUpdateSql(), selectColumns, config.getParameters());

    for (int i = 0; i < previewData.size(); i++) {
      Map<String, Object> row = previewData.get(i);
      Map<String, Object> rowParams = new java.util.HashMap<>(params);

      for (String colParam : bindings.getColumnBoundParams()) {
        Object value = getColumnValueCaseInsensitive(row, colParam);
        rowParams.put(colParam, value);
      }

      String resolvedSql = resolveParams(config.getUpdateSql(), rowParams, dbType);
      script.append(resolvedSql).append(";\n");
    }
  }

  /**
   * Replace named parameters like :param with actual values. WARNING: This is a basic
   * implementation. For production, consider using a robust SQL parser/builder.
   */
  private String resolveParams(String sql, Map<String, Object> params, DatabaseType dbType) {
    String resolved = sql;
    // Sort keys by length descending to avoid partial replacements (e.g. :param1 vs :param10)
    List<String> sortedKeys =
        params.keySet().stream()
            .sorted((a, b) -> Integer.compare(b.length(), a.length()))
            .collect(Collectors.toList());

    for (String key : sortedKeys) {
      Object value = params.get(key);
      String stringValue = formatValue(value, dbType);
      // Regex to match :paramName not followed by alphanumeric chars
      resolved = resolved.replaceAll(":" + key + "\\b", stringValue);
    }
    return resolved;
  }

  private String formatValue(Object value, DatabaseType dbType) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof Number) {
      return value.toString();
    }
    if (value instanceof Boolean) {
      // Some DBs don't support TRUE/FALSE literals
      if (dbType == DatabaseType.SQLSERVER || dbType == DatabaseType.DB2) {
        return ((Boolean) value) ? "1" : "0";
      }
      return value.toString().toUpperCase();
    }
    if (value instanceof List<?> list) {
      return list.stream().map(item -> formatValue(item, dbType)).collect(Collectors.joining(", "));
    }
    // String, Date, etc. - wrap in quotes and escape
    String str = value.toString();
    // Basic SQL escaping (replace ' with '')
    return "'" + str.replace("'", "''") + "'";
  }

  private Object getColumnValueCaseInsensitive(Map<String, Object> row, String columnName) {
    if (row.containsKey(columnName)) {
      return row.get(columnName);
    }
    for (Map.Entry<String, Object> entry : row.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(columnName)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
