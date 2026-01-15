package com.ivamare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivamare.domain.*;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.repository.BackupRecordRepository;
import java.time.LocalDateTime;
import java.util.*;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int MAX_PREVIEW_ROWS = 100000;

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

    log.info(
        "Executing UPDATE for '{}' by user '{}' ({} rows to update)",
        query.getName(),
        executedBy,
        previewData.size());

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
          "UPDATE for '{}' completed: {} rows affected in {}ms, backup ID: {}",
          query.getName(),
          rowsAffected,
          duration,
          backupId);

      ExecutionResult result =
          ExecutionResult.builder()
              .success(true)
              .rowCount(rowsAffected)
              .totalRows(rowsAffected)
              .executionTimeMs(duration)
              .executionLogId(logEntry.getId())
              .build();

      return result;

    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      logService.logUpdateFailure(query, params, duration, e.getMessage(), executedBy);
      log.error("UPDATE for '{}' failed: {}", query.getName(), e.getMessage());
      return ExecutionResult.failure(e.getMessage(), duration, null);
    }
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
