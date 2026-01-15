package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.repository.QueryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Tests for ConfigExportService. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfigExportServiceTest {

  @Autowired private ConfigExportService exportService;
  @Autowired private QueryRepository queryRepository;

  @Test
  void exportAll_includesFormatVersion() {
    String yaml = exportService.exportAll("admin");

    assertThat(yaml).contains("formatVersion:");
    assertThat(yaml).contains("1.0");
  }

  @Test
  void exportAll_includesExportedBy() {
    String yaml = exportService.exportAll("testuser");

    assertThat(yaml).contains("exportedBy: testuser");
  }

  @Test
  void exportAll_includesExportedAt() {
    String yaml = exportService.exportAll("admin");

    assertThat(yaml).contains("exportedAt:");
  }

  @Test
  void exportAll_includesQueries() {
    // Create a test query
    Query query =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Test Export Query")
            .description("Query for export test")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdBy("admin")
            .build();

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(1)
            .configYaml("sql: SELECT 1")
            .createdBy("admin")
            .build();

    query.getVersions().add(version);
    queryRepository.save(query);

    String yaml = exportService.exportAll("admin");

    assertThat(yaml).contains("name: Test Export Query");
    assertThat(yaml).contains("category: Test");
    assertThat(yaml).contains("queryType: SELECT");
  }

  @Test
  void exportQuery_exportsSingleQuery() {
    Query query =
        Query.builder()
            .id(UUID.randomUUID().toString())
            .name("Single Export Query")
            .description("Single query export")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdBy("admin")
            .build();

    QueryVersion version =
        QueryVersion.builder()
            .id(UUID.randomUUID().toString())
            .query(query)
            .version(1)
            .configYaml("sql: SELECT 2")
            .createdBy("admin")
            .build();

    query.getVersions().add(version);
    queryRepository.save(query);

    String yaml = exportService.exportQuery(query.getId(), "admin");

    assertThat(yaml).contains("name: Single Export Query");
    assertThat(yaml).contains("formatVersion:");
  }
}
