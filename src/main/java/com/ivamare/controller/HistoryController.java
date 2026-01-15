package com.ivamare.controller;

import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.repository.ExecutionLogRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Controller for execution history viewing. */
@Controller
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

  private final ExecutionLogRepository logRepository;

  @GetMapping
  public String listHistory(
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Model model) {

    // Convert empty strings to null for proper query handling
    String userFilter = (user != null && !user.isEmpty()) ? user : null;
    ExecutionStatus statusEnum =
        status != null && !status.isEmpty() ? ExecutionStatus.valueOf(status) : null;
    LocalDateTime start =
        startDate != null && !startDate.isEmpty()
            ? LocalDateTime.parse(startDate + "T00:00:00")
            : null;
    LocalDateTime end =
        endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;

    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
    var logs =
        logRepository.findWithFilters(userFilter, null, statusEnum, null, start, end, pageable);

    model.addAttribute("logs", logs.getContent());
    model.addAttribute("page", page);
    model.addAttribute("size", size);
    model.addAttribute("totalElements", logs.getTotalElements());
    model.addAttribute("totalPages", logs.getTotalPages());
    model.addAttribute("user", user);
    model.addAttribute("status", status);
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    model.addAttribute("pageTitle", "Execution History");

    return "history/list";
  }

  @GetMapping("/export")
  public void exportCsv(
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      HttpServletResponse response)
      throws IOException {

    // Convert empty strings to null for proper query handling
    String userFilter = (user != null && !user.isEmpty()) ? user : null;
    ExecutionStatus statusEnum =
        status != null && !status.isEmpty() ? ExecutionStatus.valueOf(status) : null;
    LocalDateTime start =
        startDate != null && !startDate.isEmpty()
            ? LocalDateTime.parse(startDate + "T00:00:00")
            : null;
    LocalDateTime end =
        endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;

    var pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "executedAt"));
    List<ExecutionLog> logs =
        logRepository
            .findWithFilters(userFilter, null, statusEnum, null, start, end, pageable)
            .getContent();

    // Set response headers for CSV download
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String filename = "execution-history-" + timestamp + ".csv";
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    // Write UTF-8 BOM for Excel compatibility
    response.getOutputStream().write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

    try (PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {

      // Write header
      writer.println(
          "ID,Query ID,Connection,Executed By,Executed At,Type,Status,Row Count,Duration (ms),Error Message");

      // Write data rows
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      for (ExecutionLog log : logs) {
        writer.println(
            String.join(
                ",",
                escapeCsv(log.getId()),
                escapeCsv(log.getQueryId()),
                escapeCsv(log.getConnectionName()),
                escapeCsv(log.getExecutedBy()),
                escapeCsv(log.getExecutedAt() != null ? log.getExecutedAt().format(dtf) : ""),
                escapeCsv(log.getExecutionType() != null ? log.getExecutionType().name() : ""),
                escapeCsv(log.getStatus() != null ? log.getStatus().name() : ""),
                escapeCsv(log.getRowCount() != null ? log.getRowCount().toString() : ""),
                escapeCsv(
                    log.getExecutionTimeMs() != null ? log.getExecutionTimeMs().toString() : ""),
                escapeCsv(log.getErrorMessage())));
      }
    }
  }

  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  @GetMapping("/{id}")
  public String viewDetail(@PathVariable String id, Model model) {
    var log =
        logRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));

    model.addAttribute("log", log);
    model.addAttribute("pageTitle", "Execution Detail");

    return "history/detail";
  }
}
