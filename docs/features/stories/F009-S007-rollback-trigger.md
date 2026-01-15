# F009-S007: Add Rollback Trigger from Log Detail

## User Story

**As a** user
**I want** to trigger rollback from execution history
**So that** I can undo changes after reviewing the log

## Acceptance Criteria

- [ ] Given UPDATE log with backup, then Rollback button visible
- [ ] Given Rollback click, then confirmation dialog shown
- [ ] Given confirmation, then rollback executed
- [ ] Given rollback success, then redirected to rollback log
- [ ] Given rollback failure, then error message displayed
- [ ] Given already rolled back, then button disabled

## Technical Notes

### Files to Modify
- `src/main/resources/templates/history/detail.html`
- `src/main/java/com/ivamare/controller/HistoryController.java`

### Rollback Button in Detail Page
```html
<div th:if="${backup != null && !backup.rolledBack}"
     class="bg-yellow-50 border border-yellow-200 rounded-lg p-6 mt-6">
    <h2 class="text-lg font-semibold text-yellow-800 mb-2">Rollback Available</h2>
    <p class="text-yellow-700 mb-4">
        This update can be rolled back to restore the original values.
        <strong>This action can only be performed once.</strong>
    </p>

    <form th:action="@{/history/{logId}/rollback(logId=${log.id})}" method="post"
          id="rollback-form">
        <div class="flex items-center gap-4">
            <label class="flex items-center gap-2">
                <input type="checkbox" id="confirm-rollback" required
                       class="h-4 w-4 text-yellow-600 border-gray-300 rounded">
                <span class="text-sm text-yellow-800">
                    I understand this will revert <span th:text="${backup.rowCount}">0</span> rows
                </span>
            </label>
        </div>
        <button type="submit" id="rollback-btn" disabled
                class="mt-4 px-6 py-2 bg-yellow-600 text-white rounded-lg disabled:opacity-50">
            Execute Rollback
        </button>
    </form>
</div>

<script>
document.getElementById('confirm-rollback')?.addEventListener('change', function() {
    document.getElementById('rollback-btn').disabled = !this.checked;
});
</script>
```

### Controller Endpoint
```java
@PostMapping("/{logId}/rollback")
public String rollbackFromLog(@PathVariable String logId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
    ExecutionLog log = logService.findById(logId)
        .orElseThrow(() -> new EntityNotFoundException("Log not found"));

    if (log.getBackupRecordId() == null) {
        redirectAttributes.addFlashAttribute("error", "No backup available for this execution");
        return "redirect:/history/" + logId;
    }

    try {
        RollbackResult result = rollbackService.executeRollback(
            log.getBackupRecordId(),
            log.getQueryId(),
            auth.getName()
        );

        redirectAttributes.addFlashAttribute("message",
            String.format("Rollback successful: %d rows restored in %dms",
                result.getRowCount(), result.getDurationMs()));

        // Get the rollback execution log ID
        BackupRecord backup = backupService.getBackup(log.getBackupRecordId());
        return "redirect:/history/" + backup.getRollbackExecutionLogId();
    } catch (IllegalStateException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/history/" + logId;
    } catch (RollbackException e) {
        redirectAttributes.addFlashAttribute("error", "Rollback failed: " + e.getMessage());
        return "redirect:/history/" + logId;
    }
}
```

### Already Rolled Back State
```html
<div th:if="${backup != null && backup.rolledBack}"
     class="bg-purple-50 border border-purple-200 rounded-lg p-6 mt-6">
    <h2 class="text-lg font-semibold text-purple-800 mb-2">Already Rolled Back</h2>
    <p class="text-purple-700">
        This update was rolled back on
        <span th:text="${#temporals.format(backup.rolledBackAt, 'MMM d, yyyy HH:mm')}">date</span>
        by <span th:text="${backup.rolledBackBy}">user</span>.
    </p>
    <a th:href="@{/history/{id}(id=${backup.rollbackExecutionLogId})}"
       class="mt-4 inline-block text-purple-600 hover:underline">
        View Rollback Execution →
    </a>
</div>
```

## Test Plan

- [ ] Integration test: Rollback from log detail works
- [ ] Integration test: Confirmation required
- [ ] Integration test: Already rolled back shows different state

## Parent Feature

Relates to F009-execution-logging
