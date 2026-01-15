# F007-S007: Add Column Sorting Functionality

## User Story

**As a** user
**I want** to sort results by clicking column headers
**So that** I can organize data for analysis

## Acceptance Criteria

- [ ] Given column header click, then sort by that column
- [ ] Given ascending sort, then second click sorts descending
- [ ] Given sort state, then arrow indicator shown
- [ ] Given sort, then pagination reset to page 1
- [ ] Given sort, then maintains other parameters

## Technical Notes

### Files to Modify
- `src/main/resources/templates/fragments/results-table.html`
- `src/main/java/com/ivamare/controller/QueryExecutionController.java`

### Sort Parameters
```java
@GetMapping("/{id}/execute")
public String executeQuery(@PathVariable String id,
                          @RequestParam Map<String, String> params,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "25") int size,
                          @RequestParam(required = false) String sortBy,
                          @RequestParam(defaultValue = "asc") String sortDir,
                          Model model) {
    // ... execute query ...

    // Sort results in memory (server-side)
    if (sortBy != null && !sortBy.isBlank()) {
        Comparator<Map<String, Object>> comparator = (a, b) -> {
            Object valA = a.get(sortBy);
            Object valB = b.get(sortBy);
            return compareValues(valA, valB);
        };
        if ("desc".equals(sortDir)) {
            comparator = comparator.reversed();
        }
        result.getRows().sort(comparator);
    }

    model.addAttribute("sortBy", sortBy);
    model.addAttribute("sortDir", sortDir);
}
```

### Sortable Header Template
```html
<th th:each="col : ${columns}"
    class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
    th:onclick="'sortColumn(\'' + ${col} + '\')'">
    <div class="flex items-center gap-1">
        <span th:text="${col}">Column</span>
        <span th:if="${sortBy == col}">
            <span th:if="${sortDir == 'asc'}">▲</span>
            <span th:if="${sortDir == 'desc'}">▼</span>
        </span>
        <span th:unless="${sortBy == col}" class="text-gray-300">⇅</span>
    </div>
</th>
```

### JavaScript
```javascript
function sortColumn(column) {
    const url = new URL(window.location.href);
    const currentSort = url.searchParams.get('sortBy');
    const currentDir = url.searchParams.get('sortDir') || 'asc';

    if (currentSort === column) {
        // Toggle direction
        url.searchParams.set('sortDir', currentDir === 'asc' ? 'desc' : 'asc');
    } else {
        url.searchParams.set('sortBy', column);
        url.searchParams.set('sortDir', 'asc');
    }
    url.searchParams.set('page', '0'); // Reset to first page
    window.location.href = url.toString();
}
```

## Test Plan

- [ ] Integration test: Sorting applies correctly
- [ ] Integration test: Sort direction toggles
- [ ] Integration test: Pagination resets on sort

## Parent Feature

Relates to F007-select-execution
