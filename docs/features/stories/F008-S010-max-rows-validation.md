# F008-S010: Add Max Rows Validation (100K Limit)

## User Story

**As a** system administrator
**I want** a maximum row limit for updates
**So that** accidental mass updates are prevented

## Acceptance Criteria

- [ ] Given preview > 100,000 rows, then update blocked
- [ ] Given blocked update, then clear error message
- [ ] Given blocked update, then can modify parameters
- [ ] Given limit, then configurable via application.yml
- [ ] Given limit check, then happens at preview step

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/config/AppProperties.java`
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/resources/application.yml`

### Configuration
```yaml
sqlrunner:
  update:
    max-affected-rows: 100000
```

### Properties
```java
@ConfigurationProperties(prefix = "sqlrunner.update")
@Data
public class UpdateProperties {
    private int maxAffectedRows = 100000;
}
```

### Validation in Preview Step
```java
@GetMapping("/preview")
public String previewStep(@PathVariable String queryId, Model model) {
    // ... execute selectSql ...

    ExecutionResult preview = queryExecutionService.executeSelect(...);

    // Check max rows limit
    int maxRows = updateProperties.getMaxAffectedRows();
    if (preview.getRowCount() > maxRows) {
        model.addAttribute("blocked", true);
        model.addAttribute("error", String.format(
            "Update blocked: %,d rows affected exceeds maximum limit of %,d rows. " +
            "Please narrow your criteria.",
            preview.getRowCount(), maxRows
        ));
        model.addAttribute("rowCount", preview.getRowCount());
        model.addAttribute("maxRows", maxRows);
    }

    // ... rest of preview logic ...
}
```

### UI for Blocked State
```html
<div th:if="${blocked}" class="bg-red-50 border-2 border-red-300 rounded-lg p-8 text-center">
    <div class="text-6xl mb-4">⚠️</div>
    <h3 class="text-xl font-semibold text-red-800 mb-2">Update Blocked</h3>
    <p class="text-red-700 mb-4" th:text="${error}">Error message</p>

    <div class="bg-white rounded-lg p-4 inline-block mb-6">
        <div class="text-4xl font-bold text-red-600" th:text="${#numbers.formatInteger(rowCount, 0, 'COMMA')}">0</div>
        <div class="text-gray-600">rows affected</div>
        <div class="text-sm text-gray-500 mt-1">
            Maximum: <span th:text="${#numbers.formatInteger(maxRows, 0, 'COMMA')}">100,000</span>
        </div>
    </div>

    <div>
        <a th:href="@{/queries/{id}/update(id=${query.id})}"
           class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
            ← Modify Parameters
        </a>
    </div>
</div>
```

### Admin Override (Optional)
```java
// For ADMIN role, can override with confirmation
@PostMapping("/preview/override")
@PreAuthorize("hasRole('ADMIN')")
public String overrideMaxRows(@PathVariable String queryId,
                             @RequestParam boolean confirmed) {
    if (confirmed) {
        wizardState.setMaxRowsOverridden(true);
        return "redirect:/queries/" + queryId + "/update/confirm";
    }
    return "redirect:/queries/" + queryId + "/update/preview";
}
```

## Test Plan

- [ ] Integration test: > 100K rows blocked
- [ ] Integration test: Exact limit (100,000) allowed
- [ ] Integration test: Error message displays count
- [ ] Unit test: Configurable limit respected

## Parent Feature

Relates to F008-update-workflow
