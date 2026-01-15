package com.ivamare.controller;

import com.ivamare.domain.ExecutionStatus;
import com.ivamare.repository.ExecutionLogRepository;
import com.ivamare.repository.QueryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controller for authentication-related pages. */
@Controller
@RequiredArgsConstructor
public class AuthController {

  private final QueryRepository queryRepository;
  private final ExecutionLogRepository executionLogRepository;

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
  public String dashboard(Model model) {
    // Count active queries
    long queryCount = queryRepository.countByIsActiveTrue();

    // Count today's executions
    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
    long todayExecutions = executionLogRepository.countByExecutedAtAfter(startOfDay);
    long failedToday =
        executionLogRepository.countByExecutedAtAfterAndStatus(startOfDay, ExecutionStatus.FAILED);

    model.addAttribute("queryCount", queryCount);
    model.addAttribute("todayExecutions", todayExecutions);
    model.addAttribute("failedToday", failedToday);
    model.addAttribute("pageTitle", "Dashboard");

    return "dashboard";
  }
}
