package com.ivamare.controller;

import com.ivamare.dto.QueryFormDto;
import com.ivamare.service.ConnectionRegistry;
import com.ivamare.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

  @GetMapping
  public String listQueries(Model model) {
    model.addAttribute("queriesByCategory", queryService.getQueriesGroupedByCategory());
    model.addAttribute("pageTitle", "Queries");
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
  public String newQueryForm(Model model) {
    model.addAttribute("query", new QueryFormDto());
    model.addAttribute("connections", connectionRegistry.listConnections());
    model.addAttribute("categories", queryService.getAllCategories());
    model.addAttribute("isEdit", false);
    model.addAttribute("pageTitle", "New Query");
    return "queries/form";
  }

  @GetMapping("/{id}/edit")
  @PreAuthorize("hasRole('ADMIN')")
  public String editQueryForm(@PathVariable String id, Model model) {
    model.addAttribute("query", queryService.getQueryForEdit(id));
    model.addAttribute("connections", connectionRegistry.listConnections());
    model.addAttribute("categories", queryService.getAllCategories());
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

    if (result.hasErrors()) {
      model.addAttribute("connections", connectionRegistry.listConnections());
      model.addAttribute("categories", queryService.getAllCategories());
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
}
