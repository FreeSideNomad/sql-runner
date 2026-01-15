package com.ivamare.service;

import com.ivamare.domain.UpdateBindingMode;
import com.ivamare.dto.QueryConfig;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

/** Service for analyzing UPDATE SQL parameters and detecting binding modes. */
@Service
public class UpdateParameterAnalyzer {

  private static final Pattern PARAM_PATTERN = Pattern.compile(":(\\w+)");
  private static final Pattern SELECT_COLUMNS_PATTERN =
      Pattern.compile("SELECT\\s+(.+?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /**
   * Extract all named parameters from SQL.
   *
   * @param sql SQL statement
   * @return Set of parameter names (lowercase)
   */
  public Set<String> extractParameters(String sql) {
    if (sql == null || sql.isBlank()) {
      return Set.of();
    }

    Set<String> params = new LinkedHashSet<>();
    Matcher matcher = PARAM_PATTERN.matcher(sql);
    while (matcher.find()) {
      params.add(matcher.group(1).toLowerCase());
    }
    return params;
  }

  /**
   * Extract column names from SELECT clause.
   *
   * @param selectSql SELECT SQL statement
   * @return Set of column names (lowercase)
   */
  public Set<String> extractSelectColumns(String selectSql) {
    if (selectSql == null || selectSql.isBlank()) {
      return Set.of();
    }

    Matcher matcher = SELECT_COLUMNS_PATTERN.matcher(selectSql);
    if (!matcher.find()) {
      return Set.of();
    }

    String columnsPart = matcher.group(1);
    Set<String> columns = new LinkedHashSet<>();

    // Handle SELECT * case
    if (columnsPart.trim().equals("*")) {
      return Set.of();
    }

    // Split by comma and extract column names
    for (String part : columnsPart.split(",")) {
      String col = extractColumnName(part.trim());
      if (col != null && !col.isEmpty()) {
        columns.add(col.toLowerCase());
      }
    }

    return columns;
  }

  /**
   * Extract column name from a SELECT column expression.
   *
   * <p>Handles: "column", "table.column", "column AS alias", "expression AS alias"
   */
  private String extractColumnName(String columnExpr) {
    if (columnExpr.isEmpty()) {
      return null;
    }

    // Handle AS alias - use the alias as the column name
    String upperExpr = columnExpr.toUpperCase();
    int asIndex = upperExpr.lastIndexOf(" AS ");
    if (asIndex > 0) {
      return columnExpr.substring(asIndex + 4).trim();
    }

    // Handle table.column - use just the column
    int dotIndex = columnExpr.lastIndexOf('.');
    if (dotIndex > 0) {
      return columnExpr.substring(dotIndex + 1).trim();
    }

    // Handle spaces (could be "column alias" without AS keyword)
    String[] parts = columnExpr.split("\\s+");
    if (parts.length > 1) {
      return parts[parts.length - 1];
    }

    return columnExpr;
  }

  /**
   * Detect the binding mode based on updateSql and SELECT columns.
   *
   * @param updateSql UPDATE SQL statement
   * @param selectColumns Column names from SELECT result
   * @return Detected UpdateBindingMode
   */
  public UpdateBindingMode detectBindingMode(String updateSql, Set<String> selectColumns) {
    Set<String> updateParams = extractParameters(updateSql);

    // Check for :id_list parameter
    if (updateParams.contains("id_list")) {
      return UpdateBindingMode.BATCH;
    }

    // Check if any UPDATE parameter matches SELECT columns
    Set<String> lowerColumns =
        selectColumns.stream().map(String::toLowerCase).collect(Collectors.toSet());

    for (String param : updateParams) {
      if (lowerColumns.contains(param)) {
        return UpdateBindingMode.ROW_BY_ROW;
      }
    }

    return UpdateBindingMode.STANDARD;
  }

  /**
   * Analyze and separate parameters into column-bound and user-bound groups.
   *
   * @param updateSql UPDATE SQL statement
   * @param selectColumns Column names from SELECT result
   * @param configuredParams Parameters defined in query config
   * @return ParameterBindings with separated parameter groups
   */
  public ParameterBindings analyzeBindings(
      String updateSql,
      Set<String> selectColumns,
      List<QueryConfig.ParameterConfig> configuredParams) {

    Set<String> updateParams = extractParameters(updateSql);
    Set<String> lowerColumns =
        selectColumns.stream().map(String::toLowerCase).collect(Collectors.toSet());
    Set<String> configParamNames =
        configuredParams != null
            ? configuredParams.stream()
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.toSet())
            : Set.of();

    Set<String> columnBound = new LinkedHashSet<>();
    Set<String> userBound = new LinkedHashSet<>();

    for (String param : updateParams) {
      if (param.equals("id_list")) {
        // Special parameter, not column or user bound
        continue;
      }
      if (lowerColumns.contains(param)) {
        columnBound.add(param);
      } else if (configParamNames.contains(param)) {
        userBound.add(param);
      }
    }

    return new ParameterBindings(columnBound, userBound);
  }

  /** Result of parameter binding analysis. */
  @Data
  @AllArgsConstructor
  public static class ParameterBindings {
    /** Parameters bound from SELECT result columns. */
    private Set<String> columnBoundParams;

    /** Parameters bound from user form input. */
    private Set<String> userBoundParams;
  }
}
