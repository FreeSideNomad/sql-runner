package com.ivamare.controller;

import com.ivamare.domain.QueryType;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.service.QueryExecutionService;
import com.ivamare.service.QueryService;
import com.ivamare.service.UpdateWorkflowService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller for UPDATE workflow with preview, execute, and rollback steps. */
@Controller
@RequestMapping("/queries")
@RequiredArgsConstructor
public class UpdateWorkflowController {

  private final QueryService queryService;
  private final QueryExecutionService executionService;
  private final UpdateWorkflowService updateWorkflowService;

  private static final String SESSION_PREVIEW_DATA = "updatePreviewData";
  private static final String SESSION_PREVIEW_PARAMS = "updatePreviewParams";

  /** Step 1: Show the update workflow form with parameters. */
  @GetMapping("/{id}/update")
  public String showUpdateForm(@PathVariable String id, Model model) {
    var queryDto = queryService.getQueryDto(id);

    if (queryDto.getQueryType() != QueryType.UPDATE_WORKFLOW) {
      throw new IllegalArgumentException("Query is not an UPDATE_WORKFLOW type");
    }

    String configYaml = queryService.getCurrentConfigYaml(id);
    QueryConfig config = executionService.parseConfig(configYaml);

    model.addAttribute("query", queryDto);
    model.addAttribute("config", config);
    model.addAttribute("parameters", config.getParameters());
    model.addAttribute("step", "parameters");
    model.addAttribute("pageTitle", "Update: " + queryDto.getName());
    return "queries/update-workflow";
  }

  /** Step 2: Execute preview and show data that will be updated. */
  @PostMapping("/{id}/update/preview")
  public String executePreview(
      @PathVariable String id,
      @RequestParam Map<String, String> allParams,
      Authentication auth,
      HttpSession session,
      Model model) {

    Map<String, String> params = new HashMap<>(allParams);
    params.remove("_csrf");

    var queryDto = queryService.getQueryDto(id);
    String configYaml = queryService.getCurrentConfigYaml(id);
    QueryConfig config = executionService.parseConfig(configYaml);

    ExecutionResult result = updateWorkflowService.executePreview(id, params, auth.getName());

    if (result.isSuccess() && result.getRows() != null) {
      // Store preview data in session for later use
      session.setAttribute(SESSION_PREVIEW_DATA, result.getRows());
      session.setAttribute(SESSION_PREVIEW_PARAMS, params);
    }

    model.addAttribute("query", queryDto);
    model.addAttribute("config", config);
    model.addAttribute("parameters", config.getParameters());
    model.addAttribute("result", result);
    model.addAttribute("submittedParams", params);
    model.addAttribute("step", "preview");
    model.addAttribute("pageTitle", "Preview: " + queryDto.getName());
    return "queries/update-workflow";
  }

  /** Export preview data as CSV. */
  @GetMapping("/{id}/update/export-csv")
  public void exportPreviewCsv(
      @PathVariable String id,
      @RequestParam Map<String, String> allParams,
      Authentication auth,
      HttpServletResponse response)
      throws IOException {

    Map<String, String> params = new HashMap<>(allParams);
    params.remove("_csrf");

    var queryDto = queryService.getQueryDto(id);
    ExecutionResult result = updateWorkflowService.executePreview(id, params, auth.getName());

    if (!result.isSuccess()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result.getErrorMessage());
      return;
    }

    String filename = queryDto.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_preview.csv";
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    response.getOutputStream().write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

    try (PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {

      List<String> columns =
          result.getColumns().stream().filter(col -> col != null && !col.trim().isEmpty()).toList();

      writer.println(
          columns.stream().map(this::escapeCsv).collect(java.util.stream.Collectors.joining(",")));

      for (Map<String, Object> row : result.getRows()) {
        String line =
            columns.stream()
                .map(col -> escapeCsv(row.get(col) != null ? row.get(col).toString() : ""))
                .collect(java.util.stream.Collectors.joining(","));
        writer.println(line);
      }
    }
  }

