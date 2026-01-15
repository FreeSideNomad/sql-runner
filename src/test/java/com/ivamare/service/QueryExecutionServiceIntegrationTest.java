package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.repository.ExecutionLogRepository;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import java.time.LocalDateTime;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Integration tests for QueryExecutionService. */
@SpringBootTest
@ActiveProfiles("test")
class QueryExecutionServiceIntegrationTest {

  @Autowired private QueryExecutionService executionService;
  @Autowired private QueryRepository queryRepository;
  @Autowired private QueryVersionRepository versionRepository;
  @Autowired private ExecutionLogRepository executionLogRepository;
  @Autowired private DataSource dataSource;

  @MockBean private ConnectionRegistry connectionRegistry;

  private Query testQuery;

  @AfterEach
  void cleanup() {
    // Clean up test data in correct order (respecting FK constraints)
    executionLogRepository.deleteAll();
    versionRepository.deleteAll();
    queryRepository.deleteAll();
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("DROP TABLE IF EXISTS test_table");
  }

  @BeforeEach
  void setUp() {
    // Create test tables using the main datasource
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS test_table (id INT, name VARCHAR(100), status VARCHAR(20))");
    jdbc.execute("DELETE FROM test_table");
    jdbc.execute("INSERT INTO test_table (id, name, status) VALUES (1, 'Test One', 'ACTIVE')");
    jdbc.execute("INSERT INTO test_table (id, name, status) VALUES (2, 'Test Two', 'INACTIVE')");
    jdbc.execute("INSERT INTO test_table (id, name, status) VALUES (3, 'Test Three', 'ACTIVE')");

    // Create test query
    testQuery =
        Query.builder()
            .id("test-query-exec")
            .name("Test Execution Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();
    queryRepository.save(testQuery);

    // Create version with config
    String configYaml =
        """
        sql: SELECT id, name, status FROM test_table WHERE status = :status
        parameters:
          - name: status
            dataType: STRING
            required: true
        """;

    QueryVersion version =
        QueryVersion.builder()
            .id("test-version-1")
            .query(testQuery)
            .version(1)
            .configYaml(configYaml)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();
    versionRepository.save(version);

    // Mock connection registry to return main datasource
    when(connectionRegistry.getDataSource(anyString())).thenReturn(dataSource);
  }

  @Test
  void executeSelect_withValidQuery_returnsResults() {
    ExecutionResult result =
        executionService.executeSelect("test-query-exec", Map.of("status", "ACTIVE"), "testuser");

    assertThat(result.isSuccess()).isTrue();
    // H2 returns uppercase column names
    assertThat(result.getColumns()).containsAnyOf("id", "ID");
    assertThat(result.getColumns()).containsAnyOf("name", "NAME");
    assertThat(result.getColumns()).containsAnyOf("status", "STATUS");
    assertThat(result.getExecutionLogId()).isNotNull();
  }

  @Test
  void executeSelect_withNoMatchingRows_returnsEmptyResult() {
    ExecutionResult result =
        executionService.executeSelect("test-query-exec", Map.of("status", "PENDING"), "testuser");

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(0);
  }

  @Test
  void executeSelect_withInvalidSql_returnsError() {
    // Create query with invalid SQL
    Query badQuery =
        Query.builder()
            .id("bad-query")
            .name("Bad Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();
    queryRepository.save(badQuery);

    QueryVersion badVersion =
        QueryVersion.builder()
            .id("bad-version-1")
            .query(badQuery)
            .version(1)
            .configYaml("sql: SELECT * FROM nonexistent_table")
            .createdAt(LocalDateTime.now())
            .createdBy("test")
            .build();
    versionRepository.save(badVersion);

    ExecutionResult result = executionService.executeSelect("bad-query", Map.of(), "testuser");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getErrorMessage()).isNotNull();
    assertThat(result.getExecutionLogId()).isNotNull();
  }

  @Test
  void executeSelectWithTimeout_completesBeforeTimeout() {
    ExecutionResult result =
        executionService.executeSelectWithTimeout(
            "test-query-exec", Map.of("status", "ACTIVE"), "testuser", 10);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getRowCount()).isEqualTo(2);
  }
}
