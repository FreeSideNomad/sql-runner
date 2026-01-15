package com.ivamare.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Tests for GlobalControllerAdvice. */
class GlobalControllerAdviceTest {

  private GlobalControllerAdvice advice;

  @BeforeEach
  void setUp() {
    advice = new GlobalControllerAdvice();
  }

  @Test
  void getUserRole_withAdminRole_shouldReturnAdmin() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    Collection<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    when(auth.getAuthorities()).thenReturn((Collection) authorities);

    String role = advice.getUserRole(auth);

    assertThat(role).isEqualTo("ADMIN");
  }

  @Test
  void getUserRole_withUpdateRunnerRole_shouldReturnUpdateRunner() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    Collection<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_UPDATE_RUNNER"));
    when(auth.getAuthorities()).thenReturn((Collection) authorities);

    String role = advice.getUserRole(auth);

    assertThat(role).isEqualTo("UPDATE_RUNNER");
  }

  @Test
  void getUserRole_withSelectRunnerRole_shouldReturnSelectRunner() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    Collection<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELECT_RUNNER"));
    when(auth.getAuthorities()).thenReturn((Collection) authorities);

    String role = advice.getUserRole(auth);

    assertThat(role).isEqualTo("SELECT_RUNNER");
  }

  @Test
  void getUserRole_withNoMatchingRole_shouldReturnUser() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    Collection<GrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_OTHER"));
    when(auth.getAuthorities()).thenReturn((Collection) authorities);

    String role = advice.getUserRole(auth);

    assertThat(role).isEqualTo("USER");
  }

  @Test
  void getUserRole_withNullAuth_shouldReturnNull() {
    String role = advice.getUserRole(null);

    assertThat(role).isNull();
  }

  @Test
  void getUserRole_withUnauthenticated_shouldReturnNull() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);

    String role = advice.getUserRole(auth);

    assertThat(role).isNull();
  }

  @Test
  void getCurrentPage_withDashboardPath_shouldReturnDashboard() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/dashboard");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("dashboard");
  }

  @Test
  void getCurrentPage_withRootPath_shouldReturnDashboard() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("dashboard");
  }

  @Test
  void getCurrentPage_withQueriesPath_shouldReturnQueries() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/queries");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("queries");
  }

  @Test
  void getCurrentPage_withHistoryPath_shouldReturnHistory() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/history");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("history");
  }

  @Test
  void getCurrentPage_withAdminConnectionsPath_shouldReturnConnections() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/admin/connections");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("connections");
  }

  @Test
  void getCurrentPage_withAdminQueriesPath_shouldReturnAdminQueries() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/admin/queries");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("admin-queries");
  }

  @Test
  void getCurrentPage_withAdminImportExportPath_shouldReturnImportExport() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/admin/import-export");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEqualTo("import-export");
  }

  @Test
  void getCurrentPage_withUnknownPath_shouldReturnEmpty() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/unknown/path");

    String page = advice.getCurrentPage(request);

    assertThat(page).isEmpty();
  }
}
