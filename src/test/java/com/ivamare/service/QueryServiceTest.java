package com.ivamare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.QueryDto;
import com.ivamare.dto.QueryFormDto;
import com.ivamare.repository.QueryRepository;
import com.ivamare.repository.QueryVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for QueryService. */
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @Mock private QueryRepository queryRepository;
  @Mock private QueryVersionRepository versionRepository;

  private QueryService queryService;

  @BeforeEach
  void setUp() {
    queryService = new QueryService(queryRepository, versionRepository);
  }

  @Test
  void getQueriesGroupedByCategory_returnsGroupedQueries() {
    Query q1 = createQuery("1", "Query1", "Category A");
    Query q2 = createQuery("2", "Query2", "Category A");
    Query q3 = createQuery("3", "Query3", "Category B");

    when(queryRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(q1, q2, q3));

    Map<String, List<QueryDto>> result = queryService.getQueriesGroupedByCategory();

    assertThat(result).hasSize(2);
    assertThat(result.get("Category A")).hasSize(2);
    assertThat(result.get("Category B")).hasSize(1);
  }

  @Test
  void getQuery_existingId_returnsQuery() {
    Query query = createQuery("test-id", "Test Query", "Test");
    when(queryRepository.findById("test-id")).thenReturn(Optional.of(query));

    Query result = queryService.getQuery("test-id");

    assertThat(result.getId()).isEqualTo("test-id");
    assertThat(result.getName()).isEqualTo("Test Query");
  }

  @Test
  void getQuery_nonExistingId_throwsEntityNotFoundException() {
    when(queryRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> queryService.getQuery("unknown"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Query not found: unknown");
  }

  @Test
  void createQuery_savesQueryAndVersion() {
    QueryFormDto form =
        QueryFormDto.builder()
            .name("New Query")
            .description("Description")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .configYaml("sql: SELECT 1")
            .build();

    when(queryRepository.save(any(Query.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Query result = queryService.createQuery(form, "admin");

    assertThat(result.getId()).isNotNull();
    assertThat(result.getName()).isEqualTo("New Query");
    assertThat(result.getCurrentVersion()).isEqualTo(1);
    assertThat(result.getIsActive()).isTrue();
    assertThat(result.getCreatedBy()).isEqualTo("admin");
    assertThat(result.getVersions()).hasSize(1);
    assertThat(result.getVersions().get(0).getConfigYaml()).isEqualTo("sql: SELECT 1");

    verify(queryRepository).save(any(Query.class));
  }

  @Test
  void updateQuery_updatesMetadataAndCreatesNewVersion() {
    Query existingQuery = createQuery("test-id", "Old Name", "Old Category");
    existingQuery.setCurrentVersion(1);

    QueryFormDto form =
        QueryFormDto.builder()
            .id("test-id")
            .name("New Name")
            .description("New Description")
            .category("New Category")
            .connectionName("new-conn")
            .queryType(QueryType.UPDATE_WORKFLOW)
            .configYaml("sql: UPDATE table SET x=1")
            .build();

    when(queryRepository.findById("test-id")).thenReturn(Optional.of(existingQuery));
    when(queryRepository.save(any(Query.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(versionRepository.save(any(QueryVersion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Query result = queryService.updateQuery("test-id", form, "editor");

    assertThat(result.getName()).isEqualTo("New Name");
    assertThat(result.getCategory()).isEqualTo("New Category");
    assertThat(result.getCurrentVersion()).isEqualTo(2);
    assertThat(result.getUpdatedBy()).isEqualTo("editor");

    ArgumentCaptor<QueryVersion> versionCaptor = ArgumentCaptor.forClass(QueryVersion.class);
    verify(versionRepository).save(versionCaptor.capture());
    QueryVersion savedVersion = versionCaptor.getValue();
    assertThat(savedVersion.getVersion()).isEqualTo(2);
    assertThat(savedVersion.getConfigYaml()).isEqualTo("sql: UPDATE table SET x=1");
  }

  @Test
  void deleteQuery_softDeletesQuery() {
    Query query = createQuery("test-id", "Test Query", "Test");
    query.setIsActive(true);

    when(queryRepository.findById("test-id")).thenReturn(Optional.of(query));
    when(queryRepository.save(any(Query.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    queryService.deleteQuery("test-id", "admin");

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    verify(queryRepository).save(queryCaptor.capture());
    Query savedQuery = queryCaptor.getValue();
    assertThat(savedQuery.getIsActive()).isFalse();
    assertThat(savedQuery.getUpdatedBy()).isEqualTo("admin");
  }

  @Test
  void getQueryForEdit_returnsFormWithCurrentVersion() {
    Query query = createQuery("test-id", "Test Query", "Test");
    query.setCurrentVersion(2);

    QueryVersion version =
        QueryVersion.builder()
            .id("v2")
            .query(query)
            .version(2)
            .configYaml("sql: SELECT * FROM table")
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();

    when(queryRepository.findById("test-id")).thenReturn(Optional.of(query));
    when(versionRepository.findByQueryIdAndVersion("test-id", 2)).thenReturn(Optional.of(version));

    QueryFormDto result = queryService.getQueryForEdit("test-id");

    assertThat(result.getId()).isEqualTo("test-id");
    assertThat(result.getName()).isEqualTo("Test Query");
    assertThat(result.getConfigYaml()).isEqualTo("sql: SELECT * FROM table");
  }

  @Test
  void getVersionHistory_returnsVersionsNewestFirst() {
    Query query = createQuery("test-id", "Test Query", "Test");
    QueryVersion v1 = QueryVersion.builder().version(1).build();
    QueryVersion v2 = QueryVersion.builder().version(2).build();

    when(queryRepository.findById("test-id")).thenReturn(Optional.of(query));
    when(versionRepository.findByQueryIdOrderByVersionDesc("test-id")).thenReturn(List.of(v2, v1));

    List<QueryVersion> result = queryService.getVersionHistory("test-id");

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getVersion()).isEqualTo(2);
    assertThat(result.get(1).getVersion()).isEqualTo(1);
  }

  @Test
  void queryNameExists_returnsTrue_whenNameExists() {
    when(queryRepository.existsByNameAndIsActiveTrue("Existing Query")).thenReturn(true);

    assertThat(queryService.queryNameExists("Existing Query")).isTrue();
  }

  @Test
  void queryNameExists_returnsFalse_whenNameDoesNotExist() {
    when(queryRepository.existsByNameAndIsActiveTrue("New Query")).thenReturn(false);

    assertThat(queryService.queryNameExists("New Query")).isFalse();
  }

  private Query createQuery(String id, String name, String category) {
    return Query.builder()
        .id(id)
        .name(name)
        .description("Test description")
        .category(category)
        .connectionName("test-conn")
        .queryType(QueryType.SELECT)
        .currentVersion(1)
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .createdBy("admin")
        .build();
  }
}
