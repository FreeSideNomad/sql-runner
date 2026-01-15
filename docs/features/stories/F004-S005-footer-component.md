# F004-S005: Build Footer Component

## User Story

**As a** user
**I want** a footer with version information
**So that** I know what version of the application is running

## Acceptance Criteria

- [ ] Given footer, then version number displayed
- [ ] Given footer, then environment indicator shown (DEV/PROD)
- [ ] Given footer, then copyright text present
- [ ] Given footer, then fixed to bottom of viewport
- [ ] Given DEV mode, then yellow highlight indicator

## Technical Notes

### Files to Create
- `src/main/resources/templates/fragments/footer.html`

### Footer Fragment
```html
<footer th:fragment="footer" class="bg-gray-800 text-gray-400 text-sm py-3 px-6 mt-auto">
    <div class="flex justify-between items-center">
        <span>SQL Runner v<span th:text="${@environment.getProperty('app.version', '1.0.0')}">1.0.0</span></span>

        <span th:if="${@environment.getProperty('spring.profiles.active', 'dev').contains('prod')}"
              class="px-2 py-1 bg-green-600 text-white rounded text-xs">PROD</span>
        <span th:unless="${@environment.getProperty('spring.profiles.active', 'dev').contains('prod')}"
              class="px-2 py-1 bg-rbc-yellow text-rbc-blue rounded text-xs font-bold">DEV</span>

        <span>&copy; 2024 SQL Runner</span>
    </div>
</footer>
```

### Version Property
```yaml
# application.yml
app:
  version: '@project.version@'  # Maven resource filtering
```

## Test Plan

- [ ] Unit test: Footer fragment renders
- [ ] Integration test: Version displays correctly
- [ ] Integration test: Environment indicator correct per profile

## Parent Feature

Relates to F004-ui-layout
