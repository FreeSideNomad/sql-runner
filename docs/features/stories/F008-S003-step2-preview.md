# F008-S003: Implement Step 2 - Preview with selectSql

## User Story

**As a** user
**I want** to preview affected records
**So that** I can verify what will be updated

## Acceptance Criteria

- [ ] Given step 2, then selectSql executed
- [ ] Given results, then displayed in table format
- [ ] Given row count, then displayed prominently
- [ ] Given > 100,000 rows, then error and blocked
- [ ] Given 0 rows, then warning displayed
- [ ] Given preview, then Continue or Go Back options

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/resources/templates/queries/update-steps.html` (fragment: step2)

### Controller
```java
@GetMapping("/preview")
public String previewStep(@PathVariable String queryId, Model model) {
    if (wizardState.getCurrentStep() < 2) {
        return "redirect:/queries/" + queryId + "/update";
    }

    Query query = queryService.getQueryById(queryId);
    QueryConfig config = queryService.getCurrentConfig(queryId);

    // Execute selectSql
    ExecutionResult preview = queryExecutionService.executeSelect(
        config.getSelectSql(),
        query.getConnectionName(),
        wizardState.getParameters()
    );

    // Check max rows
    if (preview.getRowCount() > 100000) {
        model.addAttribute("error", "Cannot update more than 100,000 rows at once. " +
            "Affected rows: " + preview.getRowCount());
        model.addAttribute("blocked", true);
    } else {
        wizardState.setPreviewData(preview.getRows());
        wizardState.setAffectedRowCount(preview.getRowCount());
    }

    model.addAttribute("query", query);
    model.addAttribute("preview", preview);
    model.addAttribute("currentStep", 2);

    return "queries/update-wizard";
}
```

### Step 2 Fragment
```html
<div th:fragment="step2">
    <h2 class="text-xl font-semibold mb-4">Step 2: Preview Affected Records</h2>

    <!-- Error: Too many rows -->
    <div th:if="${blocked}" class="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <p class="text-red-800 font-semibold text-lg" th:text="${error}">Error message</p>
        <a th:href="@{/queries/{id}/update(id=${query.id})}"
           class="mt-4 inline-block px-6 py-2 bg-gray-200 rounded-lg">
            ← Modify Parameters
        </a>
    </div>

    <!-- Preview results -->
    <div th:unless="${blocked}">
        <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <p class="font-semibold text-yellow-800">
                <span th:text="${preview.rowCount}">0</span> records will be affected
            </p>
        </div>

        <!-- Warning if 0 rows -->
        <div th:if="${preview.rowCount == 0}" class="bg-gray-50 border rounded-lg p-6 text-center mb-6">
            <p class="text-gray-600">No records match your criteria.</p>
        </div>

        <!-- Results table (first 100 rows preview) -->
        <div th:if="${preview.rowCount > 0}" class="overflow-x-auto mb-6">
            <table class="min-w-full divide-y divide-gray-200">
                <!-- ... table content ... -->
            </table>
            <p th:if="${preview.rowCount > 100}" class="text-sm text-gray-500 mt-2">
                Showing first 100 of <span th:text="${preview.rowCount}">0</span> rows
            </p>
        </div>

        <!-- Navigation -->
        <div class="flex justify-between">
            <a th:href="@{/queries/{id}/update(id=${query.id})}"
               class="px-6 py-2 border rounded-lg">← Back to Parameters</a>
            <form th:action="@{/queries/{id}/update/confirm(id=${query.id})}" method="post">
                <button type="submit" th:disabled="${preview.rowCount == 0}"
                        class="px-6 py-2 bg-rbc-blue text-white rounded-lg disabled:opacity-50">
                    Continue to Confirmation →
                </button>
            </form>
        </div>
    </div>
</div>
```

## Test Plan

- [ ] Integration test: selectSql executed correctly
- [ ] Integration test: > 100K rows blocked
- [ ] Integration test: 0 rows shows warning

## Parent Feature

Relates to F008-update-workflow
