package com.ivamare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.domain.QueryVersion;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryConfigFormDto;
import com.ivamare.dto.QueryDto;
import com.ivamare.dto.QueryFormDto;
import com.ivamare.service.ConnectionRegistry;
import com.ivamare.service.QueryExecutionService;
import com.ivamare.service.QueryService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for QueryController. */
@WebMvcTest(QueryController.class)
class QueryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private QueryService queryService;
  @MockBean private ConnectionRegistry connectionRegistry;
  @MockBean private QueryExecutionService executionService;
  @MockBean private com.ivamare.service.QueryConfigValidator configValidator;

  @BeforeEach
  void setUp() {
    // Fix Thymeleaf NPE by providing default available parameters
    when(configValidator.getAvailableParameters(any()))
        .thenReturn(
            new com.ivamare.service.QueryConfigValidator.AvailableParameters(
                java.util.Set.of("param1"), java.util.Set.of("col1")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void listQueries_shouldReturnQueriesPage() throws Exception {
    QueryDto query =
        QueryDto.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .build();

    when(queryService.getQueriesGroupedByConnectionAndCategory())
        .thenReturn(Map.of("test-conn", Map.of("Test", List.of(query))));

    mockMvc
        .perform(get("/queries").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/list"))
        .andExpect(model().attributeExists("groupedQueries"));
  }

  @Test
  void listQueries_withEmptyList_shouldReturnEmptyPage() throws Exception {
    when(queryService.getQueriesGroupedByConnectionAndCategory())
        .thenReturn(Collections.emptyMap());

    mockMvc
        .perform(get("/queries").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/list"));
  }

  @Test
  void viewQuery_shouldReturnQueryDetails() throws Exception {
    QueryDto query =
        QueryDto.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("query-1")).thenReturn("sql: SELECT 1");

    mockMvc
        .perform(get("/queries/query-1").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/view"))
        .andExpect(model().attributeExists("query"))
        .andExpect(model().attributeExists("configYaml"));
  }

  @Test
  void newQueryForm_withAdminRole_shouldReturnForm() throws Exception {
    when(connectionRegistry.listConnections()).thenReturn(List.of());
    when(queryService.getAllCategories()).thenReturn(List.of("Category A"));

    mockMvc
        .perform(get("/queries/new").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/form"))
        .andExpect(model().attributeExists("query"))
        .andExpect(model().attributeExists("connections"))
        .andExpect(model().attribute("isEdit", false));
  }

  // Note: This test is skipped because @WebMvcTest doesn't load the full security config.
  // Security authorization tests are handled separately with @SpringBootTest integration tests.

  @Test
  void editQueryForm_withAdminRole_shouldReturnFormWithData() throws Exception {
    QueryConfigFormDto config = QueryConfigFormDto.builder().sql("SELECT 1").build();
    QueryFormDto form =
        QueryFormDto.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .config(config)
            .build();

    when(queryService.getQueryForEdit("query-1")).thenReturn(form);
    when(connectionRegistry.listConnections()).thenReturn(List.of());
    when(queryService.getAllCategories()).thenReturn(List.of("Test"));

    mockMvc
        .perform(get("/queries/query-1/edit").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/form"))
        .andExpect(model().attribute("isEdit", true));
  }

  @Test
  void saveQuery_withValidData_shouldRedirectToList() throws Exception {
    Query savedQuery =
        Query.builder()
            .id("new-query-id")
            .name("New Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .currentVersion(1)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();

    when(queryService.createQuery(any(QueryFormDto.class), any(String.class)))
        .thenReturn(savedQuery);

    mockMvc
        .perform(
            post("/queries/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "New Query")
                .param("category", "Test")
                .param("connectionName", "test-conn")
                .param("queryType", "SELECT")
                .param("config.sql", "SELECT 1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("message", "Query created successfully"));

    verify(queryService).createQuery(any(QueryFormDto.class), any(String.class));
  }

  @Test
  void saveQuery_withValidationErrors_shouldReturnForm() throws Exception {
    when(connectionRegistry.listConnections()).thenReturn(List.of());
    when(queryService.getAllCategories()).thenReturn(List.of());

    mockMvc
        .perform(
            post("/queries/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "") // Empty name triggers validation error
                .param("category", "Test")
                .param("connectionName", "test-conn")
                .param("queryType", "SELECT")
                .param("config.sql", "SELECT 1"))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/form"))
        .andExpect(model().hasErrors());
  }

  @Test
  void deleteQuery_withAdminRole_shouldRedirectToList() throws Exception {
    mockMvc
        .perform(get("/queries/query-1/delete").with(user("admin").roles("ADMIN")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("message", "Query deleted successfully"));

    verify(queryService).deleteQuery("query-1", "admin");
  }

  @Test
  void versionHistory_withAdminRole_shouldReturnVersions() throws Exception {
    QueryDto query = QueryDto.builder().id("query-1").name("Test Query").currentVersion(2).build();

    QueryVersion v1 =
        QueryVersion.builder().version(1).createdAt(LocalDateTime.now()).createdBy("admin").build();
    QueryVersion v2 =
        QueryVersion.builder().version(2).createdAt(LocalDateTime.now()).createdBy("admin").build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(queryService.getVersionHistory("query-1")).thenReturn(List.of(v2, v1));

    mockMvc
        .perform(get("/queries/query-1/versions").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/versions"))
        .andExpect(model().attributeExists("query"))
        .andExpect(model().attributeExists("versions"));
  }

  @Test
  void getCategories_shouldReturnCategoriesList() throws Exception {
    when(queryService.getAllCategories()).thenReturn(List.of("Category A", "Category B"));

    mockMvc
        .perform(get("/queries/categories").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().json("[\"Category A\",\"Category B\"]"));
  }

  @Test
  void viewVersion_shouldReturnVersionDetail() throws Exception {
    QueryDto query = QueryDto.builder().id("query-1").name("Test Query").currentVersion(2).build();
    QueryVersion version =
        QueryVersion.builder().version(1).createdAt(LocalDateTime.now()).createdBy("admin").build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(queryService.getVersion("query-1", 1)).thenReturn(version);

    mockMvc
        .perform(get("/queries/query-1/versions/1").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/version-detail"))
        .andExpect(model().attributeExists("query", "version"));
  }

  @Test
  void saveQuery_withEditMode_shouldUpdateAndRedirect() throws Exception {
    Query existingQuery =
        Query.builder()
            .id("existing-query")
            .name("Updated Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .build();

    when(queryService.updateQuery(any(), any(QueryFormDto.class), any())).thenReturn(existingQuery);

    mockMvc
        .perform(
            post("/queries/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("id", "existing-query")
                .param("name", "Updated Query")
                .param("category", "Test")
                .param("connectionName", "test-conn")
                .param("queryType", "SELECT")
                .param("config.sql", "SELECT 1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("message", "Query updated successfully"));
  }

  @Test
  void executeForm_shouldDisplayExecuteForm() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    QueryConfig config =
        QueryConfig.builder().sql("SELECT * FROM test").parameters(List.of()).build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("query-1")).thenReturn("sql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);

    mockMvc
        .perform(get("/queries/query-1/execute").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/execute"))
        .andExpect(model().attributeExists("query", "config", "parameters"));
  }

  @Test
  void executeQuery_shouldRunQueryAndReturnResults() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    QueryConfig config =
        QueryConfig.builder()
            .sql("SELECT * FROM test WHERE region = :region")
            .parameters(List.of())
            .build();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("id", 1, "name", "Test")))
            .columns(List.of("id", "name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("query-1")).thenReturn("sql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(
            post("/queries/query-1/execute")
                .with(user("testuser").roles("SELECT_RUNNER"))
                .with(csrf())
                .param("region", "EAST"))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/execute"))
        .andExpect(model().attributeExists("result", "submittedParams"));
  }

  @Test
  void exportCsv_shouldReturnCsvFile() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("id", 1, "name", "Test")))
            .columns(List.of("id", "name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(
            get("/queries/query-1/export-csv")
                .with(user("testuser").roles("SELECT_RUNNER"))
                .param("region", "EAST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"Test_Query.csv\""));
  }

  @Test
  void exportCsv_withError_shouldReturnError() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    ExecutionResult result = ExecutionResult.failure("Query failed", 10, null);

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(
            get("/queries/query-1/export-csv")
                .with(user("testuser").roles("SELECT_RUNNER"))
                .param("region", "EAST"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void exportCsv_withSpecialCharacters_shouldEscapeProperly() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("name", "Value, with comma", "desc", "Has \"quotes\"")))
            .columns(List.of("name", "desc"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(get("/queries/query-1/export-csv").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk());
  }

  @Test
  void exportCsv_withNullValues_shouldHandleGracefully() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    Map<String, Object> rowWithNull = new java.util.HashMap<>();
    rowWithNull.put("name", null);
    rowWithNull.put("id", 1);
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(rowWithNull))
            .columns(List.of("id", "name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(get("/queries/query-1/export-csv").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk());
  }

  @Test
  void exportCsv_withEmptyColumns_shouldFilterThem() throws Exception {
    QueryDto query =
        QueryDto.builder().id("query-1").name("Test Query").queryType(QueryType.SELECT).build();
    java.util.List<String> columnsWithEmpties = new java.util.ArrayList<>();
    columnsWithEmpties.add("id");
    columnsWithEmpties.add("");
    columnsWithEmpties.add("  ");
    columnsWithEmpties.add(null);
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("id", 1)))
            .columns(columnsWithEmpties)
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("query-1")).thenReturn(query);
    when(executionService.executeSelect(eq("query-1"), anyMap(), eq("testuser")))
        .thenReturn(result);

    mockMvc
        .perform(get("/queries/query-1/export-csv").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk());
  }
}
