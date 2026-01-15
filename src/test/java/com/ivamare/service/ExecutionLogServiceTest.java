package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.domain.ExecutionType;
import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.repository.ExecutionLogRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for ExecutionLogService. */
@ExtendWith(MockitoExtension.class)
class ExecutionLogServiceTest {

  @Mock private ExecutionLogRepository repository;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private ExecutionLogService service;

  @Captor private ArgumentCaptor<ExecutionLog> logCaptor;

  private Query testQuery;

  @BeforeEach
  void setUp() {
    testQuery =
        Query.builder()
            .id("query-123")
            .name("Test Query")
            .connectionName("test-connection")
            .currentVersion(1)
            .queryType(QueryType.SELECT)
            .build();
  }

  @Test
  void logSelectSuccess_shouldCreateLogWithCorrectFields() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("status", "ACTIVE");

    when(objectMapper.writeValueAsString(params)).thenReturn("{\"status\":\"ACTIVE\"}");
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    ExecutionLog result = service.logSelectSuccess(testQuery, params, 100, 250L, "testuser");

    verify(repository).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getQueryId()).isEqualTo("query-123");
    assertThat(captured.getQueryVersion()).isEqualTo(1);
    assertThat(captured.getConnectionName()).isEqualTo("test-connection");
    assertThat(captured.getExecutedBy()).isEqualTo("testuser");
    assertThat(captured.getRowCount()).isEqualTo(100);
    assertThat(captured.getExecutionTimeMs()).isEqualTo(250L);
    assertThat(captured.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(captured.getExecutionType()).isEqualTo(ExecutionType.SELECT);
    assertThat(captured.getErrorMessage()).isNull();
    assertThat(result).isNotNull();
  }

  @Test
  void logSelectFailure_shouldCreateLogWithErrorMessage() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("id", "123");

    when(objectMapper.writeValueAsString(params)).thenReturn("{\"id\":\"123\"}");
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    ExecutionLog result =
        service.logSelectFailure(testQuery, params, 500L, "Connection timeout", "testuser");

    verify(repository).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(captured.getErrorMessage()).isEqualTo("Connection timeout");
    assertThat(captured.getRowCount()).isEqualTo(0);
    assertThat(result).isNotNull();
  }

  @Test
  void logSelectTimeout_shouldCreateLogWithTimeoutStatus() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("timeout", 60);

    when(objectMapper.writeValueAsString(params)).thenReturn("{\"timeout\":60}");
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    ExecutionLog result = service.logSelectTimeout(testQuery, params, 60000L, "testuser");

    verify(repository).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getStatus()).isEqualTo(ExecutionStatus.TIMEOUT);
    assertThat(captured.getErrorMessage()).isEqualTo("Query execution timed out");
    assertThat(result).isNotNull();
  }

  @Test
  void logUpdateSuccess_shouldIncludeBackupRecordId() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("account", "ACC-001");

    when(objectMapper.writeValueAsString(params)).thenReturn("{\"account\":\"ACC-001\"}");
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    ExecutionLog result =
        service.logUpdateSuccess(testQuery, params, 50, 300L, "backup-456", "testuser");

    verify(repository, times(2)).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getExecutionType()).isEqualTo(ExecutionType.UPDATE);
    assertThat(captured.getBackupRecordId()).isEqualTo("backup-456");
    assertThat(result).isNotNull();
  }

  @Test
  void logRollbackSuccess_shouldCreateRollbackLog() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("backupId", "backup-456");

    when(objectMapper.writeValueAsString(params)).thenReturn("{\"backupId\":\"backup-456\"}");
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    ExecutionLog result =
        service.logRollbackSuccess(testQuery, params, 50, 200L, "backup-456", "testuser");

    verify(repository, times(2)).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getExecutionType()).isEqualTo(ExecutionType.ROLLBACK);
    assertThat(captured.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(result).isNotNull();
  }

  @Test
  void logExecution_withNullParameters_shouldUseEmptyJson() throws JsonProcessingException {
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    service.logSelectSuccess(testQuery, null, 10, 100L, "testuser");

    verify(repository).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getParameters()).isEqualTo("{}");
  }

  @Test
  void logExecution_withSerializationFailure_shouldUseEmptyJson() throws JsonProcessingException {
    Map<String, Object> params = new HashMap<>();
    params.put("data", "test");

    when(objectMapper.writeValueAsString(params))
        .thenThrow(new JsonProcessingException("error") {});
    when(repository.save(any(ExecutionLog.class))).thenAnswer(i -> i.getArguments()[0]);

    service.logSelectSuccess(testQuery, params, 10, 100L, "testuser");

    verify(repository).save(logCaptor.capture());
    ExecutionLog captured = logCaptor.getValue();

    assertThat(captured.getParameters()).isEqualTo("{}");
  }

  @Test
  void countByStatus_shouldDelegateToRepository() {
    when(repository.countByStatus(ExecutionStatus.SUCCESS)).thenReturn(42L);

    long count = service.countByStatus(ExecutionStatus.SUCCESS);

    assertThat(count).isEqualTo(42L);
    verify(repository).countByStatus(ExecutionStatus.SUCCESS);
  }
}
