package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ivamare.domain.BackupRecord;
import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.repository.BackupRecordRepository;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for UpdateWorkflowService using H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UpdateWorkflowServiceIntegrationTest {

  @Autowired private UpdateWorkflowService updateWorkflowService;
  @Autowired private QueryRepository queryRepository;
  @Autowired private QueryVersionRepository versionRepository;
  @Autowired private BackupRecordRepository backupRepository;
  @Autowired private DataSource dataSource;
  @MockBean private ConnectionRegistry connectionRegistry;

  private JdbcTemplate jdbcTemplate;
  private Query testQuery;

  @BeforeEach
  void setUp() {
    jdbcTemplate = new JdbcTemplate(dataSource);

    // Mock connection registry to return the main datasource for any connection
    when(connectionRegistry.getDataSource(anyString())).thenReturn(dataSource);

    // Create a test table for UPDATE workflow using the main datasource
    jdbcTemplate.execute(
        "CREATE TABLE IF NOT EXISTS sqlrunner.test_update_table ("
            + "id INT PRIMARY KEY, "
            + "name VARCHAR(100), "
            + "status VARCHAR(20)"
            + ")");
    jdbcTemplate.execute("DELETE FROM sqlrunner.test_update_table");
    jdbcTemplate.execute(
        "INSERT INTO sqlrunner.test_update_table VALUES (1, 'Original', 'ACTIVE')");
    jdbcTemplate.execute("INSERT INTO sqlrunner.test_update_table VALUES (2, 'Second', 'ACTIVE')");

    // Create update workflow query that uses the internal connection
    String configYaml =
        """
        selectSql: SELECT id, name, status FROM sqlrunner.test_update_table WHERE status = :status
        updateSql: UPDATE sqlrunner.test_update_table SET name = 'Updated' WHERE status = :status
        primaryKeyColumn: id
        backupColumns:
          - name
          - status
        rollbackColumns:
          - name
        parameters:
          - name: status
            dataType: STRING
            required: true
        """;

    testQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Test Update Query")
            .description("Test update workflow")
            .category("Test")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .connectionName("test-conn")
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    testQuery = queryRepository.save(testQuery);

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(testQuery)
            .version(1)
            .configYaml(configYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    versionRepository.save(version);
  }

  @Test
  void executePreview_returnsMatchingRows() {
    Map<String, String> params = Map.of("status", "ACTIVE");

    ExecutionResult result =
        updateWorkflowService.executePreview(testQuery.getId(), params, "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(2);
    assertThat(result.getRows()).hasSize(2);
  }

  @Test
  void executePreview_withNoMatch_returnsEmptyResult() {
    Map<String, String> params = Map.of("status", "INACTIVE");

    ExecutionResult result =
        updateWorkflowService.executePreview(testQuery.getId(), params, "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(0);
  }

  @Test
  void executeUpdate_updatesRowsAndCreatesBackup() {
    Map<String, String> params = Map.of("status", "ACTIVE");
    List<Map<String, Object>> previewData =
        List.of(
            Map.of("id", 1, "name", "Original", "status", "ACTIVE"),
            Map.of("id", 2, "name", "Second", "status", "ACTIVE"));

    ExecutionResult result =
        updateWorkflowService.executeUpdate(testQuery.getId(), params, previewData, "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(2);
    assertThat(result.getExecutionLogId()).isNotNull();

    // Verify data was updated
    List<Map<String, Object>> updated =
        jdbcTemplate.queryForList(
            "SELECT name FROM sqlrunner.test_update_table WHERE status = 'ACTIVE'");
    assertThat(updated).allSatisfy(row -> assertThat(row.get("NAME")).isEqualTo("Updated"));

    // Verify backup was created
    Optional<BackupRecord> backup =
        updateWorkflowService.getBackupForExecution(result.getExecutionLogId());
    assertThat(backup).isPresent();
    assertThat(backup.get().getRowCount()).isEqualTo(2);
    assertThat(backup.get().getIsRolledBack()).isFalse();
  }

  @Test
  void executeRollback_restoresOriginalData() {
    // First, execute an update
    Map<String, String> params = Map.of("status", "ACTIVE");
    List<Map<String, Object>> previewData =
        List.of(
            Map.of("id", 1, "name", "Original", "status", "ACTIVE"),
            Map.of("id", 2, "name", "Second", "status", "ACTIVE"));

    ExecutionResult updateResult =
        updateWorkflowService.executeUpdate(testQuery.getId(), params, previewData, "user");
    assertThat(updateResult.isSuccess()).isTrue();

    // Verify data was updated
    String updatedName =
        jdbcTemplate.queryForObject(
            "SELECT name FROM sqlrunner.test_update_table WHERE id = 1", String.class);
    assertThat(updatedName).isEqualTo("Updated");

    // Now rollback
    ExecutionResult rollbackResult =
        updateWorkflowService.executeRollback(updateResult.getExecutionLogId(), "user");

    assertThat(rollbackResult.isSuccess()).isTrue();
    assertThat(rollbackResult.getRowCount()).isEqualTo(2);

    // Verify data was restored
    String restoredName =
        jdbcTemplate.queryForObject(
            "SELECT name FROM sqlrunner.test_update_table WHERE id = 1", String.class);
    assertThat(restoredName).isEqualTo("Original");

    // Verify backup is marked as rolled back
    Optional<BackupRecord> backup =
        updateWorkflowService.getBackupForExecution(updateResult.getExecutionLogId());
    assertThat(backup).isPresent();
    assertThat(backup.get().getIsRolledBack()).isTrue();
  }

  @Test
  void executeRollback_alreadyRolledBack_throwsException() {
    // First, execute an update
    Map<String, String> params = Map.of("status", "ACTIVE");
    List<Map<String, Object>> previewData =
        List.of(Map.of("id", 1, "name", "Original", "status", "ACTIVE"));

    ExecutionResult updateResult =
        updateWorkflowService.executeUpdate(testQuery.getId(), params, previewData, "user");

    // Rollback once
    updateWorkflowService.executeRollback(updateResult.getExecutionLogId(), "user");

    // Try to rollback again
    assertThatThrownBy(
            () -> updateWorkflowService.executeRollback(updateResult.getExecutionLogId(), "user"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already been rolled back");
  }

  @Test
  void executeBatchUpdate_usesIdListParameter() {
    // Create batch mode query
    String batchConfigYaml =
        """
        selectSql: SELECT id, name, status FROM sqlrunner.test_update_table WHERE status = :status
        updateSql: UPDATE sqlrunner.test_update_table SET name = 'BatchUpdated' WHERE id IN (:id_list)
        updateBindingMode: BATCH
        primaryKeyColumn: id
        backupColumns:
          - name
          - status
        rollbackColumns:
          - name
        parameters:
          - name: status
            dataType: STRING
            required: true
        """;

    Query batchQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Batch Update Query")
            .category("Test")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .connectionName("test-conn")
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    batchQuery = queryRepository.save(batchQuery);

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(batchQuery)
            .version(1)
            .configYaml(batchConfigYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    versionRepository.save(version);

    // Execute with preview data containing IDs
    Map<String, String> params = Map.of("status", "ACTIVE");
    List<Map<String, Object>> previewData =
        List.of(
            Map.of("id", 1, "name", "Original", "status", "ACTIVE"),
            Map.of("id", 2, "name", "Second", "status", "ACTIVE"));

    ExecutionResult result =
        updateWorkflowService.executeUpdate(batchQuery.getId(), params, previewData, "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(2);

    // Verify data was updated
    List<Map<String, Object>> updated =
        jdbcTemplate.queryForList(
            "SELECT name FROM sqlrunner.test_update_table WHERE id IN (1, 2)");
    assertThat(updated).allSatisfy(row -> assertThat(row.get("NAME")).isEqualTo("BatchUpdated"));
  }

  @Test
  void executeBatchUpdate_withEmptyIdList_returnsSuccessWithZeroRows() {
    // Create batch mode query
    String batchConfigYaml =
        """
        selectSql: SELECT id, name, status FROM sqlrunner.test_update_table WHERE status = :status
        updateSql: UPDATE sqlrunner.test_update_table SET name = 'BatchUpdated' WHERE id IN (:id_list)
        updateBindingMode: BATCH
        primaryKeyColumn: id
        backupColumns:
          - name
        rollbackColumns:
          - name
        parameters:
          - name: status
            dataType: STRING
        """;

    Query batchQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Batch Empty Query")
            .category("Test")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .connectionName("test-conn")
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    batchQuery = queryRepository.save(batchQuery);

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(batchQuery)
            .version(1)
            .configYaml(batchConfigYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    versionRepository.save(version);

    // Execute with empty preview data
    ExecutionResult result =
        updateWorkflowService.executeUpdate(batchQuery.getId(), Map.of(), List.of(), "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(0);
    assertThat(result.getMessage()).isEqualTo("No rows to update");
  }

  @Test
  void executeRowByRowUpdate_updatesEachRowWithColumnValues() {
    // Create row-by-row mode query
    String rowByRowConfigYaml =
        """
        selectSql: SELECT id, name, status FROM sqlrunner.test_update_table WHERE status = :status
        updateSql: UPDATE sqlrunner.test_update_table SET name = UPPER(:name) WHERE id = :id
        updateBindingMode: ROW_BY_ROW
        primaryKeyColumn: id
        backupColumns:
          - name
          - status
        rollbackColumns:
          - name
        parameters:
          - name: status
            dataType: STRING
            required: true
        """;

    Query rowByRowQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Row By Row Query")
            .category("Test")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .connectionName("test-conn")
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    rowByRowQuery = queryRepository.save(rowByRowQuery);

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(rowByRowQuery)
            .version(1)
            .configYaml(rowByRowConfigYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    versionRepository.save(version);

    // Execute with preview data containing column values
    Map<String, String> params = Map.of("status", "ACTIVE");
    List<Map<String, Object>> previewData =
        List.of(
            Map.of("id", 1, "name", "original", "status", "ACTIVE"),
            Map.of("id", 2, "name", "second", "status", "ACTIVE"));

    ExecutionResult result =
        updateWorkflowService.executeUpdate(rowByRowQuery.getId(), params, previewData, "user");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(2);

    // Verify data was updated with UPPER transformation
    String name1 =
        jdbcTemplate.queryForObject(
            "SELECT name FROM sqlrunner.test_update_table WHERE id = 1", String.class);
    String name2 =
        jdbcTemplate.queryForObject(
            "SELECT name FROM sqlrunner.test_update_table WHERE id = 2", String.class);

    assertThat(name1).isEqualTo("ORIGINAL");
    assertThat(name2).isEqualTo("SECOND");
  }

  @Test
  void executeBatchUpdate_withMissingPrimaryKeyColumn_returnsFailure() {
    // Create batch mode query without primaryKeyColumn
    String batchConfigYaml =
        """
        selectSql: SELECT id, name FROM sqlrunner.test_update_table
        updateSql: UPDATE sqlrunner.test_update_table SET name = 'Test' WHERE id IN (:id_list)
        updateBindingMode: BATCH
        """;

    Query batchQuery =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Batch No PK Query")
            .category("Test")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .connectionName("test-conn")
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    batchQuery = queryRepository.save(batchQuery);

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(batchQuery)
            .version(1)
            .configYaml(batchConfigYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();

    versionRepository.save(version);

    List<Map<String, Object>> previewData = List.of(Map.of("id", 1, "name", "Test"));

    ExecutionResult result =
        updateWorkflowService.executeUpdate(batchQuery.getId(), Map.of(), previewData, "user");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).isNotNull();
    assertThat(result.getErrorMessage()).contains("primaryKeyColumn");
  }
}
