package com.ivamare.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.repository.QueryRepository;
import java.util.Collections;
import java.util.List;
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

  @MockBean private QueryRepository queryRepository;

  @Test
  void listQueries_shouldReturnQueriesPage() throws Exception {
    Query query =
        Query.builder()
            .id("query-1")
            .name("Test Query")
            .category("Test")
            .connectionName("test-conn")
            .queryType(QueryType.SELECT)
            .build();

    when(queryRepository.findByIsActiveTrue()).thenReturn(List.of(query));
    when(queryRepository.findDistinctCategories()).thenReturn(List.of("Test"));

    mockMvc
        .perform(get("/queries").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/list"))
        .andExpect(model().attributeExists("queries"))
        .andExpect(model().attributeExists("categories"));
  }

  @Test
  void listQueries_withEmptyList_shouldReturnEmptyPage() throws Exception {
    when(queryRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
    when(queryRepository.findDistinctCategories()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/queries").with(user("testuser").roles("SELECT_RUNNER")))
        .andExpect(status().isOk())
        .andExpect(view().name("queries/list"));
  }
}
