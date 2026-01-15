package com.ivamare.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for ExecutionResult. */
class ExecutionResultTest {

  @Test
  void success_withRows_createsSuccessfulResult() {
    List<Map<String, Object>> rows =
        List.of(Map.of("id", 1, "name", "Test"), Map.of("id", 2, "name", "Test2"));

    ExecutionResult result = ExecutionResult.success(rows, 100L, "log-123");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRows()).hasSize(2);
    assertThat(result.getColumns()).containsExactlyInAnyOrder("id", "name");
    assertThat(result.getRowCount()).isEqualTo(2);
    assertThat(result.getTotalRows()).isEqualTo(2);
    assertThat(result.getExecutionTimeMs()).isEqualTo(100L);
    assertThat(result.getExecutionLogId()).isEqualTo("log-123");
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  void success_withEmptyRows_createsSuccessfulResult() {
    List<Map<String, Object>> rows = List.of();

    ExecutionResult result = ExecutionResult.success(rows, 50L, "log-456");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRows()).isEmpty();
    assertThat(result.getColumns()).isEmpty();
    assertThat(result.getRowCount()).isEqualTo(0);
    assertThat(result.getTotalRows()).isEqualTo(0);
    assertThat(result.getExecutionTimeMs()).isEqualTo(50L);
    assertThat(result.getExecutionLogId()).isEqualTo("log-456");
  }

  @Test
  void failure_createsFailedResult() {
    ExecutionResult result = ExecutionResult.failure("Connection refused", 200L, "log-789");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getRows()).isEmpty();
    assertThat(result.getColumns()).isEmpty();
    assertThat(result.getRowCount()).isEqualTo(0);
    assertThat(result.getTotalRows()).isEqualTo(0);
    assertThat(result.getExecutionTimeMs()).isEqualTo(200L);
    assertThat(result.getErrorMessage()).isEqualTo("Connection refused");
    assertThat(result.getExecutionLogId()).isEqualTo("log-789");
  }

  @Test
  void builder_createsResultWithAllFields() {
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("col", "val")))
            .columns(List.of("col"))
            .rowCount(1)
            .totalRows(10)
            .executionTimeMs(150L)
            .executionLogId("log-abc")
            .errorMessage(null)
            .build();

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(1);
    assertThat(result.getTotalRows()).isEqualTo(10);
  }
}
