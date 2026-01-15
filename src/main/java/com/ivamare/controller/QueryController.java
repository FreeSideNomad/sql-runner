package com.ivamare.controller;

import com.ivamare.domain.ParameterType;
import com.ivamare.dto.ExecutionResult;
import com.ivamare.dto.QueryConfig;
import com.ivamare.dto.QueryFormDto;
import com.ivamare.service.ConnectionRegistry;
import com.ivamare.service.QueryExecutionService;
import com.ivamare.service.QueryService;
import com.ivamare.util.CsvUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller for query browsing and management. */
@Controller
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {

  private final QueryService queryService;
  private final ConnectionRegistry connectionRegistry;
  private final QueryExecutionService executionService;
  private final com.ivamare.service.QueryConfigValidator configValidator;

  @Value("${sqlrunner.read-only-mode:false}")
  private boolean readOnlyMode;

  @GetMapping
  public String listQueries(Model model) {
    model.addAttribute("groupedQueries", queryService.getQueriesGroupedByConnectionAndCategory());
    model.addAttribute("pageTitle", "Queries");
    model.addAttribute("readOnlyMode", readOnlyMode);
    return "queries/list";
  }

  @GetMapping("/{id}")
  public String viewQuery(@PathVariable String id, Model model) {
    model.addAttribute("query", queryService.getQueryDto(id));
    model.addAttribute("configYaml", queryService.getCurrentConfigYaml(id));
    model.addAttribute("pageTitle", "Query Details");
    return "queries/view";
  }

  @GetMapping("/new")
  @PreAuthorize("hasRole('ADMIN')")
  public String newQueryForm(Model model, RedirectAttributes redirectAttributes) {
    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Cannot create queries in read-only mode");
      return "redirect:/queries";
    }
    QueryFormDto form = new QueryFormDto();
    form.ensureConfigInitialized();
    model.addAttribute("query", form);
    model.addAttribute("connections", connectionRegistry.listConnections());
    model.addAttribute("categories", queryService.getAllCategories());
    model.addAttribute("parameterTypes", ParameterType.values());
    model.addAttribute(
        "availableParameters", configValidator.getAvailableParameters(form.getConfig()));
    model.addAttribute("isEdit", false);
    model.addAttribute("pageTitle", "New Query");
    return "queries/form";
  }

  @GetMapping("/{id}/edit")
  @PreAuthorize("hasRole('ADMIN')")
  public String editQueryForm(
      @PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Cannot edit queries in read-only mode");
      return "redirect:/queries";
    }
    QueryFormDto form = queryService.getQueryForEdit(id);
    form.ensureConfigInitialized();
    model.addAttribute("query", form);
    model.addAttribute("connections", connectionRegistry.listConnections());
    model.addAttribute("categories", queryService.getAllCategories());
    model.addAttribute("parameterTypes", ParameterType.values());
    model.addAttribute(
        "availableParameters", configValidator.getAvailableParameters(form.getConfig()));
    model.addAttribute("isEdit", true);
    model.addAttribute("pageTitle", "Edit Query");
    return "queries/form";
  }

  @PostMapping("/save")
  @PreAuthorize("hasRole('ADMIN')")
  public String saveQuery(
      @Valid @ModelAttribute("query") QueryFormDto form,
      BindingResult result,
      Authentication auth,
      Model model,
      RedirectAttributes redirectAttributes) {

    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Cannot save queries in read-only mode");
      return "redirect:/queries";
    }

    // Custom validation for UPDATE_WORKFLOW
    if (form.getQueryType() == com.ivamare.domain.QueryType.UPDATE_WORKFLOW) {
      com.ivamare.service.QueryConfigValidator.ValidationResult validationResult =
          configValidator.validateUpdateConfig(form.getConfig(), form.getQueryType());
      if (validationResult.hasErrors()) {
        for (String error : validationResult.getErrors()) {
          result.reject("config", error);
        }
      }
      if (validationResult.hasWarnings()) {
        model.addAttribute("warnings", validationResult.getWarnings());
      }
    }

    if (result.hasErrors()) {
      form.ensureConfigInitialized();
      model.addAttribute("connections", connectionRegistry.listConnections());
      model.addAttribute("categories", queryService.getAllCategories());
      model.addAttribute("parameterTypes", ParameterType.values());
      model.addAttribute(
          "availableParameters", configValidator.getAvailableParameters(form.getConfig()));
      model.addAttribute("isEdit", form.isEdit());
      model.addAttribute("pageTitle", form.isEdit() ? "Edit Query" : "New Query");
      return "queries/form";
    }

    String username = auth.getName();

    if (form.isEdit()) {
      queryService.updateQuery(form.getId(), form, username);
      redirectAttributes.addFlashAttribute("message", "Query updated successfully");
    } else {
      queryService.createQuery(form, username);
      redirectAttributes.addFlashAttribute("message", "Query created successfully");
    }

    return "redirect:/queries";
  }

  @GetMapping("/{id}/delete")
  @PreAuthorize("hasRole('ADMIN')")
  public String deleteQuery(
      @PathVariable String id, Authentication auth, RedirectAttributes redirectAttributes) {
    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Cannot delete queries in read-only mode");
      return "redirect:/queries";
    }
    queryService.deleteQuery(id, auth.getName());
    redirectAttributes.addFlashAttribute("message", "Query deleted successfully");
    return "redirect:/queries";
  }

  @GetMapping("/{id}/versions")
  @PreAuthorize("hasRole('ADMIN')")
  public String versionHistory(@PathVariable String id, Model model) {
    model.addAttribute("query", queryService.getQueryDto(id));
    model.addAttribute("versions", queryService.getVersionHistory(id));
    model.addAttribute("pageTitle", "Version History");
    return "queries/versions";
  }

  @GetMapping("/{id}/versions/{version}")
  @PreAuthorize("hasRole('ADMIN')")
  public String viewVersion(@PathVariable String id, @PathVariable int version, Model model) {
    model.addAttribute("query", queryService.getQueryDto(id));
    model.addAttribute("version", queryService.getVersion(id, version));
    model.addAttribute("pageTitle", "Version " + version);
    return "queries/version-detail";
  }

  @GetMapping("/categories")
  @ResponseBody
  public java.util.List<String> getCategories() {
    return queryService.getAllCategories();
  }

  // ==================== Query Execution ====================

  @GetMapping("/{id}/execute")
  public String executeForm(@PathVariable String id, Model model) {
    var queryDto = queryService.getQueryDto(id);
    String configYaml = queryService.getCurrentConfigYaml(id);
    QueryConfig config = executionService.parseConfig(configYaml);

    model.addAttribute("query", queryDto);
    model.addAttribute("config", config);
    model.addAttribute("parameters", config.getParameters());
    model.addAttribute("pageTitle", "Execute: " + queryDto.getName());
    return "queries/execute";
  }

  @PostMapping("/{id}/execute")
  public String executeQuery(
      @PathVariable String id,
      @RequestParam Map<String, String> allParams,
      Authentication auth,
      Model model) {

    // Remove non-parameter fields
    Map<String, String> params = new HashMap<>(allParams);
    params.remove("_csrf");

    var queryDto = queryService.getQueryDto(id);
    String configYaml = queryService.getCurrentConfigYaml(id);
    QueryConfig config = executionService.parseConfig(configYaml);

    ExecutionResult result = executionService.executeSelect(id, params, auth.getName());

    model.addAttribute("query", queryDto);
    model.addAttribute("config", config);
    model.addAttribute("parameters", config.getParameters());
    model.addAttribute("result", result);
    model.addAttribute("submittedParams", params);
    model.addAttribute("pageTitle", "Results: " + queryDto.getName());
    return "queries/execute";
  }

  @GetMapping("/{id}/export-csv")
  public void exportCsv(
      @PathVariable String id,
      @RequestParam Map<String, String> allParams,
      Authentication auth,
      HttpServletResponse response)
      throws IOException {

    // Remove non-parameter fields
    Map<String, String> params = new HashMap<>(allParams);
    params.remove("_csrf");

    var queryDto = queryService.getQueryDto(id);
    ExecutionResult result = executionService.executeSelect(id, params, auth.getName());

    if (!result.isSuccess()) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result.getErrorMessage());
      return;
    }

    // Set response headers for CSV download
    String filename = queryDto.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".csv";
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    // Filter out any empty column names
    List<String> columns =
        result.getColumns().stream().filter(col -> col != null && !col.trim().isEmpty()).toList();

    CsvUtils.writeCsv(response.getOutputStream(), result.getRows(), columns, columns);
  }
}
