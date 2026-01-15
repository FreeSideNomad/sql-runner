package com.ivamare.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for AuthController. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void loginPage_shouldDisplayLoginForm() throws Exception {
    mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  void loginPage_withError_shouldDisplayErrorMessage() throws Exception {
    mockMvc
        .perform(get("/login").param("error", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attributeExists("errorMessage"));
  }

  @Test
  void loginPage_withLogout_shouldDisplayLogoutMessage() throws Exception {
    mockMvc
        .perform(get("/login").param("logout", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attributeExists("logoutMessage"));
  }

  @Test
  void loginPage_withExpired_shouldDisplayExpiredMessage() throws Exception {
    mockMvc
        .perform(get("/login").param("expired", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attributeExists("expiredMessage"));
  }

  @Test
  @WithMockUser
  void homePage_shouldRedirectToDashboard() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"));
  }

  @Test
  @WithMockUser
  void dashboard_shouldDisplayDashboard() throws Exception {
    mockMvc
        .perform(get("/dashboard"))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"));
  }
}
