# F008-S006: Implement Step 5 - Result Display

## User Story

**As a** user
**I want** to see the result of my update
**So that** I know it completed successfully

## Acceptance Criteria

- [ ] Given step 5, then success message displayed
- [ ] Given result, then row count shown
- [ ] Given result, then execution time shown
- [ ] Given result, then Rollback button available
- [ ] Given result, then link to execution log
- [ ] Given rollback used, then rollback button disabled

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/resources/templates/queries/update-steps.html` (fragment: step5)

### Controller
```java
@GetMapping("/result")
public String resultStep(@PathVariable String queryId, Model model) {
    if (wizardState.getCurrentStep() < 4 || wizardState.getExecutionLogId() == null) {
        return "redirect:/queries/" + queryId + "/update";
    }

    wizardState.setCurrentStep(5);

    ExecutionLog log = executionLogRepository.findById(wizardState.getExecutionLogId()).orElseThrow();
    BackupRecord backup = backupRepository.findById(wizardState.getBackupRecordId()).orElse(null);

    model.addAttribute("executionLog", log);
    model.addAttribute("backup", backup);
    model.addAttribute("canRollback", backup != null && !backup.isRolledBack());
    model.addAttribute("currentStep", 5);

    return "queries/update-wizard";
}
```

### Step 5 Fragment
```html
<div th:fragment="step5">
    <div class="text-center mb-8">
        <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span class="text-3xl text-green-600">✓</span>
        </div>
        <h2 class="text-2xl font-semibold text-green-800">Update Completed Successfully</h2>
    </div>

    <div class="bg-white border rounded-lg divide-y mb-6">
        <div class="p-4 flex justify-between">
            <span class="text-gray-600">Records Updated</span>
            <span class="font-semibold" th:text="${executionLog.rowCount}">0</span>
        </div>
        <div class="p-4 flex justify-between">
            <span class="text-gray-600">Execution Time</span>
            <span th:text="${executionLog.executionTimeMs + 'ms'}">0ms</span>
        </div>
        <div class="p-4 flex justify-between">
            <span class="text-gray-600">Executed By</span>
            <span th:text="${executionLog.executedBy}">user</span>
        </div>
        <div class="p-4 flex justify-between">
            <span class="text-gray-600">Timestamp</span>
            <span th:text="${#temporals.format(executionLog.executedAt, 'MMM d, yyyy HH:mm:ss')}">
                Jan 1, 2024 12:00:00
            </span>
        </div>
    </div>

    <!-- Rollback Section -->
    <div th:if="${backup != null}" class="bg-yellow-50 border border-yellow-200 rounded-lg p-6 mb-6">
        <h3 class="font-semibold text-yellow-800 mb-2">Rollback Available</h3>
        <p class="text-yellow-700 text-sm mb-4">
            A backup was created before this update. You can rollback to restore the original values.
            <strong>This can only be done once.</strong>
        </p>

        <div th:if="${canRollback}">
            <form th:action="@{/queries/{id}/rollback(id=${backup.id})}" method="post">
                <button type="submit" class="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700">
                    Rollback Changes
                </button>
            </form>
        </div>
        <div th:unless="${canRollback}" class="text-gray-600">
            <span th:if="${backup.rolledBack}">
                ✓ Rolled back on <span th:text="${#temporals.format(backup.rolledBackAt, 'MMM d, yyyy HH:mm')}">date</span>
                by <span th:text="${backup.rolledBackBy}">user</span>
            </span>
        </div>
    </div>

    <div class="flex justify-between">
        <a th:href="@{/history/{id}(id=${executionLog.id})}" class="text-rbc-blue hover:underline">
            View Execution Log
        </a>
        <a href="/queries" class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
            Done
        </a>
    </div>
</div>
```

## Test Plan

- [ ] Visual test: Success message displayed
- [ ] Integration test: Execution details shown
- [ ] Integration test: Rollback button works

## Parent Feature

Relates to F008-update-workflow