  /** Step 3: Execute the UPDATE with backup. */
  @PostMapping("/{id}/update/execute")
  @SuppressWarnings("unchecked")
  public String executeUpdate(
      @PathVariable String id,
      Authentication auth,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    List<Map<String, Object>> previewData =
        (List<Map<String, Object>>) session.getAttribute(SESSION_PREVIEW_DATA);
    Map<String, String> params = (Map<String, String>) session.getAttribute(SESSION_PREVIEW_PARAMS);

    if (previewData == null || params == null) {
      redirectAttributes.addFlashAttribute(
          "error", "Preview data expired. Please run preview again.");
      return "redirect:/queries/" + id + "/update";
    }

    ExecutionResult result =
        updateWorkflowService.executeUpdate(id, params, previewData, auth.getName());

    // Clear session data
    session.removeAttribute(SESSION_PREVIEW_DATA);
    session.removeAttribute(SESSION_PREVIEW_PARAMS);

    if (result.isSuccess()) {
      redirectAttributes.addFlashAttribute("updateResult", result);
      redirectAttributes.addFlashAttribute(
          "message", "Update completed successfully. " + result.getRowCount() + " rows affected.");
      return "redirect:/queries/" + id + "/update/complete/" + result.getExecutionLogId();
    } else {
      redirectAttributes.addFlashAttribute("error", "Update failed: " + result.getErrorMessage());
      return "redirect:/queries/" + id + "/update";
    }
  }

  /** Step 4: Show completion page with rollback option. */
  @GetMapping("/{id}/update/complete/{executionLogId}")
  public String showComplete(
      @PathVariable String id, @PathVariable String executionLogId, Model model) {

    var queryDto = queryService.getQueryDto(id);
    var backup = updateWorkflowService.getBackupForExecution(executionLogId);

    model.addAttribute("query", queryDto);
    model.addAttribute("executionLogId", executionLogId);
    model.addAttribute("backup", backup.orElse(null));
    model.addAttribute("step", "complete");
    model.addAttribute("pageTitle", "Update Complete: " + queryDto.getName());
    return "queries/update-workflow";
  }

  /** Download SQL script for the update operation. */
  @GetMapping("/{id}/update/download-script")
  @SuppressWarnings("unchecked")
  public void downloadUpdateScript(
      @PathVariable String id, HttpSession session, HttpServletResponse response)
      throws IOException {

    List<Map<String, Object>> previewData =
        (List<Map<String, Object>>) session.getAttribute(SESSION_PREVIEW_DATA);
    Map<String, String> params = (Map<String, String>) session.getAttribute(SESSION_PREVIEW_PARAMS);

    if (previewData == null || params == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Preview data expired");
      return;
    }

    var queryDto = queryService.getQueryDto(id);
    String script = updateWorkflowService.generateUpdateScript(id, params, previewData);

    String filename = queryDto.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_update.sql";
    downloadText(response, script, filename);
  }

  /** Download SQL script for the rollback operation. */
  @GetMapping("/{id}/update/rollback/{executionLogId}/download-script")
  public void downloadRollbackScript(
      @PathVariable String id, @PathVariable String executionLogId, HttpServletResponse response)
      throws IOException {

    var queryDto = queryService.getQueryDto(id);
    String script = updateWorkflowService.generateRollbackScript(executionLogId);

    String filename = queryDto.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_rollback.sql";
    downloadText(response, script, filename);
  }

  private void downloadText(HttpServletResponse response, String text, String filename)
      throws IOException {
    response.setContentType("text/plain; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    response.getWriter().write(text);
    response.getWriter().flush();
  }

  /** Execute rollback for a previous update. */
  @PostMapping("/update/rollback/{executionLogId}")
  public String executeRollback(
      @PathVariable String executionLogId,
      Authentication auth,
      RedirectAttributes redirectAttributes) {

    try {
      ExecutionResult result =
          updateWorkflowService.executeRollback(executionLogId, auth.getName());

      if (result.isSuccess()) {
        redirectAttributes.addFlashAttribute(
            "message",
            "Rollback completed successfully. " + result.getRowCount() + " rows restored.");
      } else {
        redirectAttributes.addFlashAttribute(
            "error", "Rollback failed: " + result.getErrorMessage());
      }
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", "Invalid rollback request: " + e.getMessage());
    }

    return "redirect:/history";
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
}
