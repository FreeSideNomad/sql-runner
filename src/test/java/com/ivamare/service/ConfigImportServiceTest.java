package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.service.ConfigImportService.ImportResult;
import com.ivamare.service.ConfigImportService.ImportValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Tests for ConfigImportService. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfigImportServiceTest {

  @Autowired private ConfigImportService importService;

  @Test
  void validateImport_rejectsInvalidYaml() {
    ImportValidationResult result = importService.validateImport("not valid yaml: [");

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void validateImport_rejectsMissingFormatVersion() {
    String yaml = """
        exportedBy: admin
        queries: []
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("format version"));
  }

  @Test
  void validateImport_acceptsValidYaml() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: test-id-123
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                createdBy: admin
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.queryCount()).isEqualTo(1);
  }

  @Test
  void validateImport_detectsDuplicateIds() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: same-id
            name: Query 1
            category: Test
            connectionName: conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
          - id: same-id
            name: Query 2
            category: Test
            connectionName: conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 2"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("Duplicate"));
  }

  @Test
  void importQueries_createsNewQueries() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: new-import-query-456
            name: Imported Query
            description: A test import
            category: Import Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                createdBy: admin
                config: "sql: SELECT 1"
        """;

    ImportResult result = importService.importQueries(yaml, "testuser");

    assertThat(result.success()).isTrue();
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isEqualTo(0);
  }

  @Test
  void importQueries_rejectsInvalidYaml() {
    ImportResult result = importService.importQueries("invalid yaml [", "admin");

    assertThat(result.success()).isFalse();
    assertThat(result.error()).contains("parse");
  }

  @Test
  void validateImport_rejectsUnsupportedFormatVersion() {
    String yaml =
        """
        formatVersion: "2.0"
        exportedBy: admin
        queries:
          - id: test-id
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("Unsupported"));
  }

  @Test
  void validateImport_detectsMissingQueryId() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: ""
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("missing ID"));
  }

  @Test
  void validateImport_detectsMissingQueryName() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: test-id
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("missing name"));
  }

  @Test
  void validateImport_detectsMissingQueryType() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: test-id
            name: Test Query
            category: Test
            connectionName: test-conn
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("missing type"));
  }

  @Test
  void validateImport_detectsMissingVersions() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: test-id
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anyMatch(e -> e.contains("no versions"));
  }

  @Test
  void validateImport_warnsOnEmptyQueries() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries: []
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).anyMatch(w -> w.contains("No queries"));
  }

  @Test
  void validateImport_withIsoDateFormat_acceptsValidDates() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        exportedAt: 2024-01-15T10:30:00
        queries:
          - id: test-id-iso
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                createdAt: 2024-01-15T10:30:00
                createdBy: admin
                config: "sql: SELECT 1"
        """;

    ImportValidationResult result = importService.validateImport(yaml);

    assertThat(result.isValid()).isTrue();
  }

  @Test
  void importQueries_updatesExistingQuery() {
    // First create a query
    String yaml1 =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: update-test-query
            name: Original Name
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                createdBy: admin
                config: "sql: SELECT 1"
        """;
    importService.importQueries(yaml1, "admin");

    // Now import with newer version
    String yaml2 =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: update-test-query
            name: Updated Name
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 2
            versions:
              - version: 1
                createdBy: admin
                config: "sql: SELECT 1"
              - version: 2
                createdBy: admin
                config: "sql: SELECT 2"
        """;

    ImportResult result = importService.importQueries(yaml2, "admin");

    assertThat(result.success()).isTrue();
    assertThat(result.updated()).isEqualTo(1);
  }

  @Test
  void importQueries_skipsOlderVersion() {
    // First create a query with version 2
    String yaml1 =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: skip-test-query
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 2
            versions:
              - version: 1
                config: "sql: SELECT 1"
              - version: 2
                config: "sql: SELECT 2"
        """;
    importService.importQueries(yaml1, "admin");

    // Try to import with older/same version
    String yaml2 =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: skip-test-query
            name: Test Query
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 2
            versions:
              - version: 1
                config: "sql: SELECT 1"
              - version: 2
                config: "sql: SELECT 2"
        """;

    ImportResult result = importService.importQueries(yaml2, "admin");

    assertThat(result.success()).isTrue();
    assertThat(result.skipped()).isEqualTo(1);
  }

  @Test
  void importQueries_failsOnValidationErrors() {
    String yaml =
        """
        formatVersion: "1.0"
        exportedBy: admin
        queries:
          - id: same-id-dup
            name: Query 1
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 1"
          - id: same-id-dup
            name: Query 2
            category: Test
            connectionName: test-conn
            queryType: SELECT
            currentVersion: 1
            versions:
              - version: 1
                config: "sql: SELECT 2"
        """;

    ImportResult result = importService.importQueries(yaml, "admin");

    assertThat(result.success()).isFalse();
    assertThat(result.error()).contains("Duplicate");
  }
}
