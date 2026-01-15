package com.ivamare.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Global controller advice for adding common model attributes. */
@ControllerAdvice
public class GlobalControllerAdvice {

  /**
   * Adds the current user's highest role to the model.
   *
   * @param auth the current authentication
   * @return the role name (ADMIN, UPDATE_RUNNER, or SELECT_RUNNER)
   */
  @ModelAttribute("userRole")
  public String getUserRole(Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }

    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

    if (hasRole(authorities, "ROLE_ADMIN")) {
      return "ADMIN";
    } else if (hasRole(authorities, "ROLE_UPDATE_RUNNER")) {
      return "UPDATE_RUNNER";
    } else if (hasRole(authorities, "ROLE_SELECT_RUNNER")) {
      return "SELECT_RUNNER";
    }

    return "USER";
  }

  /**
   * Adds the current page identifier to the model for navigation highlighting.
   *
   * @param request the HTTP request
   * @return the current page identifier
   */
  @ModelAttribute("currentPage")
  public String getCurrentPage(HttpServletRequest request) {
    String path = request.getRequestURI();

    if (path.equals("/") || path.equals("/dashboard")) {
      return "dashboard";
    } else if (path.startsWith("/queries") && !path.startsWith("/admin")) {
      return "queries";
    } else if (path.startsWith("/history")) {
      return "history";
    } else if (path.startsWith("/admin/connections")) {
      return "connections";
    } else if (path.startsWith("/admin/queries")) {
      return "admin-queries";
    } else if (path.startsWith("/admin/import-export")) {
      return "import-export";
    } else if (path.startsWith("/admin")) {
      return "admin";
    }

    return "";
  }

  private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
    return authorities.stream().anyMatch(a -> a.getAuthority().equals(role));
  }
}
