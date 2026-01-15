# F008-S004: Implement Step 3 - Confirmation Summary

## User Story

**As a** user
**I want** a confirmation step before executing
**So that** I can verify my intent before making changes

## Acceptance Criteria

- [ ] Given step 3, then summary of operation displayed
- [ ] Given summary, then query name shown
- [ ] Given summary, then connection name shown
- [ ] Given summary, then row count shown prominently
- [ ] Given summary, then parameters listed
- [ ] Given confirmation, then checkbox required before Execute
- [ ] Given Execute button, then proceeds to execution

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/resources/templates/queries/update-steps.html` (fragment: step3)

### Controller
```java
@PostMapping("/confirm")
public String confirmStep(@PathVariable String queryId, Model model) {
    if (wizardState.getCurrentStep() < 2 || wizardState.getAffectedRowCount() == 0) {
        return "redirect:/queries/" + queryId + "/update";
    }

    wizardState.setCurrentStep(3);

    Query query = queryService.getQueryById(queryId);
    QueryConfig config = queryService.getCurrentConfig(queryId);

    model.addAttribute("query", query);
    model.addAttribute("config", config);
    model.addAttribute("parameters", wizardState.getParameters());
    model.addAttribute("affectedRows", wizardState.getAffectedRowCount());
    model.addAttribute("currentStep", 3);

    return "queries/update-wizard";
}
```

### Step 3 Fragment
```html
<div th:fragment="step3">
    <h2 class="text-xl font-semibold mb-4">Step 3: Confirm Update</h2>

    <div class="bg-white border rounded-lg divide-y">
        <!-- Query Info -->
        <div class="p-4">
            <h3 class="text-sm font-medium text-gray-500">Query</h3>
            <p class="text-lg font-semibold" th:text="${query.name}">Query Name</p>
        </div>

        <!-- Connection -->
        <div class="p-4">
            <h3 class="text-sm font-medium text-gray-500">Database Connection</h3>
            <p th:text="${query.connectionName}">Connection</p>
        </div>

        <!-- Affected Rows -->
        <div class="p-4 bg-yellow-50">
            <h3 class="text-sm font-medium text-gray-500">Records to Update</h3>
            <p class="text-2xl font-bold text-yellow-700" th:text="${affectedRows}">0</p>
        </div>

        <!-- Parameters -->
        <div class="p-4">
            <h3 class="text-sm font-medium text-gray-500 mb-2">Parameters</h3>
            <dl class="grid grid-cols-2 gap-2 text-sm">
                <div th:each="param : ${parameters}" class="contents">
                    <dt class="text-gray-600" th:text="${param.key}">Param</dt>
                    <dd class="font-medium" th:text="${param.value}">Value</dd>
                </div>
            </dl>
        </div>
    </div>

    <!-- Confirmation Checkbox -->
    <form th:action="@{/queries/{id}/update/execute(id=${query.id})}" method="post" class="mt-6">
        <div class="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <label class="flex items-start gap-3">
                <input type="checkbox" required id="confirm-checkbox"
                       class="mt-1 h-4 w-4 text-red-600 border-gray-300 rounded">
                <span class="text-red-800">
                    I understand that this action will modify
                    <strong th:text="${affectedRows}">0</strong> records and cannot be automatically undone.
                    I have reviewed the affected records and confirm this is correct.
                </span>
            </label>
        </div>

        <div class="flex justify-between">
            <a th:href="@{/queries/{id}/update/preview(id=${query.id})}"
               class="px-6 py-2 border rounded-lg">← Back to Preview</a>
            <button type="submit" id="execute-btn"
                    class="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
                Execute Update
            </button>
        </div>
    </form>
</div>

<script>
document.getElementById('confirm-checkbox').addEventListener('change', function() {
    document.getElementById('execute-btn').disabled = !this.checked;
});
</script>
```

## Test Plan

- [ ] Visual test: Summary displays all information
- [ ] Integration test: Checkbox required for execution
- [ ] Integration test: Back button returns to preview

## Parent Feature

Relates to F008-update-workflow
