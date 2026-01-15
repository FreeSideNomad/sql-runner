# F006-S010: Implement Prod Mode Read-Only Behavior

## User Story

**As a** security administrator
**I want** editing disabled in production mode
**So that** query templates can only be changed via import

## Acceptance Criteria

- [ ] Given prod profile active, then create button hidden
- [ ] Given prod profile active, then edit button hidden
- [ ] Given prod profile active, then delete disabled
- [ ] Given prod profile active, then import still works (ADMIN)
- [ ] Given non-prod profile, then full editing enabled
- [ ] Given prod mode, then visual indicator in UI

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/config/AppProperties.java`
- `src/main/java/com/ivamare/controller/QueryController.java`
- `src/main/resources/templates/queries/list.html`
- `src/main/resources/templates/queries/detail.html`

### Configuration
```yaml
# application-prod.yml
sqlrunner:
  read-only-mode: true
```

### Properties Class
```java
@ConfigurationProperties(prefix = "sqlrunner")
@Data
public class AppProperties {
    private boolean readOnlyMode = false;
}
```

### Controller Advice
```java
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    private final AppProperties appProperties;

    @ModelAttribute("readOnlyMode")
    public boolean readOnlyMode() {
        return appProperties.isReadOnlyMode();
    }
}
```

### Template Conditionals
```html
<!-- Hide create button in prod mode -->
<a th:unless="${readOnlyMode}"
   sec:authorize="hasRole('ADMIN')"
   href="/queries/new"
   class="bg-rbc-yellow text-rbc-blue px-4 py-2 rounded-lg">
    + New Query
</a>

<!-- Show warning banner in prod mode -->
<div th:if="${readOnlyMode}" class="bg-yellow-100 border-l-4 border-yellow-500 p-4 mb-6">
    <p class="text-yellow-700">
        <strong>Production Mode:</strong> Query editing is disabled.
        Use Import to add or update queries.
    </p>
</div>
```

### Controller Guard
```java
@PostMapping("/save")
@PreAuthorize("hasRole('ADMIN')")
public String saveQuery(@Valid @ModelAttribute QueryFormDto dto, ...) {
    if (appProperties.isReadOnlyMode()) {
        throw new AccessDeniedException("Query editing is disabled in production mode");
    }
    // ... rest of save logic
}
```

## Test Plan

- [ ] Integration test: Create blocked in prod mode
- [ ] Integration test: Edit blocked in prod mode
- [ ] Integration test: Import works in prod mode
- [ ] Visual test: Warning banner displayed

## Parent Feature

Relates to F006-query-management
