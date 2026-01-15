# F009-S005: Create Log Detail View

## User Story

**As a** user
**I want** to view execution details
**So that** I can see exactly what was executed

## Acceptance Criteria

- [ ] Given log detail page, then all execution info displayed
- [ ] Given log detail, then query name and version shown
- [ ] Given log detail, then parameters displayed
- [ ] Given log detail, then error message shown if failed
- [ ] Given UPDATE log, then link to backup shown
- [ ] Given log detail, then execution time formatted

## Technical Notes

### Files to Create
- `src/main/resources/templates/history/detail.html`

### Controller
```java
@GetMapping("/{id}")
public String logDetail(@PathVariable String id, Model model) {
    ExecutionLog log = logService.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Log not found"));

    Query query = queryRepository.findById(log.getQueryId()).orElse(null);
    BackupRecord backup = log.getBackupRecordId() != null ?
        backupRepository.findById(log.getBackupRecordId()).orElse(null) : null;

    // Parse parameters JSON
    Map<String, Object> params = parseParameters(log.getParameters());

    model.addAttribute("pageTitle", "Execution Detail");
    model.addAttribute("log", log);
    model.addAttribute("query", query);
    model.addAttribute("backup", backup);
    model.addAttribute("params", params);

    return "history/detail";
}
```

### Detail Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold text-rbc-blue">Execution Detail</h1>
        <a href="/history" class="text-rbc-blue hover:underline">← Back to History</a>
    </div>

    <!-- Status Banner -->
    <div th:class="${log.status.name() == 'SUCCESS' ? 'bg-green-50 border-green-200' :
                    log.status.name() == 'FAILED' ? 'bg-red-50 border-red-200' :
                    'bg-yellow-50 border-yellow-200'}"
         class="border rounded-lg p-4 mb-6">
        <span th:class="${log.status.name() == 'SUCCESS' ? 'text-green-800' :
                         log.status.name() == 'FAILED' ? 'text-red-800' :
                         'text-yellow-800'}"
              class="font-semibold text-lg"
              th:text="${log.status}">SUCCESS</span>
        <span th:if="${log.errorMessage}" class="block mt-2 text-red-700" th:text="${log.errorMessage}">Error</span>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Execution Info -->
        <div class="bg-white rounded-lg shadow p-6">
            <h2 class="text-lg font-semibold mb-4">Execution Info</h2>
            <dl class="space-y-3">
                <div class="flex justify-between">
                    <dt class="text-gray-600">Query</dt>
                    <dd th:text="${query?.name ?: 'Unknown'}">Query Name</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Version</dt>
                    <dd th:text="${log.queryVersion}">1</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Type</dt>
                    <dd th:text="${log.executionType}">SELECT</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Connection</dt>
                    <dd th:text="${log.connectionName}">main-db</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">User</dt>
                    <dd th:text="${log.executedBy}">user</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Time</dt>
                    <dd th:text="${#temporals.format(log.executedAt, 'MMM d, yyyy HH:mm:ss')}">Jan 1, 2024 12:00:00</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Duration</dt>
                    <dd th:text="${log.executionTimeMs + 'ms'}">0ms</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-600">Rows Affected</dt>
                    <dd th:text="${log.rowCount}">0</dd>
                </div>
            </dl>
        </div>

        <!-- Parameters -->
        <div class="bg-white rounded-lg shadow p-6">
            <h2 class="text-lg font-semibold mb-4">Parameters</h2>
            <div th:if="${params.isEmpty()}" class="text-gray-500">No parameters</div>
            <dl th:unless="${params.isEmpty()}" class="space-y-2">
                <div th:each="param : ${params}" class="flex justify-between">
                    <dt class="text-gray-600" th:text="${param.key}">param</dt>
                    <dd class="font-mono text-sm" th:text="${param.value}">value</dd>
                </div>
            </dl>
        </div>
    </div>

    <!-- Backup Info (for UPDATE) -->
    <div th:if="${backup != null}" class="bg-white rounded-lg shadow p-6 mt-6">
        <h2 class="text-lg font-semibold mb-4">Backup Record</h2>
        <div th:replace="~{history/backup-info :: backup(${backup})}"></div>
    </div>
</div>
</html>
```

## Test Plan

- [ ] Integration test: Log detail page loads
- [ ] Integration test: Parameters displayed correctly
- [ ] Integration test: Backup info shown for UPDATE

## Parent Feature

Relates to F009-execution-logging
