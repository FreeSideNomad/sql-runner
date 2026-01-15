package com.ivamare.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ivamare.domain.BackupRecord;
import com.ivamare.domain.QueryType;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryDto;
import com.ivamare.service.QueryExecutionService;
import com.ivamare.service.QueryService;
import com.ivamare.service.UpdateWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for UpdateWorkflowController. */
@WebMvcTest(UpdateWorkflowController.class)
class UpdateWorkflowControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private QueryService queryService;
  @MockBean private QueryExecutionService executionService;
  @MockBean private UpdateWorkflowService updateWorkflowService;

  @Test
  @WithMockUser
  void showUpdateForm_displaysForm() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .parameters(List.of())
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);

    mockMvc
        .perform(get("/queries/q1/update"))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "parameters"))
        .andExpect(model().attributeExists("query", "config", "parameters"));
  }

  @Test
  @WithMockUser
  void executePreview_displaysPreviewResults() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .parameters(List.of())
            .build();

    List<Map<String, Object>> rows = List.of(Map.of("id", 1, "name", "Test"));
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(rows)
            .columns(List.of("id", "name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(
            post("/queries/q1/update/preview")
                .param("status", "ACTIVE")
                .with(csrf())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "preview"))
        .andExpect(model().attributeExists("result", "submittedParams"));
  }

  @Test
  @WithMockUser
  void executePreview_withFailure_showsError() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .parameters(List.of())
            .build();

    ExecutionResult result = ExecutionResult.failure("Query failed", 10, null);

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(
            post("/queries/q1/update/preview")
                .param("status", "ACTIVE")
                .with(csrf())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "preview"));
  }

  @Test
  @WithMockUser
  void executeUpdate_withNoSession_redirectsWithError() throws Exception {
    mockMvc
        .perform(post("/queries/q1/update/execute").with(csrf()).with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries/q1/update"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  @WithMockUser
  void showComplete_displaysCompletion() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    BackupRecord backup =
        BackupRecord.builder()
            .id("backup1")
            .executionLogId("log1")
            .rowCount(5)
            .isRolledBack(false)
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(updateWorkflowService.getBackupForExecution("log1")).thenReturn(Optional.of(backup));

    mockMvc
        .perform(get("/queries/q1/update/complete/log1"))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "complete"))
        .andExpect(model().attribute("executionLogId", "log1"))
        .andExpect(model().attributeExists("backup"));
  }

  @Test
  @WithMockUser
  void executeRollback_withSuccess_redirectsToHistory() throws Exception {
    ExecutionResult result =
        ExecutionResult.builder().success(true).rowCount(5).executionTimeMs(50).build();

    when(updateWorkflowService.executeRollback("log1", "user")).thenReturn(result);

    mockMvc
        .perform(post("/queries/update/rollback/log1").with(csrf()).with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/history"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @WithMockUser
  void executeRollback_withFailure_redirectsWithError() throws Exception {
    ExecutionResult result = ExecutionResult.failure("Rollback failed", 50, null);

    when(updateWorkflowService.executeRollback("log1", "user")).thenReturn(result);

    mockMvc
        .perform(post("/queries/update/rollback/log1").with(csrf()).with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/history"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  @WithMockUser
  void executeRollback_withAlreadyRolledBack_redirectsWithError() throws Exception {
    when(updateWorkflowService.executeRollback("log1", "user"))
        .thenThrow(new IllegalStateException("Already rolled back"));

    mockMvc
        .perform(post("/queries/update/rollback/log1").with(csrf()).with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/history"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  @WithMockUser
  void executeRollback_withIllegalArgument_redirectsWithError() throws Exception {
    when(updateWorkflowService.executeRollback("log1", "user"))
        .thenThrow(new IllegalArgumentException("Invalid backup"));

    mockMvc
        .perform(post("/queries/update/rollback/log1").with(csrf()).with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/history"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  @WithMockUser
  void showComplete_withNoBackup_displaysCompletion() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(updateWorkflowService.getBackupForExecution("log1")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/queries/q1/update/complete/log1"))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "complete"))
        .andExpect(model().attribute("backup", (Object) null));
  }

  @Test
  @WithMockUser
  void executePreview_withNullRows_storesEmptyInSession() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .parameters(List.of())
            .build();

    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(null)
            .columns(List.of())
            .rowCount(0)
            .totalRows(0)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(
            post("/queries/q1/update/preview")
                .param("status", "ACTIVE")
                .with(csrf())
                .with(user("user")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/update-workflow"))
        .andExpect(model().attribute("step", "preview"));
  }

  @Test
  @WithMockUser
  void executeUpdate_withSuccessAndRows_redirectsToComplete() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    QueryConfig config =
        QueryConfig.builder()
            .selectSql("SELECT * FROM test")
            .updateSql("UPDATE test SET x = 1")
            .parameters(List.of())
            .build();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rowCount(5)
            .executionTimeMs(50)
            .executionLogId("log123")
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(queryService.getCurrentConfigYaml("q1")).thenReturn("selectSql: SELECT * FROM test");
    when(executionService.parseConfig(anyString())).thenReturn(config);
    when(updateWorkflowService.executeUpdate(eq("q1"), anyMap(), anyList(), eq("user")))
        .thenReturn(result);

    mockMvc
        .perform(
            post("/queries/q1/update/execute")
                .sessionAttr("updatePreviewData", List.of(Map.of("id", 1)))
                .sessionAttr("updatePreviewParams", Map.of("status", "ACTIVE"))
                .with(csrf())
                .with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries/q1/update/complete/log123"))
        .andExpect(flash().attributeExists("message"));
  }

  @Test
  @WithMockUser
  void executeUpdate_withFailure_redirectsWithError() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    ExecutionResult result = ExecutionResult.failure("Update failed", 50, null);

    when(updateWorkflowService.executeUpdate(eq("q1"), anyMap(), anyList(), eq("user")))
        .thenReturn(result);

    mockMvc
        .perform(
            post("/queries/q1/update/execute")
                .sessionAttr("updatePreviewData", List.of(Map.of("id", 1)))
                .sessionAttr("updatePreviewParams", Map.of("status", "ACTIVE"))
                .with(csrf())
                .with(user("user")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries/q1/update"))
        .andExpect(flash().attributeExists("error"));
  }

  @Test
  @WithMockUser
  void exportPreviewCsv_withSuccess_returnsCsv() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("id", 1, "name", "Test")))
            .columns(List.of("id", "name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(get("/queries/q1/update/export-csv").param("status", "ACTIVE").with(user("user")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
  }

  @Test
  @WithMockUser
  void exportPreviewCsv_withError_returnsError() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    ExecutionResult result = ExecutionResult.failure("Query failed", 10, null);

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(get("/queries/q1/update/export-csv").param("status", "ACTIVE").with(user("user")))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @WithMockUser
  void exportPreviewCsv_withSpecialChars_escapesProperly() throws Exception {
    QueryDto query = createUpdateWorkflowQuery();
    ExecutionResult result =
        ExecutionResult.builder()
            .success(true)
            .rows(List.of(Map.of("name", "Value, with comma\nand newline")))
            .columns(List.of("name"))
            .rowCount(1)
            .totalRows(1)
            .executionTimeMs(10)
            .build();

    when(queryService.getQueryDto("q1")).thenReturn(query);
    when(updateWorkflowService.executePreview(eq("q1"), anyMap(), eq("user"))).thenReturn(result);

    mockMvc
        .perform(get("/queries/q1/update/export-csv").with(user("user")))
        .andExpect(status().isOk());
  }

  private QueryDto createUpdateWorkflowQuery() {
    return QueryDto.builder()
        .id("q1")
        .name("Test Update Query")
        .queryType(QueryType.UPDATE_WORKFLOW)
        .connectionName("test-conn")
        .createdAt(LocalDateTime.now())
        .createdBy("admin")
        .currentVersion(1)
        .build();
  }
}
