package com.ivamare.controller;

import com.ivamare.domain.BackupRecord;
import com.ivamare.domain.ExecutionLog;
import com.ivamare.domain.ExecutionStatus;
import com.ivamare.domain.ExecutionType;
import com.ivamare.repository.ExecutionLogRepository;
import com.ivamare.service.UpdateWorkflowService;
import com.ivamare.util.CsvUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final UpdateWorkflowService updateWorkflowService;

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

    String headerRow =
        CsvUtils.toRow(
            "ID",
            "Query ID",
            "Connection",
            "Executed By",
            "Executed At",
            "Type",
            "Status",
            "Row Count",
            "Duration (ms)",
            "Error Message");

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    CsvUtils.writeCsv(
        response.getOutputStream(),
        headerRow,
        writer -> {
          for (ExecutionLog log : logs) {
            writer.println(
                CsvUtils.toRow(
                    log.getId(),
                    log.getQueryId(),
                    log.getConnectionName(),
                    log.getExecutedBy(),
                    log.getExecutedAt() != null ? log.getExecutedAt().format(dtf) : "",
                    log.getExecutionType() != null ? log.getExecutionType().name() : "",
                    log.getStatus() != null ? log.getStatus().name() : "",
                    log.getRowCount(),
                    log.getExecutionTimeMs(),
                    log.getErrorMessage()));
          }
        });
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

    // Check if this is an UPDATE execution with backup data
    if (log.getExecutionType() == ExecutionType.UPDATE) {
      Optional<BackupRecord> backup = updateWorkflowService.getBackupForExecution(id);
      if (backup.isPresent()) {
        model.addAttribute("backup", backup.get());
        model.addAttribute("canRollback", !backup.get().getIsRolledBack());
      }
    }

    return "history/detail";
  }

  /** Download backup data as CSV. */
  @GetMapping("/{id}/backup/download")
  public void downloadBackupCsv(@PathVariable String id, HttpServletResponse response)
      throws IOException {

    var log =
        logRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execution not found"));

    Optional<BackupRecord> backupOpt = updateWorkflowService.getBackupForExecution(id);
    if (backupOpt.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Backup not found for this execution");
    }

    BackupRecord backup = backupOpt.get();
    List<Map<String, Object>> backupData = updateWorkflowService.deserializeBackupData(backup);

    if (backupData.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Backup data is empty");
    }

    // Get columns from first row
    List<String> columns = backupData.get(0).keySet().stream().sorted().toList();

    // Set response headers
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String filename = "backup-" + id.substring(0, 8) + "-" + timestamp + ".csv";
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    CsvUtils.writeCsv(response.getOutputStream(), backupData, columns, columns);
  }
}
