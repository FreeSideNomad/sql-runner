package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ivamare.domain.*;
import com.ivamare.dto.QueryConfig;
import com.ivamare.repository.BackupRecordRepository;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for UpdateWorkflowService. */
@ExtendWith(MockitoExtension.class)
class UpdateWorkflowServiceTest {

  @Mock private ConnectionRegistry connectionRegistry;
  @Mock private ExecutionLogService logService;
  @Mock private QueryService queryService;
  @Mock private QueryExecutionService queryExecutionService;
  @Mock private BackupRecordRepository backupRepository;
  @Mock private UpdateParameterAnalyzer parameterAnalyzer;
  @Mock private SqlScriptGenerator scriptGenerator;
  @Mock private DataSource dataSource;

  private UpdateWorkflowService service;

  @BeforeEach
  void setUp() {
    service =
        new UpdateWorkflowService(
            connectionRegistry,
            logService,
            queryService,
            queryExecutionService,
            backupRepository,
            parameterAnalyzer,
            scriptGenerator);
  }

  @Test
  void executePreview_withNonUpdateWorkflowQuery_throwsException() {
    Query query = Query.builder().id("q1").name("Test").queryType(QueryType.SELECT).build();
    when(queryService.getQuery("q1")).thenReturn(query);

    assertThatThrownBy(() -> service.executePreview("q1", Map.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not an UPDATE_WORKFLOW");
  }

  @Test
  void executePreview_withMissingSelectSql_throwsException() {
    Query query =
        Query.builder().id("q1").name("Test").queryType(QueryType.UPDATE_WORKFLOW).build();
    when(queryService.getQuery("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("updateSql: UPDATE test SET x = 1");

    QueryConfig config = QueryConfig.builder().updateSql("UPDATE test SET x = 1").build();
    when(queryExecutionService.parseConfig(anyString())).thenReturn(config);

    assertThatThrownBy(() -> service.executePreview("q1", Map.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("selectSql");
  }

  @Test
  void executeUpdate_withNonUpdateWorkflowQuery_throwsException() {
    Query query = Query.builder().id("q1").name("Test").queryType(QueryType.SELECT).build();
    when(queryService.getQuery("q1")).thenReturn(query);

    assertThatThrownBy(() -> service.executeUpdate("q1", Map.of(), List.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not an UPDATE_WORKFLOW");
  }

  @Test
  void executeUpdate_withMissingUpdateSql_throwsException() {
    Query query =
        Query.builder().id("q1").name("Test").queryType(QueryType.UPDATE_WORKFLOW).build();
    when(queryService.getQuery("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");

    QueryConfig config = QueryConfig.builder().selectSql("SELECT * FROM test").build();
    when(queryExecutionService.parseConfig(anyString())).thenReturn(config);

    assertThatThrownBy(() -> service.executeUpdate("q1", Map.of(), List.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("updateSql");
  }

  @Test
  void executeRollback_withNoBackup_throwsException() {
    when(backupRepository.findByExecutionLogId("log1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.executeRollback("log1", "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No backup found");
  }

  @Test
  void executeRollback_withAlreadyRolledBack_throwsException() {
    BackupRecord backup =
        BackupRecord.builder().id("backup1").executionLogId("log1").isRolledBack(true).build();
    when(backupRepository.findByExecutionLogId("log1")).thenReturn(Optional.of(backup));

    assertThatThrownBy(() -> service.executeRollback("log1", "user"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already been rolled back");
  }

  @Test
  void getBackupForExecution_returnsBackup() {
    BackupRecord backup =
        BackupRecord.builder().id("backup1").executionLogId("log1").rowCount(5).build();
    when(backupRepository.findByExecutionLogId("log1")).thenReturn(Optional.of(backup));

    Optional<BackupRecord> result = service.getBackupForExecution("log1");

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo("backup1");
    assertThat(result.get().getRowCount()).isEqualTo(5);
  }

  @Test
  void getBackupForExecution_whenNotFound_returnsEmpty() {
    when(backupRepository.findByExecutionLogId("log1")).thenReturn(Optional.empty());

    Optional<BackupRecord> result = service.getBackupForExecution("log1");

    assertThat(result).isEmpty();
  }

  @Test
  void executePreview_withBlankSelectSql_throwsException() {
    Query query =
        Query.builder().id("q1").name("Test").queryType(QueryType.UPDATE_WORKFLOW).build();
    when(queryService.getQuery("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: ''");

    QueryConfig config = QueryConfig.builder().selectSql("   ").build();
    when(queryExecutionService.parseConfig(anyString())).thenReturn(config);

    assertThatThrownBy(() -> service.executePreview("q1", Map.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("selectSql");
  }

  @Test
  void executeUpdate_withBlankUpdateSql_throwsException() {
    Query query =
        Query.builder().id("q1").name("Test").queryType(QueryType.UPDATE_WORKFLOW).build();
    when(queryService.getQuery("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("updateSql: ''");

    QueryConfig config = QueryConfig.builder().selectSql("SELECT 1").updateSql("   ").build();
    when(queryExecutionService.parseConfig(anyString())).thenReturn(config);

    assertThatThrownBy(() -> service.executeUpdate("q1", Map.of(), List.of(), "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("updateSql");
  }

  @Test
  void executeRollback_withMissingExecutionLog_throwsException() {
    BackupRecord backup =
        BackupRecord.builder().id("backup1").executionLogId("log1").isRolledBack(false).build();
    when(backupRepository.findByExecutionLogId("log1")).thenReturn(Optional.of(backup));
    when(logService.findByIdOptional("log1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.executeRollback("log1", "user"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Execution log not found");
  }
}
