# F007-S006: Create Results Table with Pagination

## User Story

**As a** user
**I want** query results displayed in a paginated table
**So that** I can browse large result sets

## Acceptance Criteria

- [ ] Given results, then displayed in table format
- [ ] Given column headers, then from result metadata
- [ ] Given pagination, then 25/50/100 rows per page options
- [ ] Given page navigation, then prev/next buttons
- [ ] Given total count, then displayed above table
- [ ] Given empty results, then "No results" message

## Technical Notes

### Files to Create
- `src/main/resources/templates/queries/results.html`
- `src/main/resources/templates/fragments/results-table.html`
- `src/main/java/com/ivamare/dto/PagedResult.java`

### Paged Result DTO
```java
@Data
@AllArgsConstructor
public class PagedResult<T> {
    private List<T> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;
}
```

### Controller
```java
@GetMapping("/{id}/execute")
public String executeQuery(@PathVariable String id,
                          @RequestParam Map<String, String> params,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "25") int size,
                          Model model) {
    ExecutionResult result = queryExecutionService.execute(id, params, getCurrentUser());

    // Server-side pagination
    int start = page * size;
    int end = Math.min(start + size, result.getRows().size());
    List<Map<String, Object>> pageContent = result.getRows().subList(start, end);

    PagedResult<Map<String, Object>> pagedResult = new PagedResult<>(
        pageContent, page, size,
        (int) Math.ceil((double) result.getRowCount() / size),
        result.getRowCount(),
        end < result.getRowCount(),
        page > 0
    );

    model.addAttribute("result", pagedResult);
    model.addAttribute("columns", result.getColumns());
    model.addAttribute("executionTime", result.getExecutionTimeMs());

    return "queries/results";
}
```

### Results Table Fragment
```html
<div th:fragment="results-table" class="bg-white rounded-lg shadow overflow-hidden">
    <!-- Header with count and export -->
    <div class="px-4 py-3 bg-gray-50 flex justify-between items-center">
        <span class="text-sm text-gray-600">
            <span th:text="${result.totalElements}">0</span> results
            (<span th:text="${executionTime}">0</span>ms)
        </span>
        <div class="flex gap-2">
            <select id="pageSize" onchange="changePageSize(this.value)" class="text-sm border rounded px-2 py-1">
                <option value="25" th:selected="${result.size == 25}">25</option>
                <option value="50" th:selected="${result.size == 50}">50</option>
                <option value="100" th:selected="${result.size == 100}">100</option>
            </select>
            <a th:href="@{export}" class="text-sm text-rbc-blue hover:underline">Export CSV</a>
        </div>
    </div>

    <!-- Table -->
    <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
                <tr>
                    <th th:each="col : ${columns}" th:text="${col}"
                        class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                        Column
                    </th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
                <tr th:each="row : ${result.content}">
                    <td th:each="col : ${columns}" th:text="${row[col]}"
                        class="px-4 py-2 text-sm text-gray-900 whitespace-nowrap">
                        Value
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- Pagination -->
    <div class="px-4 py-3 bg-gray-50 flex justify-between items-center">
        <span class="text-sm text-gray-600">
            Page <span th:text="${result.page + 1}">1</span> of <span th:text="${result.totalPages}">1</span>
        </span>
        <div class="flex gap-2">
            <button th:disabled="${!result.hasPrevious}" onclick="prevPage()"
                    class="px-3 py-1 border rounded disabled:opacity-50">Prev</button>
            <button th:disabled="${!result.hasNext}" onclick="nextPage()"
                    class="px-3 py-1 border rounded disabled:opacity-50">Next</button>
        </div>
    </div>
</div>
```

## Test Plan

- [ ] Integration test: Results displayed correctly
- [ ] Integration test: Pagination works
- [ ] Visual test: Empty state displays message

## Parent Feature

Relates to F007-select-execution
