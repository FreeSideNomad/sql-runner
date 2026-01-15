package com.ivamare.controller;

import com.ivamare.domain.ExecutionStatus;
import com.ivamare.repository.ExecutionLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    ExecutionStatus statusEnum =
        status != null && !status.isEmpty() ? ExecutionStatus.valueOf(status) : null;
    LocalDateTime start =
        startDate != null && !startDate.isEmpty()
            ? LocalDateTime.parse(startDate + "T00:00:00")
            : null;
    LocalDateTime end =
        endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;

    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
    var logs = logRepository.findWithFilters(user, null, statusEnum, null, start, end, pageable);

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
}
