package com.ivamare.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.domain.ExecutionType;
import com.ivamare.repository.ExecutionLogRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for HistoryController. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ExecutionLogRepository logRepository;

  @Test
  void listHistory_shouldReturnHistoryPage() throws Exception {
    ExecutionLog log =
        ExecutionLog.builder()
            .id("log-1")
            .queryId("query-1")
            .queryVersion(1)
            .connectionName("test-conn")
            .executedBy("testuser")
            .executedAt(LocalDateTime.now())
            .status(ExecutionStatus.SUCCESS)
            .executionType(ExecutionType.SELECT)
            .executionTimeMs(100L)
            .build();

    Page<ExecutionLog> page = new PageImpl<>(List.of(log));
    when(logRepository.findWithFilters(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(get("/history").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("history/list"))
        .andExpect(model().attributeExists("logs"))
        .andExpect(model().attributeExists("page"))
        .andExpect(model().attributeExists("size"));
  }

  @Test
  void listHistory_withFilters_shouldPassFilters() throws Exception {
    Page<ExecutionLog> page = new PageImpl<>(Collections.emptyList());
    when(logRepository.findWithFilters(
            eq("admin"),
            any(),
            eq(ExecutionStatus.FAILED),
            any(),
            any(),
            any(),
            any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/history")
                .param("user", "admin")
                .param("status", "FAILED")
                .with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("history/list"))
        .andExpect(model().attribute("user", "admin"))
        .andExpect(model().attribute("status", "FAILED"));
  }

  @Test
  void listHistory_withDateFilters_shouldPassDateRange() throws Exception {
    Page<ExecutionLog> page = new PageImpl<>(Collections.emptyList());
    when(logRepository.findWithFilters(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/history")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31")
                .with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("history/list"))
        .andExpect(model().attribute("startDate", "2024-01-01"))
        .andExpect(model().attribute("endDate", "2024-12-31"));
  }

  @Test
  void listHistory_withPagination_shouldHandlePaging() throws Exception {
    Page<ExecutionLog> page = new PageImpl<>(Collections.emptyList());
    when(logRepository.findWithFilters(
            any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(page);

    mockMvc
        .perform(
            get("/history")
                .param("page", "2")
                .param("size", "50")
                .with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("history/list"))
        .andExpect(model().attribute("page", 2))
        .andExpect(model().attribute("size", 50));
  }
}
