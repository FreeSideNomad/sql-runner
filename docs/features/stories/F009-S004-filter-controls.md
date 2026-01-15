# F009-S004: Implement Filter Controls

## User Story

**As a** user
**I want** to filter execution history
**So that** I can find specific executions

## Acceptance Criteria

- [ ] Given filter panel, then date range picker available
- [ ] Given filter panel, then user dropdown (ADMIN only)
- [ ] Given filter panel, then query dropdown
- [ ] Given filter panel, then status dropdown
- [ ] Given filters, then applied on submit
- [ ] Given filters, then preserved in URL

## Technical Notes

### Files to Create
- `src/main/resources/templates/history/filters.html`

### Filter Panel Fragment
```html
<div th:fragment="filter-panel" class="bg-white rounded-lg shadow p-4 mb-6">
    <form method="get" action="/history" class="grid grid-cols-1 md:grid-cols-5 gap-4">
        <!-- Date Range -->
        <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input type="date" name="startDate" th:value="${currentFilters.startDate}"
                   class="w-full border rounded px-3 py-2">
        </div>
        <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input type="date" name="endDate" th:value="${currentFilters.endDate}"
                   class="w-full border rounded px-3 py-2">
        </div>

        <!-- User (ADMIN only) -->
        <div sec:authorize="hasRole('ADMIN')">
            <label class="block text-sm font-medium text-gray-700 mb-1">User</label>
            <input type="text" name="user" th:value="${currentFilters.user}"
                   placeholder="Username"
                   class="w-full border rounded px-3 py-2">
        </div>

        <!-- Query -->
        <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Query</label>
            <select name="queryId" class="w-full border rounded px-3 py-2">
                <option value="">All Queries</option>
                <option th:each="q : ${queries}"
                        th:value="${q.id}"
                        th:text="${q.name}"
                        th:selected="${q.id == currentFilters.queryId}">Query</option>
            </select>
        </div>

        <!-- Status -->
        <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select name="status" class="w-full border rounded px-3 py-2">
                <option value="">All Statuses</option>
                <option value="SUCCESS" th:selected="${currentFilters.status == 'SUCCESS'}">Success</option>
                <option value="FAILED" th:selected="${currentFilters.status == 'FAILED'}">Failed</option>
                <option value="CANCELLED" th:selected="${currentFilters.status == 'CANCELLED'}">Cancelled</option>
                <option value="TIMEOUT" th:selected="${currentFilters.status == 'TIMEOUT'}">Timeout</option>
            </select>
        </div>

        <!-- Buttons -->
        <div class="flex items-end gap-2">
            <button type="submit" class="px-4 py-2 bg-rbc-blue text-white rounded">
                Filter
            </button>
            <a href="/history" class="px-4 py-2 border rounded hover:bg-gray-50">
                Clear
            </a>
        </div>
    </form>
</div>
```

### Quick Filters
```html
<div class="flex gap-2 mb-4">
    <a href="/history?startDate=today" class="text-sm text-rbc-blue hover:underline">Today</a>
    <span class="text-gray-300">|</span>
    <a href="/history?startDate=week" class="text-sm text-rbc-blue hover:underline">This Week</a>
    <span class="text-gray-300">|</span>
    <a href="/history?status=FAILED" class="text-sm text-rbc-blue hover:underline">Failed Only</a>
</div>
```

### Controller Filter Handling
```java
// Handle quick filter shortcuts
if ("today".equals(startDateParam)) {
    startDate = LocalDate.now();
}
if ("week".equals(startDateParam)) {
    startDate = LocalDate.now().minusDays(7);
}
```

## Test Plan

- [ ] Integration test: Date filter works
- [ ] Integration test: Status filter works
- [ ] Integration test: Filters combine correctly

## Parent Feature

Relates to F009-execution-logging
