package com.ivamare.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of query execution. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResult {

  private boolean success;
  private List<Map<String, Object>> rows;
  private List<String> columns;
  private int rowCount;
  private int totalRows;
  private long executionTimeMs;
  private String errorMessage;
  private String message;
  private String executionLogId;

  /** Create a successful execution result. */
  public static ExecutionResult success(List<Map<String, Object>> rows, long timeMs, String logId) {
    List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
    return ExecutionResult.builder()
        .success(true)
        .rows(rows)
        .columns(columns)
        .rowCount(rows.size())
        .totalRows(rows.size())
        .executionTimeMs(timeMs)
        .executionLogId(logId)
        .build();
  }

  /** Create a failed execution result. */
  public static ExecutionResult failure(String error, long timeMs, String logId) {
    return ExecutionResult.builder()
        .success(false)
        .rows(List.of())
        .columns(List.of())
        .rowCount(0)
        .totalRows(0)
        .executionTimeMs(timeMs)
        .errorMessage(error)
        .executionLogId(logId)
        .build();
  }
}
