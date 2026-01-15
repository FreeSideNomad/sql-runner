package com.ivamare.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ivamare.domain.DatabaseType;
import com.ivamare.domain.Query;
import com.ivamare.domain.QueryType;
import com.ivamare.dto.ConnectionInfo;
import com.ivamare.repository.QueryRepository;
import com.ivamare.service.ConfigExportService;
import com.ivamare.service.ConfigImportService;
import com.ivamare.service.ConfigImportService.ImportResult;
import com.ivamare.service.ConfigImportService.ImportValidationResult;
import com.ivamare.service.ConnectionRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for AdminController. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ConfigExportService exportService;
  @MockBean private ConfigImportService importService;
  @MockBean private ConnectionRegistry connectionRegistry;
  @MockBean private QueryRepository queryRepository;

  @Test
  void adminDashboard_withAdminRole_shouldReturnAdminPage() throws Exception {
    mockMvc
        .perform(get("/admin").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/index"))
        .andExpect(model().attribute("pageTitle", "Administration"));
  }

  @Test
  void adminDashboard_withoutAdminRole_shouldReturnForbidden() throws Exception {
    mockMvc
        .perform(get("/admin").with(user("user").roles("SELECT_RUNNER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void exportPage_shouldReturnExportPage() throws Exception {
    mockMvc
        .perform(get("/admin/export").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/export"))
        .andExpect(model().attribute("pageTitle", "Export Configuration"));
  }

  @Test
  void downloadExport_shouldReturnYamlFile() throws Exception {
    String yaml = "formatVersion: '1.0'\nqueries: []";
    when(exportService.exportAll("admin")).thenReturn(yaml);

    mockMvc
        .perform(get("/admin/export/download").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/x-yaml; charset=UTF-8"))
        .andExpect(
            header()
                .string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
        .andExpect(content().string(yaml));

    verify(exportService).exportAll("admin");
  }

  @Test
  void importPage_shouldReturnImportPage() throws Exception {
    mockMvc
        .perform(get("/admin/import").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/import"))
        .andExpect(model().attribute("pageTitle", "Import Configuration"));
  }

  @Test
  void validateImport_withEmptyFile_shouldRedirectWithError() throws Exception {
    MockMultipartFile emptyFile =
        new MockMultipartFile("file", "", "application/x-yaml", new byte[0]);

    mockMvc
        .perform(
            multipart("/admin/import/validate")
                .file(emptyFile)
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/import"))
        .andExpect(flash().attribute("error", "Please select a file to import"));
  }

  @Test
  void validateImport_withValidFile_shouldReturnValidationPage() throws Exception {
    String yaml = "formatVersion: '1.0'\nqueries: []";
    MockMultipartFile file =
        new MockMultipartFile("file", "config.yaml", "application/x-yaml", yaml.getBytes());

    ImportValidationResult validation = new ImportValidationResult(true, List.of(), List.of(), 0);
    when(importService.validateImport(yaml)).thenReturn(validation);

    mockMvc
        .perform(
            multipart("/admin/import/validate")
                .file(file)
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/import-validate"))
        .andExpect(model().attributeExists("validation"))
        .andExpect(model().attribute("yamlContent", yaml))
        .andExpect(model().attribute("filename", "config.yaml"));

    verify(importService).validateImport(yaml);
  }

  @Test
  void executeImport_withSuccessfulImport_shouldRedirectWithSuccessMessage() throws Exception {
    String yaml = "formatVersion: '1.0'\nqueries: []";
    ImportResult result =
        ImportResult.success(2, 1, 0, List.of("Created: Query A", "Updated: Query B"));
    when(importService.importQueries(yaml, "admin")).thenReturn(result);

    mockMvc
        .perform(
            post("/admin/import/execute")
                .param("yamlContent", yaml)
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/import"))
        .andExpect(flash().attributeExists("message"))
        .andExpect(flash().attributeExists("importMessages"));

    verify(importService).importQueries(yaml, "admin");
  }

  @Test
  void executeImport_withFailedImport_shouldRedirectWithError() throws Exception {
    String yaml = "invalid yaml";
    ImportResult result = ImportResult.failure("Parse error");
    when(importService.importQueries(yaml, "admin")).thenReturn(result);

    mockMvc
        .perform(
            post("/admin/import/execute")
                .param("yamlContent", yaml)
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/import"))
        .andExpect(flash().attribute("error", "Import failed: Parse error"));
  }

  @Test
  void listConnections_shouldReturnConnectionsPage() throws Exception {
    ConnectionInfo conn =
        ConnectionInfo.builder()
            .id("test-conn")
            .name("Test Connection")
            .type(DatabaseType.H2)
            .host("localhost")
            .build();
    when(connectionRegistry.listConnections()).thenReturn(List.of(conn));

    mockMvc
        .perform(get("/admin/connections").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/connections"))
        .andExpect(model().attributeExists("connections"));

    verify(connectionRegistry).listConnections();
  }

  @Test
  void testConnection_withSuccessfulConnection_shouldRedirectWithSuccessMessage() throws Exception {
    ConnectionInfo connResult =
        ConnectionInfo.builder().id("test-conn").name("Test Connection").connected(true).build();
    when(connectionRegistry.testConnection("test-conn")).thenReturn(connResult);

    mockMvc
        .perform(
            post("/admin/connections/test-conn/test")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/connections"))
        .andExpect(flash().attribute("message", "Connection 'test-conn' is working"));

    verify(connectionRegistry).testConnection("test-conn");
  }

  @Test
  void testConnection_withFailedConnection_shouldRedirectWithError() throws Exception {
    ConnectionInfo connResult =
        ConnectionInfo.builder()
            .id("test-conn")
            .name("Test Connection")
            .connected(false)
            .errorMessage("Connection refused")
            .build();
    when(connectionRegistry.testConnection("test-conn")).thenReturn(connResult);

    mockMvc
        .perform(
            post("/admin/connections/test-conn/test")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/connections"))
        .andExpect(flash().attribute("error", "Connection 'test-conn' failed: Connection refused"));
  }

  @Test
  void adminQueries_shouldReturnQueriesPage() throws Exception {
    Query query =
        Query.builder()
            .id("query-1")
            .name("Test Query")
            .queryType(QueryType.SELECT)
            .isActive(true)
            .currentVersion(1)
            .createdAt(LocalDateTime.now())
            .createdBy("admin")
            .build();
    when(queryRepository.findAll()).thenReturn(List.of(query));

    mockMvc
        .perform(get("/admin/queries").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/queries"))
        .andExpect(model().attributeExists("queries"));

    verify(queryRepository).findAll();
  }

  @Test
  void importExportPage_shouldReturnImportExportPage() throws Exception {
    mockMvc
        .perform(get("/admin/import-export").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/import-export"))
        .andExpect(model().attribute("pageTitle", "Import/Export Configuration"));
  }
}
