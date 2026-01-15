package com.ivamare.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for read-only mode behavior. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sqlrunner.read-only-mode=true")
class ReadOnlyModeTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void newQueryForm_inReadOnlyMode_shouldRedirectWithError() throws Exception {
    mockMvc
        .perform(get("/queries/new").with(user("admin").roles("ADMIN")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("error", "Cannot create queries in read-only mode"));
  }

  @Test
  void editQueryForm_inReadOnlyMode_shouldRedirectWithError() throws Exception {
    mockMvc
        .perform(get("/queries/some-id/edit").with(user("admin").roles("ADMIN")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("error", "Cannot edit queries in read-only mode"));
  }

  @Test
  void saveQuery_inReadOnlyMode_shouldRedirectWithError() throws Exception {
    mockMvc
        .perform(
            post("/queries/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "Test Query")
                .param("category", "Test")
                .param("connectionName", "test-conn")
                .param("queryType", "SELECT"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("error", "Cannot save queries in read-only mode"));
  }

  @Test
  void deleteQuery_inReadOnlyMode_shouldRedirectWithError() throws Exception {
    mockMvc
        .perform(get("/queries/some-id/delete").with(user("admin").roles("ADMIN")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/queries"))
        .andExpect(flash().attribute("error", "Cannot delete queries in read-only mode"));
  }

  @Test
  void importPage_inReadOnlyMode_shouldRedirectWithError() throws Exception {
    mockMvc
        .perform(get("/admin/import").with(user("admin").roles("ADMIN")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/import-export"))
        .andExpect(flash().attribute("error", "Import is disabled in read-only mode"));
  }

  @Test
  void importExportPage_inReadOnlyMode_shouldShowReadOnlyIndicator() throws Exception {
    mockMvc
        .perform(get("/admin/import-export").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(model().attribute("readOnlyMode", true));
  }

  @Test
  void listQueries_inReadOnlyMode_shouldShowReadOnlyModeAttribute() throws Exception {
    mockMvc
        .perform(get("/queries").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(model().attribute("readOnlyMode", true));
  }
}
