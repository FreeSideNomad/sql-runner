# F008-S005: Implement Step 4 - Execute Update

## User Story

**As a** user
**I want** the update to execute with progress indication
**So that** I know the operation is in progress

## Acceptance Criteria

- [ ] Given step 4, then backup created first
- [ ] Given backup complete, then update SQL executed
- [ ] Given execution, then progress spinner shown
- [ ] Given execution, then elapsed time displayed
- [ ] Given success, then proceed to step 5
- [ ] Given failure, then error displayed with rollback option

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/java/com/ivamare/service/UpdateExecutionService.java`
- `src/main/resources/templates/queries/update-steps.html` (fragment: step4)

### Controller
```java
@PostMapping("/execute")
public String executeUpdate(@PathVariable String queryId, Model model) {
    if (wizardState.getCurrentStep() < 3) {
        return "redirect:/queries/" + queryId + "/update";
    }

    wizardState.setCurrentStep(4);
    model.addAttribute("currentStep", 4);
    model.addAttribute("queryId", queryId);

    return "queries/update-wizard";
}

@PostMapping("/execute/run")
@ResponseBody
public ExecutionProgressDto runExecution(@PathVariable String queryId) {
    try {
        // Create backup
        BackupRecord backup = backupService.createBackup(
            queryId,
            wizardState.getPreviewData(),
            getCurrentUser()
        );
        wizardState.setBackupRecordId(backup.getId());

        // Execute update
        ExecutionResult result = updateExecutionService.executeUpdate(
            queryId,
            wizardState.getParameters(),
            getCurrentUser()
        );
        wizardState.setExecutionLogId(result.getExecutionLogId());

        return ExecutionProgressDto.success(result);
    } catch (Exception e) {
        return ExecutionProgressDto.failure(e.getMessage());
    }
}
```

### Step 4 Fragment
```html
<div th:fragment="step4">
    <h2 class="text-xl font-semibold mb-4">Step 4: Executing Update</h2>

    <div id="execution-progress" class="text-center py-12">
        <div class="animate-spin h-16 w-16 border-4 border-rbc-blue border-t-transparent rounded-full mx-auto mb-6"></div>
        <p class="text-lg font-medium text-gray-700">Creating backup and executing update...</p>
        <p class="text-gray-500 mt-2">Elapsed: <span id="elapsed">0.0</span>s</p>
        <p class="text-sm text-gray-400 mt-4">Do not close this page</p>
    </div>

    <div id="execution-error" class="hidden bg-red-50 border border-red-200 rounded-lg p-6">
        <h3 class="text-lg font-semibold text-red-800 mb-2">Execution Failed</h3>
        <p id="error-message" class="text-red-700 mb-4">Error message here</p>
        <div class="flex gap-4">
            <a th:href="@{/queries/{id}/update(id=${queryId})}"
               class="px-4 py-2 bg-gray-200 rounded-lg">Start Over</a>
        </div>
    </div>
</div>

<script th:inline="javascript">
(async function() {
    const startTime = Date.now();
    const timer = setInterval(() => {
        document.getElementById('elapsed').textContent =
            ((Date.now() - startTime) / 1000).toFixed(1);
    }, 100);

    try {
        const response = await fetch(`/queries/[[${queryId}]]/update/execute/run`, {
            method: 'POST'
        });
        const result = await response.json();
        clearInterval(timer);

        if (result.success) {
            window.location.href = `/queries/[[${queryId}]]/update/result`;
        } else {
            document.getElementById('execution-progress').classList.add('hidden');
            document.getElementById('execution-error').classList.remove('hidden');
            document.getElementById('error-message').textContent = result.error;
        }
    } catch (e) {
        clearInterval(timer);
        document.getElementById('execution-progress').classList.add('hidden');
        document.getElementById('execution-error').classList.remove('hidden');
        document.getElementById('error-message').textContent = e.message;
    }
})();
</script>
```

## Test Plan

- [ ] Integration test: Backup created before update
- [ ] Integration test: Update executes successfully
- [ ] Integration test: Failure shows error message

## Parent Feature

Relates to F008-update-workflow
