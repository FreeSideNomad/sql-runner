package com.ivamare.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.ivamare.dto.QueryDto;
import com.ivamare.dto.QueryFormDto;
import com.ivamare.service.ConnectionRegistry;
import com.ivamare.service.QueryService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for QueryController. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private QueryService queryService;
  @MockBean private ConnectionRegistry connectionRegistry;

  @Test
  void listQueries_shouldReturnQueriesPage() throws Exception {
    QueryDto query =
        QueryDto.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .build();

    when(queryService.getQueriesGroupedByCategory()).thenReturn(Map.of("Test", List.of(query)));

    mockMvc
        .perform(get("/queries").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/list"))
        .andExpect(model().attributeExists("queriesByCategory"));
  }

  @Test
  void listQueries_withEmptyList_shouldReturnEmptyPage() throws Exception {
    when(queryService.getQueriesGroupedByCategory()).thenReturn(Collections.emptyMap());

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

  @Test
  void newQueryForm_withoutAdminRole_shouldReturnForbidden() throws Exception {
    mockMvc
        .perform(get("/queries/new").with(user("user").roles("SELECT_RUNNER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void editQueryForm_withAdminRole_shouldReturnFormWithData() throws Exception {
    QueryFormDto form =
        QueryFormDto.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .configYaml("sql: SELECT 1")
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
                .param("configYaml", "sql: SELECT 1"))
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
                .param("configYaml", "sql: SELECT 1"))
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
}
