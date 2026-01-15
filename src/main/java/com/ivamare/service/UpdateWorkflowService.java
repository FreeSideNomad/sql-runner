package com.ivamare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivamare.config.ConnectionProperties.ConnectionConfig;
import com.ivamare.domain.*;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.repository.BackupRecordRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/** Service for executing UPDATE workflow queries with backup and rollback support. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateWorkflowService {

  private final ConnectionRegistry connectionRegistry;
  private final ExecutionLogService logService;
  private final QueryService queryService;
  private final QueryExecutionService queryExecutionService;
  private final BackupRecordRepository backupRepository;
  private final UpdateParameterAnalyzer parameterAnalyzer;
  private final SqlScriptGenerator scriptGenerator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int MAX_PREVIEW_ROWS = 100000;
  private static final int PROGRESS_LOG_INTERVAL = 100;

  /**
   * Execute the preview SELECT for an UPDATE workflow query.
   *
   * @param queryId Query ID
   * @param rawParams Raw parameter values from form
   * @param executedBy Username executing the query
   * @return ExecutionResult with preview rows
   */
  public ExecutionResult executePreview(
      String queryId, Map<String, String> rawParams, String executedBy) {

    Query query = queryService.getQuery(queryId);

    if (query.getQueryType() != QueryType.UPDATE_WORKFLOW) {
      throw new IllegalArgumentException("Query is not an UPDATE_WORKFLOW type");
    }

    String configYaml = queryService.getCurrentConfigYaml(queryId);
    QueryConfig config = queryExecutionService.parseConfig(configYaml);

    if (config.getSelectSql() == null || config.getSelectSql().isBlank()) {
      throw new IllegalArgumentException("UPDATE_WORKFLOW query must have selectSql defined");
    }

    log.info("Executing UPDATE preview for '{}' by user '{}'", query.getName(), executedBy);

    Map<String, Object> params =
        queryExecutionService.convertParameters(rawParams, config.getParameters());

    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

    long startTime = System.currentTimeMillis();

    try {
      List<Map<String, Object>> results = jdbc.queryForList(config.getSelectSql(), params);
      long duration = System.currentTimeMillis() - startTime;

      if (results.size() > MAX_PREVIEW_ROWS) {
        log.warn(
            "Preview for '{}' returned {} rows, exceeds max of {}",
            query.getName(),
            results.size(),
            MAX_PREVIEW_ROWS);
        return ExecutionResult.failure(
            "Preview returned " + results.size() + " rows. Maximum allowed is " + MAX_PREVIEW_ROWS,
            duration,
            null);
      }

      log.info(
          "UPDATE preview for '{}' completed: {} rows in {}ms",
          query.getName(),
          results.size(),
          duration);

      return ExecutionResult.success(results, duration, null);

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("UPDATE preview for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
  }

  /**
   * Execute the UPDATE with backup creation.
   *
   * @param queryId Query ID
   * @param rawParams Raw parameter values
   * @param previewData Previously fetched preview data to backup
   * @param executedBy Username
   * @return ExecutionResult with update count
   */
  @Transactional
  public ExecutionResult executeUpdate(
      String queryId,
      Map<String, String> rawParams,
      List<Map<String, Object>> previewData,
      String executedBy) {

    Query query = queryService.getQuery(queryId);

    if (query.getQueryType() != QueryType.UPDATE_WORKFLOW) {
      throw new IllegalArgumentException("Query is not an UPDATE_WORKFLOW type");
    }

    String configYaml = queryService.getCurrentConfigYaml(queryId);
    QueryConfig config = queryExecutionService.parseConfig(configYaml);

    if (config.getUpdateSql() == null || config.getUpdateSql().isBlank()) {
      throw new IllegalArgumentException("UPDATE_WORKFLOW query must have updateSql defined");
    }

    // Determine binding mode
    UpdateBindingMode mode = config.getUpdateBindingMode();
    if (mode == null) {
      mode = UpdateBindingMode.STANDARD;
    }

    log.info(
        "Executing UPDATE for '{}' by user '{}' ({} rows, mode: {})",
        query.getName(),
        executedBy,
        previewData.size(),
        mode);

    return switch (mode) {
      case BATCH -> executeBatchUpdate(query, config, rawParams, previewData, executedBy);
      case ROW_BY_ROW -> executeRowByRowUpdate(query, config, rawParams, previewData, executedBy);
      case STANDARD -> executeStandardUpdate(query, config, rawParams, previewData, executedBy);
    };
  }

  /** Execute STANDARD mode - UPDATE uses only user-input parameters. */
  private ExecutionResult executeStandardUpdate(
      Query query,
      QueryConfig config,
      Map<String, String> rawParams,
      List<Map<String, Object>> previewData,
      String executedBy) {

    Map<String, Object> params =
        queryExecutionService.convertParameters(rawParams, config.getParameters());

    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

    long startTime = System.currentTimeMillis();

    try {
      // Create backup record first
      String backupId = UUID.randomUUID().toString();
      String backupJson = serializeBackupData(previewData, config);

      BackupRecord backup =
          BackupRecord.builder()
              .id(backupId)
              .backupData(backupJson)
              .rowCount(previewData.size())
              .isRolledBack(false)
              .build();

      // Execute the UPDATE
      int rowsAffected = jdbc.update(config.getUpdateSql(), params);
      long duration = System.currentTimeMillis() - startTime;

      // Log successful execution
      ExecutionLog logEntry =
          logService.logUpdateSuccess(query, params, rowsAffected, duration, backupId, executedBy);

      // Link backup to execution log and save
      backup.setExecutionLogId(logEntry.getId());
      backupRepository.save(backup);

      log.info(
          "STANDARD UPDATE for '{}' completed: {} rows affected in {}ms",
          query.getName(),
          rowsAffected,
          duration);

      return ExecutionResult.builder()
          .success(true)
          .rowCount(rowsAffected)
          .totalRows(rowsAffected)
          .executionTimeMs(duration)
          .executionLogId(logEntry.getId())
          .build();

    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      long duration = System.currentTimeMillis() - startTime;
      logService.logUpdateFailure(query, params, duration, e.getMessage(), executedBy);
      log.error("STANDARD UPDATE for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
  }

  /** Execute BATCH mode - Collects all primary key values into :id_list for IN clause. */
  private ExecutionResult executeBatchUpdate(
      Query query,
      QueryConfig config,
      Map<String, String> rawParams,
      List<Map<String, Object>> previewData,
      String executedBy) {

    Map<String, Object> params =
        queryExecutionService.convertParameters(rawParams, config.getParameters());

    // Build id_list from preview data
    String pkColumn = config.getPrimaryKeyColumn();
    if (pkColumn == null || pkColumn.isBlank()) {
      return ExecutionResult.failure("Batch mode requires primaryKeyColumn to be defined", 0, null);
    }

    List<Object> idList =
        previewData.stream()
            .map(row -> getColumnValueCaseInsensitive(row, pkColumn))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (idList.isEmpty()) {
      log.info("BATCH UPDATE for '{}': No rows to update (empty id_list)", query.getName());
      return ExecutionResult.builder()
          .success(true)
          .rowCount(0)
          .totalRows(0)
          .executionTimeMs(0)
          .message("No rows to update")
          .build();
    }

    params.put("id_list", idList);

    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

    long startTime = System.currentTimeMillis();

    try {
      // Create backup record first
      String backupId = UUID.randomUUID().toString();
      String backupJson = serializeBackupData(previewData, config);

      BackupRecord backup =
          BackupRecord.builder()
              .id(backupId)
              .backupData(backupJson)
              .rowCount(previewData.size())
              .isRolledBack(false)
              .build();

      // Execute single UPDATE with IN clause
      int rowsAffected = jdbc.update(config.getUpdateSql(), params);
      long duration = System.currentTimeMillis() - startTime;

      // Log successful execution
      ExecutionLog logEntry =
          logService.logUpdateSuccess(query, params, rowsAffected, duration, backupId, executedBy);

      // Link backup to execution log and save
      backup.setExecutionLogId(logEntry.getId());
      backupRepository.save(backup);

      log.info(
          "BATCH UPDATE for '{}' completed: {} rows affected in {}ms (id_list size: {})",
          query.getName(),
          rowsAffected,
          duration,
          idList.size());

      return ExecutionResult.builder()
          .success(true)
          .rowCount(rowsAffected)
          .totalRows(rowsAffected)
          .executionTimeMs(duration)
          .executionLogId(logEntry.getId())
          .build();

    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      long duration = System.currentTimeMillis() - startTime;
      logService.logUpdateFailure(query, params, duration, e.getMessage(), executedBy);
      log.error("BATCH UPDATE for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
  }

  /** Execute ROW_BY_ROW mode - Executes one UPDATE per preview row, binding column values. */
  private ExecutionResult executeRowByRowUpdate(
      Query query,
      QueryConfig config,
      Map<String, String> rawParams,
      List<Map<String, Object>> previewData,
      String executedBy) {

    // Convert user parameters once
    Map<String, Object> userParams =
        queryExecutionService.convertParameters(rawParams, config.getParameters());

    // Analyze which params come from columns
    Set<String> selectColumns = previewData.isEmpty() ? Set.of() : previewData.get(0).keySet();

    UpdateParameterAnalyzer.ParameterBindings bindings =
        parameterAnalyzer.analyzeBindings(
            config.getUpdateSql(), selectColumns, config.getParameters());

    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

    long startTime = System.currentTimeMillis();

    try {
      // Create backup record first
      String backupId = UUID.randomUUID().toString();
      String backupJson = serializeBackupData(previewData, config);

      BackupRecord backup =
          BackupRecord.builder()
              .id(backupId)
              .backupData(backupJson)
              .rowCount(previewData.size())
              .isRolledBack(false)
              .build();

      int totalUpdated = 0;

      for (int i = 0; i < previewData.size(); i++) {
        Map<String, Object> row = previewData.get(i);

        // Combine user params with row column values
        Map<String, Object> combinedParams = new HashMap<>(userParams);
        for (String colParam : bindings.getColumnBoundParams()) {
          Object value = getColumnValueCaseInsensitive(row, colParam);
          combinedParams.put(colParam, value);
        }

        int affected = jdbc.update(config.getUpdateSql(), combinedParams);
        totalUpdated += affected;

        // Progress logging
        if ((i + 1) % PROGRESS_LOG_INTERVAL == 0) {
          log.info(
              "ROW_BY_ROW UPDATE progress for '{}': {}/{} rows",
              query.getName(),
              i + 1,
              previewData.size());
        }
      }

      long duration = System.currentTimeMillis() - startTime;

      // Log successful execution
      ExecutionLog logEntry =
          logService.logUpdateSuccess(
              query, userParams, totalUpdated, duration, backupId, executedBy);

      // Link backup to execution log and save
      backup.setExecutionLogId(logEntry.getId());
      backupRepository.save(backup);

      log.info(
          "ROW_BY_ROW UPDATE for '{}' completed: {} rows updated in {}ms ({} rows processed)",
          query.getName(),
          totalUpdated,
          duration,
          previewData.size());

      return ExecutionResult.builder()
          .success(true)
          .rowCount(totalUpdated)
          .totalRows(previewData.size())
          .executionTimeMs(duration)
          .executionLogId(logEntry.getId())
          .build();

    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      long duration = System.currentTimeMillis() - startTime;
      logService.logUpdateFailure(query, userParams, duration, e.getMessage(), executedBy);
      log.error("ROW_BY_ROW UPDATE for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
  }

  /** Get column value from row, case-insensitive. */
  private Object getColumnValueCaseInsensitive(Map<String, Object> row, String columnName) {
    // Try exact match first
    if (row.containsKey(columnName)) {
      return row.get(columnName);
    }
    // Try case-insensitive match
    for (Map.Entry<String, Object> entry : row.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(columnName)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Execute rollback for a previous UPDATE.
   *
   * @param executionLogId The execution log ID of the UPDATE to rollback
   * @param executedBy Username executing the rollback
   * @return ExecutionResult with rollback count
   */
  @Transactional
  public ExecutionResult executeRollback(String executionLogId, String executedBy) {

    BackupRecord backup =
        backupRepository
            .findByExecutionLogId(executionLogId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No backup found for execution: " + executionLogId));

    if (backup.getIsRolledBack()) {
      throw new IllegalStateException("This execution has already been rolled back");
    }

    ExecutionLog originalLog =
        logService
            .findByIdOptional(executionLogId)
            .orElseThrow(
                () -> new IllegalArgumentException("Execution log not found: " + executionLogId));

    Query query = queryService.getQuery(originalLog.getQueryId());
    String configYaml = queryService.getCurrentConfigYaml(query.getId());
    QueryConfig config = queryExecutionService.parseConfig(configYaml);

    log.info(
        "Executing ROLLBACK for execution '{}' query '{}' by user '{}'",
        executionLogId,
        query.getName(),
        executedBy);

    DataSource ds = connectionRegistry.getDataSource(query.getConnectionName());
    NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(ds);

    long startTime = System.currentTimeMillis();

    try {
      List<Map<String, Object>> backupData = deserializeBackupData(backup.getBackupData());
      int rollbackCount = 0;

      String pkColumn = config.getPrimaryKeyColumn();
      List<String> rollbackColumns = config.getRollbackColumns();

      if (pkColumn == null || pkColumn.isBlank()) {
        throw new IllegalStateException("Primary key column not defined for rollback");
      }

      if (rollbackColumns == null || rollbackColumns.isEmpty()) {
        throw new IllegalStateException("Rollback columns not defined");
      }

      // Generate and execute rollback UPDATEs for each row
      for (Map<String, Object> row : backupData) {
        Object pkValue = row.get(pkColumn);
        if (pkValue == null) {
          log.warn("Skipping row with null primary key value");
          continue;
        }

        String rollbackSql = generateRollbackSql(query, config, pkColumn, rollbackColumns);
        Map<String, Object> rollbackParams = new HashMap<>();
        rollbackParams.put("pk_value", pkValue);

        for (String col : rollbackColumns) {
          rollbackParams.put(col, row.get(col));
        }

        jdbc.update(rollbackSql, rollbackParams);
        rollbackCount++;
      }

      long duration = System.currentTimeMillis() - startTime;

      // Log successful rollback
      ExecutionLog rollbackLog =
          logService.logRollbackSuccess(
              query, Map.of(), rollbackCount, duration, backup.getId(), executedBy);

      // Mark backup as rolled back
      backup.setIsRolledBack(true);
      backup.setRolledBackAt(LocalDateTime.now());
      backup.setRolledBackBy(executedBy);
      backup.setRollbackExecutionLogId(rollbackLog.getId());
      backupRepository.save(backup);

      log.info(
          "ROLLBACK for '{}' completed: {} rows restored in {}ms",
          query.getName(),
          rollbackCount,
          duration);

      return ExecutionResult.builder()
          .success(true)
          .rowCount(rollbackCount)
          .totalRows(rollbackCount)
          .executionTimeMs(duration)
          .executionLogId(rollbackLog.getId())
          .build();

    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      long duration = System.currentTimeMillis() - startTime;
      logService.logRollbackFailure(query, Map.of(), duration, e.getMessage(), executedBy);
      log.error("ROLLBACK for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
  }

  /**
   * Get backup record for an execution.
   *
   * @param executionLogId Execution log ID
   * @return Optional backup record
   */
  public Optional<BackupRecord> getBackupForExecution(String executionLogId) {
    return backupRepository.findByExecutionLogId(executionLogId);
  }

  /**
   * Generate SQL script for the update operation.
   *
   * @param queryId Query ID
   * @param rawParams Raw parameters
   * @param previewData Preview data
   * @return Generated SQL script
   */
  public String generateUpdateScript(
      String queryId, Map<String, String> rawParams, List<Map<String, Object>> previewData) {

    Query query = queryService.getQuery(queryId);
    String configYaml = queryService.getCurrentConfigYaml(queryId);
    QueryConfig config = queryExecutionService.parseConfig(configYaml);

    Map<String, Object> params =
        queryExecutionService.convertParameters(rawParams, config.getParameters());

    ConnectionConfig connConfig = connectionRegistry.getConnectionConfig(query.getConnectionName());
    if (connConfig == null) {
      throw new IllegalArgumentException("Unknown connection: " + query.getConnectionName());
    }

    return scriptGenerator.generateScript(config, params, previewData, connConfig.getType());
  }

  /**
   * Generate SQL script for the rollback operation.
   *
   * @param executionLogId Execution log ID
   * @return Generated SQL script
   */
  public String generateRollbackScript(String executionLogId) {
    BackupRecord backup =
        backupRepository
            .findByExecutionLogId(executionLogId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No backup found for execution: " + executionLogId));

    ExecutionLog originalLog =
        logService
            .findByIdOptional(executionLogId)
            .orElseThrow(
                () -> new IllegalArgumentException("Execution log not found: " + executionLogId));

    Query query = queryService.getQuery(originalLog.getQueryId());
    String configYaml = queryService.getCurrentConfigYaml(query.getId());
    QueryConfig config = queryExecutionService.parseConfig(configYaml);

    ConnectionConfig connConfig = connectionRegistry.getConnectionConfig(query.getConnectionName());
    if (connConfig == null) {
      throw new IllegalArgumentException("Unknown connection: " + query.getConnectionName());
    }

    List<Map<String, Object>> backupData = deserializeBackupData(backup.getBackupData());
    String pkColumn = config.getPrimaryKeyColumn();
    List<String> rollbackColumns = config.getRollbackColumns();

    if (pkColumn == null) {
      throw new IllegalStateException(
          "Missing rollback configuration: primaryKeyColumn is not defined");
    }
    if (rollbackColumns == null || rollbackColumns.isEmpty()) {
      throw new IllegalStateException(
          "Missing rollback configuration: rollbackColumns is not defined or empty");
    }

    String rollbackSql = generateRollbackSql(query, config, pkColumn, rollbackColumns);

    // Create a temporary config for the generator
    QueryConfig rollbackConfig =
        QueryConfig.builder()
            .updateSql(rollbackSql)
            .updateBindingMode(UpdateBindingMode.ROW_BY_ROW) // Rollback is always row-by-row
            .primaryKeyColumn(pkColumn)
            .parameters(List.of()) // No user params for rollback
            .build();

    // Map backup data to parameters format expected by generator
    // For rollback, 'rowParams' should map column names to values
    // But generateRowByRowScript expects 'params' + 'row' columns
    // Here we can just pass empty params since all values come from the row (backup data)
    // However, we need to ensure the rollback SQL parameters (:col) match the backup data keys

    // Refactor generateRollbackSql to use standard parameter naming if needed
    // Currently it uses :col for SET col = :col
    // This matches the column names in backupData, so it should work with ROW_BY_ROW logic.

    return scriptGenerator.generateScript(
        rollbackConfig, Map.of(), backupData, connConfig.getType());
  }

  private String serializeBackupData(List<Map<String, Object>> data, QueryConfig config) {
    try {
      // Only backup the columns we need for rollback
      List<String> columnsToBackup = new ArrayList<>();
      if (config.getPrimaryKeyColumn() != null) {
        columnsToBackup.add(config.getPrimaryKeyColumn());
      }
      if (config.getBackupColumns() != null) {
        columnsToBackup.addAll(config.getBackupColumns());
      }

      List<Map<String, Object>> filteredData = new ArrayList<>();
      for (Map<String, Object> row : data) {
        Map<String, Object> filteredRow = new LinkedHashMap<>();
        for (String col : columnsToBackup) {
          if (row.containsKey(col)) {
            filteredRow.put(col, row.get(col));
          }
        }
        filteredData.add(filteredRow);
      }

      return objectMapper.writeValueAsString(filteredData);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize backup data", e);
    }
  }

  private List<Map<String, Object>> deserializeBackupData(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize backup data", e);
    }
  }

  /**
   * Deserialize backup data from a BackupRecord.
   *
   * @param backup the backup record
   * @return list of row data as maps
   */
  public List<Map<String, Object>> deserializeBackupData(BackupRecord backup) {
    return deserializeBackupData(backup.getBackupData());
  }

  private String generateRollbackSql(
      Query query, QueryConfig config, String pkColumn, List<String> rollbackColumns) {

    // Get table name from the original UPDATE SQL
    String updateSql = config.getUpdateSql().trim();
    String tableName = extractTableName(updateSql);

    StringBuilder sql = new StringBuilder("UPDATE ");
    sql.append(tableName).append(" SET ");

    boolean first = true;
    for (String col : rollbackColumns) {
      if (!first) {
        sql.append(", ");
      }
      sql.append(col).append(" = :").append(col);
      first = false;
    }

    sql.append(" WHERE ").append(pkColumn).append(" = :pk_value");

    return sql.toString();
  }

  private String extractTableName(String updateSql) {
    // Simple extraction: UPDATE table_name SET ...
    String upper = updateSql.toUpperCase();
    int updateIdx = upper.indexOf("UPDATE");
    int setIdx = upper.indexOf("SET");

    if (updateIdx == -1 || setIdx == -1) {
      throw new IllegalArgumentException("Invalid UPDATE SQL format");
    }

    String tablePart = updateSql.substring(updateIdx + 6, setIdx).trim();

    // Handle schema.table format
    return tablePart.split("\\s+")[0];
  }
}
