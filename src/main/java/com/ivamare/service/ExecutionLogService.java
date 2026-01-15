package com.ivamare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.domain.ExecutionType;
import com.ivamare.domain.Query;
import com.ivamare.repository.ExecutionLogRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for logging query executions. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionLogService {

  private final ExecutionLogRepository repository;
  private final ObjectMapper objectMapper;

  /**
   * Log a query execution.
   *
   * @param query The query that was executed
   * @param parameters The parameters used
   * @param rowCount Number of rows affected/returned
   * @param executionTimeMs Execution time in milliseconds
   * @param status Execution status
   * @param errorMessage Error message if failed
   * @param executionType Type of execution
   * @param executedBy Username of executor
   * @return The created execution log
   */
  @Transactional
  public ExecutionLog logExecution(
      Query query,
      Map<String, Object> parameters,
      int rowCount,
      long executionTimeMs,
      ExecutionStatus status,
      String errorMessage,
      ExecutionType executionType,
      String executedBy) {

    String paramsJson = serializeParameters(parameters);

    ExecutionLog executionLog =
        ExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .queryId(query.getId())
            .queryVersion(query.getCurrentVersion())
            .connectionName(query.getConnectionName())
            .executedBy(executedBy)
            .executedAt(LocalDateTime.now())
            .parameters(paramsJson)
            .rowCount(rowCount)
            .executionTimeMs(executionTimeMs)
            .status(status)
            .errorMessage(errorMessage)
            .executionType(executionType)
            .build();

    return repository.save(executionLog);
  }

  /** Log a successful SELECT query execution. */
  @Transactional
  public ExecutionLog logSelectSuccess(
      Query query, Map<String, Object> params, int rows, long timeMs, String user) {
    return logExecution(
        query, params, rows, timeMs, ExecutionStatus.SUCCESS, null, ExecutionType.SELECT, user);
  }

  /** Log a failed SELECT query execution. */
  @Transactional
  public ExecutionLog logSelectFailure(
      Query query, Map<String, Object> params, long timeMs, String error, String user) {
    return logExecution(
        query, params, 0, timeMs, ExecutionStatus.FAILED, error, ExecutionType.SELECT, user);
  }

  /** Log a timed out SELECT query execution. */
  @Transactional
  public ExecutionLog logSelectTimeout(
      Query query, Map<String, Object> params, long timeMs, String user) {
    return logExecution(
        query,
        params,
        0,
        timeMs,
        ExecutionStatus.TIMEOUT,
        "Query execution timed out",
        ExecutionType.SELECT,
        user);
  }

  /** Log a successful UPDATE workflow execution. */
  @Transactional
  public ExecutionLog logUpdateSuccess(
      Query query,
      Map<String, Object> params,
      int rows,
      long timeMs,
      String backupId,
      String user) {
    ExecutionLog executionLog =
        logExecution(
            query, params, rows, timeMs, ExecutionStatus.SUCCESS, null, ExecutionType.UPDATE, user);
    executionLog.setBackupRecordId(backupId);
    return repository.save(executionLog);
  }

  /** Log a failed UPDATE workflow execution. */
  @Transactional
  public ExecutionLog logUpdateFailure(
      Query query, Map<String, Object> params, long timeMs, String error, String user) {
    return logExecution(
        query, params, 0, timeMs, ExecutionStatus.FAILED, error, ExecutionType.UPDATE, user);
  }

  /** Log a successful ROLLBACK execution. */
  @Transactional
  public ExecutionLog logRollbackSuccess(
      Query query,
      Map<String, Object> params,
      int rows,
      long timeMs,
      String backupId,
      String user) {
    ExecutionLog executionLog =
        logExecution(
            query,
            params,
            rows,
            timeMs,
            ExecutionStatus.SUCCESS,
            null,
            ExecutionType.ROLLBACK,
            user);
    executionLog.setBackupRecordId(backupId);
    return repository.save(executionLog);
  }

  /** Log a failed ROLLBACK execution. */
  @Transactional
  public ExecutionLog logRollbackFailure(
      Query query, Map<String, Object> params, long timeMs, String error, String user) {
    return logExecution(
        query, params, 0, timeMs, ExecutionStatus.FAILED, error, ExecutionType.ROLLBACK, user);
  }

  /** Find execution log by ID. */
  @Transactional(readOnly = true)
  public ExecutionLog findById(String id) {
    return repository.findById(id).orElse(null);
  }

  /** Find execution logs with filters. */
  @Transactional(readOnly = true)
  public Page<ExecutionLog> findWithFilters(
      String user,
      String queryId,
      ExecutionStatus status,
      ExecutionType executionType,
      LocalDateTime startDate,
      LocalDateTime endDate,
      Pageable pageable) {
    return repository.findWithFilters(
        user, queryId, status, executionType, startDate, endDate, pageable);
  }

  /** Find recent logs by user. */
  @Transactional(readOnly = true)
  public Page<ExecutionLog> findByUser(String user, Pageable pageable) {
    return repository.findByExecutedByOrderByExecutedAtDesc(user, pageable);
  }

  /** Count logs by status. */
  @Transactional(readOnly = true)
  public long countByStatus(ExecutionStatus status) {
    return repository.countByStatus(status);
  }

  private String serializeParameters(Map<String, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "{}";
    }

    try {
      return objectMapper.writeValueAsString(parameters);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize parameters: {}", e.getMessage());
      return "{}";
    }
  }
}
