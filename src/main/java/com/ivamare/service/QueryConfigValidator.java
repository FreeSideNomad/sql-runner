package com.ivamare.service;

import com.ivamare.domain.QueryType;
import com.ivamare.domain.UpdateBindingMode;
import com.ivamare.dto.QueryConfigFormDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service for validating query configuration, especially UPDATE_WORKFLOW binding modes. */
@Service
@RequiredArgsConstructor
public class QueryConfigValidator {

  private final UpdateParameterAnalyzer analyzer;

  /**
   * Validate UPDATE SQL parameters based on selected binding mode.
   *
   * @param config Query configuration form DTO
   * @param queryType Query type
   * @return ValidationResult with errors and warnings
   */
  public ValidationResult validateUpdateConfig(QueryConfigFormDto config, QueryType queryType) {
    ValidationResult result = new ValidationResult();

    if (queryType != QueryType.UPDATE_WORKFLOW) {
      return result;
    }

    if (config.getUpdateBindingMode() == null) {
      result.addError("Update binding mode is required for UPDATE_WORKFLOW queries");
      return result;
    }

    UpdateBindingMode mode;
    try {
      mode = UpdateBindingMode.valueOf(config.getUpdateBindingMode());
    } catch (IllegalArgumentException e) {
      result.addError("Invalid update binding mode: " + config.getUpdateBindingMode());
      return result;
    }

    Set<String> updateParams = analyzer.extractParameters(config.getUpdateSql());
    Set<String> selectColumns = analyzer.extractSelectColumns(config.getSelectSql());
    Set<String> userParams =
        config.getParameters() != null
            ? config.getParameters().stream()
                .filter(p -> p.getName() != null && !p.getName().isBlank())
                .map(p -> p.getName().toLowerCase())
                .collect(Collectors.toSet())
            : Set.of();

    switch (mode) {
      case BATCH -> validateBatchMode(config, updateParams, result);
      case ROW_BY_ROW -> validateRowByRowMode(config, updateParams, selectColumns, result);
      case STANDARD -> validateStandardMode(updateParams, userParams, result);
    }

    return result;
  }

  private void validateBatchMode(
      QueryConfigFormDto config, Set<String> updateParams, ValidationResult result) {

    if (!updateParams.contains("id_list")) {
      result.addError("Batch mode requires :id_list parameter in UPDATE SQL");
    }

    if (config.getPrimaryKeyColumn() == null || config.getPrimaryKeyColumn().isBlank()) {
      result.addError("Batch mode requires Primary Key Column to be defined");
    }
  }

  private void validateRowByRowMode(
      QueryConfigFormDto config,
      Set<String> updateParams,
      Set<String> selectColumns,
      ValidationResult result) {

    // Check if any UPDATE parameter matches SELECT columns
    Set<String> lowerColumns =
        selectColumns.stream().map(String::toLowerCase).collect(Collectors.toSet());

    Set<String> columnParams =
        updateParams.stream().filter(lowerColumns::contains).collect(Collectors.toSet());

    if (columnParams.isEmpty()) {
      result.addError(
          "Row-by-row mode requires at least one column parameter (e.g., :id, :name) in UPDATE SQL that matches a SELECT column");
    }

    // Warning if primary key not in UPDATE SQL
    String pk = config.getPrimaryKeyColumn();
    if (pk != null && !pk.isBlank() && !updateParams.contains(pk.toLowerCase())) {
      result.addWarning("Primary key column :" + pk + " is not used in UPDATE SQL WHERE clause");
    }
  }

  private void validateStandardMode(
      Set<String> updateParams, Set<String> userParams, ValidationResult result) {

    for (String param : updateParams) {
      if (!userParams.contains(param)) {
        result.addWarning(
            "Parameter :" + param + " in UPDATE SQL is not defined in parameters configuration");
      }
    }
  }

  /**
   * Get available parameters for display in the UI.
   *
   * @param config Query configuration form DTO
   * @return AvailableParameters with user, column, and special parameters
   */
  public AvailableParameters getAvailableParameters(QueryConfigFormDto config) {
    Set<String> userParams =
        config.getParameters() != null
            ? config.getParameters().stream()
                .filter(p -> p.getName() != null && !p.getName().isBlank())
                .map(QueryConfigFormDto.ParameterFormDto::getName)
                .collect(Collectors.toSet())
            : Set.of();

    Set<String> columnParams = analyzer.extractSelectColumns(config.getSelectSql());

    return new AvailableParameters(userParams, columnParams);
  }

  /** Validation result with errors and warnings. */
  @Data
  public static class ValidationResult {
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public void addError(String message) {
      errors.add(message);
    }

    public void addWarning(String message) {
      warnings.add(message);
    }
  }

  /** Available parameters for UI display. */
  @Data
  public static class AvailableParameters {
    private final Set<String> userParameters;
    private final Set<String> columnParameters;

    public String getUserParametersDisplay() {
      if (userParameters.isEmpty()) {
        return "None";
      }
      return userParameters.stream().map(p -> ":" + p).collect(Collectors.joining(", "));
    }

    public String getColumnParametersDisplay() {
      if (columnParameters.isEmpty()) {
        return "None (SELECT columns will be detected at runtime)";
      }
      return columnParameters.stream().map(p -> ":" + p).collect(Collectors.joining(", "));
    }
  }
}
