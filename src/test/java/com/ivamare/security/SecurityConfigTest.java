package com.ivamare.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for security configuration. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void loginPage_shouldBeAccessibleWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/login")).andExpect(status().isOk());
  }

  @Test
  void dashboard_shouldRedirectToLoginWhenUnauthenticated() throws Exception {
    mockMvc
        .perform(get("/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void formLogin_withValidCredentials_shouldSucceed() throws Exception {
    mockMvc
        .perform(formLogin("/login").user("admin").password("admin"))
        .andExpect(authenticated().withUsername("admin"));
  }

  @Test
  void formLogin_withInvalidCredentials_shouldFail() throws Exception {
    mockMvc
        .perform(formLogin("/login").user("admin").password("wrongpassword"))
        .andExpect(unauthenticated());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminEndpoint_withAdminRole_shouldSucceed() throws Exception {
    mockMvc.perform(get("/admin")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(
      username = "reader",
      roles = {"SELECT_RUNNER"})
  void adminEndpoint_withSelectRunnerRole_shouldBeForbidden() throws Exception {
    mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(
      username = "updater",
      roles = {"UPDATE_RUNNER"})
  void adminEndpoint_withUpdateRunnerRole_shouldBeForbidden() throws Exception {
    mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "user")
  void logout_shouldInvalidateSession() throws Exception {
    mockMvc
        .perform(logout())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout=true"));
  }

  @Test
  @WithMockUser(
      username = "reader",
      roles = {"SELECT_RUNNER"})
  void dashboard_withAuthenticatedUser_shouldSucceed() throws Exception {
    mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void exportEndpoint_withAdminRole_shouldSucceed() throws Exception {
    mockMvc.perform(get("/api/export")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(
      username = "updater",
      roles = {"UPDATE_RUNNER"})
  void exportEndpoint_withUpdateRunnerRole_shouldBeForbidden() throws Exception {
    mockMvc.perform(get("/api/export")).andExpect(status().isForbidden());
  }
}
