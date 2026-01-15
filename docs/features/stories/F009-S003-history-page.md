# F009-S003: Build Execution History Page

## User Story

**As a** user
**I want** to view execution history
**So that** I can see past query executions

## Acceptance Criteria

- [ ] Given history page, then list of executions displayed
- [ ] Given execution list, then query name, time, status shown
- [ ] Given execution list, then paginated (25 per page)
- [ ] Given execution row, then click shows detail
- [ ] Given any role, then can access own history
- [ ] Given ADMIN role, then can see all users' history

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/HistoryController.java`
- `src/main/resources/templates/history/list.html`

### Controller
```java
@Controller
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {
    private final ExecutionLogService logService;
    private final QueryRepository queryRepository;

    @GetMapping
    public String listHistory(@RequestParam(required = false) String user,
                             @RequestParam(required = false) String queryId,
                             @RequestParam(required = false) ExecutionStatus status,
                             @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                             @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                             @RequestParam(defaultValue = "0") int page,
                             Model model,
                             Authentication auth) {
        // Non-admins can only see their own history
        if (!hasRole(auth, "ADMIN")) {
            user = auth.getName();
        }

        Page<ExecutionLog> logs = logService.findWithFilters(
            user, queryId, status,
            startDate != null ? startDate.atStartOfDay() : null,
            endDate != null ? endDate.plusDays(1).atStartOfDay() : null,
            PageRequest.of(page, 25)
        );

        // Enrich with query names
        Map<String, String> queryNames = getQueryNames(logs.getContent());

        model.addAttribute("pageTitle", "Execution History");
        model.addAttribute("logs", logs);
        model.addAttribute("queryNames", queryNames);
        model.addAttribute("queries", queryRepository.findByIsActiveTrue());
        model.addAttribute("currentFilters", Map.of(
            "user", user != null ? user : "",
            "queryId", queryId != null ? queryId : "",
            "status", status != null ? status.name() : "",
            "startDate", startDate != null ? startDate.toString() : "",
            "endDate", endDate != null ? endDate.toString() : ""
        ));

        return "history/list";
    }
}
```

### Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">Execution History</h1>

    <!-- Filters (collapsible) -->
    <div th:replace="~{history/filters :: filter-panel}"></div>

    <!-- Results table -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
                <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Query</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Rows</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                    <th class="px-4 py-3"></th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
                <tr th:each="log : ${logs.content}" class="hover:bg-gray-50">
                    <td class="px-4 py-3" th:text="${queryNames[log.queryId]}">Query Name</td>
                    <td class="px-4 py-3">
                        <span th:class="${log.executionType.name() == 'SELECT' ? 'bg-blue-100 text-blue-800' :
                                         log.executionType.name() == 'UPDATE' ? 'bg-orange-100 text-orange-800' :
                                         'bg-purple-100 text-purple-800'}"
                              class="px-2 py-1 rounded text-xs font-medium"
                              th:text="${log.executionType}">SELECT</span>
                    </td>
                    <td class="px-4 py-3 text-sm" th:text="${log.executedBy}">user</td>
                    <td class="px-4 py-3 text-sm" th:text="${#temporals.format(log.executedAt, 'MMM d HH:mm')}">Jan 1 12:00</td>
                    <td class="px-4 py-3 text-sm" th:text="${log.rowCount}">0</td>
                    <td class="px-4 py-3">
                        <span th:class="${log.status.name() == 'SUCCESS' ? 'text-green-600' :
                                         log.status.name() == 'FAILED' ? 'text-red-600' :
                                         'text-yellow-600'}"
                              th:text="${log.status}">SUCCESS</span>
                    </td>
                    <td class="px-4 py-3">
                        <a th:href="@{/history/{id}(id=${log.id})}" class="text-rbc-blue hover:underline">View</a>
                    </td>
                </tr>
            </tbody>
        </table>

        <!-- Pagination -->
        <div th:replace="~{fragments/pagination :: pagination(${logs})}"></div>
    </div>
</div>
</html>
```

## Test Plan

- [ ] Integration test: History page loads
- [ ] Integration test: Non-admin sees only own history
- [ ] Integration test: Pagination works

## Parent Feature

Relates to F009-execution-logging
