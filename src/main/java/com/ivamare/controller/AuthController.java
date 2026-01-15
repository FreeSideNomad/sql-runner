package com.ivamare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controller for authentication-related pages. */
@Controller
public class AuthController {

  @GetMapping("/login")
  public String login(
      @RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "logout", required = false) String logout,
      @RequestParam(value = "expired", required = false) String expired,
      Model model) {

    if (error != null) {
      model.addAttribute("errorMessage", "Invalid username or password.");
    }
    if (logout != null) {
      model.addAttribute("logoutMessage", "You have been logged out successfully.");
    }
    if (expired != null) {
      model.addAttribute("expiredMessage", "Your session has expired. Please log in again.");
    }

    return "login";
  }

  @GetMapping("/")
  public String home() {
    return "redirect:/dashboard";
  }

  @GetMapping("/dashboard")
  public String dashboard() {
    return "dashboard";
  }
}
