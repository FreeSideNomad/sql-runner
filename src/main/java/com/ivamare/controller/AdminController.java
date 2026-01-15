package com.ivamare.controller;

import com.ivamare.repository.QueryRepository;
import com.ivamare.service.ConfigExportService;
import com.ivamare.service.ConfigImportService;
import com.ivamare.service.ConfigImportService.ImportResult;
import com.ivamare.service.ConfigImportService.ImportValidationResult;
import com.ivamare.service.ConnectionRegistry;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller for admin functions including export/import. */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

  private final ConfigExportService exportService;
  private final ConfigImportService importService;
  private final ConnectionRegistry connectionRegistry;
  private final QueryRepository queryRepository;

  @Value("${sqlrunner.read-only-mode:false}")
  private boolean readOnlyMode;

  @GetMapping
  public String adminDashboard(Model model) {
    model.addAttribute("pageTitle", "Administration");
    return "admin/index";
  }

  // ==================== Export ====================

  @GetMapping("/export")
  public String exportPage(Model model) {
    model.addAttribute("pageTitle", "Export Configuration");
    return "admin/export";
  }

  @GetMapping("/export/download")
  public void downloadExport(Authentication auth, HttpServletResponse response) throws IOException {
    String yaml = exportService.exportAll(auth.getName());

    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String filename = "sqlrunner-export-" + timestamp + ".yaml";

    response.setContentType("application/x-yaml; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    try (PrintWriter writer =
        new PrintWriter(
            new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      writer.write(yaml);
    }

    log.info("Configuration exported by '{}'", auth.getName());
  }

  // ==================== Import ====================

  @GetMapping("/import")
  public String importPage(Model model, RedirectAttributes redirectAttributes) {
    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Import is disabled in read-only mode");
      return "redirect:/admin/import-export";
    }
    model.addAttribute("pageTitle", "Import Configuration");
    return "admin/import";
  }

  @PostMapping("/import/validate")
  public String validateImport(
      @RequestParam("file") MultipartFile file, Model model, RedirectAttributes redirectAttributes)
      throws IOException {

    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Import is disabled in read-only mode");
      return "redirect:/admin/import-export";
    }

    if (file.isEmpty()) {
      redirectAttributes.addFlashAttribute("error", "Please select a file to import");
      return "redirect:/admin/import";
    }

    String yamlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
    ImportValidationResult validation = importService.validateImport(yamlContent);

    model.addAttribute("validation", validation);
    model.addAttribute("yamlContent", yamlContent);
    model.addAttribute("filename", file.getOriginalFilename());
    model.addAttribute("pageTitle", "Validate Import");
    return "admin/import-validate";
  }

  @PostMapping("/import/execute")
  public String executeImport(
      @RequestParam("yamlContent") String yamlContent,
      Authentication auth,
      RedirectAttributes redirectAttributes) {

    if (readOnlyMode) {
      redirectAttributes.addFlashAttribute("error", "Import is disabled in read-only mode");
      return "redirect:/admin/import-export";
    }

    ImportResult result = importService.importQueries(yamlContent, auth.getName());

    if (result.success()) {
      redirectAttributes.addFlashAttribute(
          "message",
          String.format(
              "Import successful: %d created, %d updated, %d skipped",
              result.created(), result.updated(), result.skipped()));
      redirectAttributes.addFlashAttribute("importMessages", result.messages());
    } else {
      redirectAttributes.addFlashAttribute("error", "Import failed: " + result.error());
    }

    return "redirect:/admin/import";
  }

  // ==================== Connections ====================

  @GetMapping("/connections")
  public String listConnections(Model model) {
    var connections = connectionRegistry.listConnections();
    model.addAttribute("connections", connections);
    model.addAttribute("pageTitle", "Database Connections");
    return "admin/connections";
  }

  @PostMapping("/connections/{id}/test")
  public String testConnection(@PathVariable String id, RedirectAttributes redirectAttributes) {
    var result = connectionRegistry.testConnection(id);
    if (result.isConnected()) {
      redirectAttributes.addFlashAttribute("message", "Connection '" + id + "' is working");
    } else {
      redirectAttributes.addFlashAttribute(
          "error", "Connection '" + id + "' failed: " + result.getErrorMessage());
    }
    return "redirect:/admin/connections";
  }

  // ==================== Query Management ====================

  @GetMapping("/queries")
  public String adminQueries(Model model) {
    var queries = queryRepository.findAll();
    model.addAttribute("queries", queries);
    model.addAttribute("pageTitle", "Query Management");
    return "admin/queries";
  }

  // ==================== Import/Export Combined ====================

  @GetMapping("/import-export")
  public String importExportPage(Model model) {
    model.addAttribute("pageTitle", "Import/Export Configuration");
    model.addAttribute("readOnlyMode", readOnlyMode);
    return "admin/import-export";
  }
}
