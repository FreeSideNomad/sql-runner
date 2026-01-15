package com.ivamare.controller;

import com.ivamare.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Controller for query browsing and execution. */
@Controller
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {

  private final QueryRepository queryRepository;

  @GetMapping
  public String listQueries(Model model) {
    model.addAttribute("queries", queryRepository.findByIsActiveTrue());
    model.addAttribute("categories", queryRepository.findDistinctCategories());
    model.addAttribute("pageTitle", "Queries");
    return "queries/list";
  }
}
