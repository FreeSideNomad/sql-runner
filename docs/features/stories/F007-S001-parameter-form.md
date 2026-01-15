# F007-S001: Create Parameter Form Component (All Types)

## User Story

**As a** user
**I want** a dynamic form for query parameters
**So that** I can provide input values for execution

## Acceptance Criteria

- [ ] Given parameter config, then appropriate input rendered
- [ ] Given STRING type, then text input shown
- [ ] Given INTEGER type, then number input with validation
- [ ] Given DECIMAL type, then number input with decimals
- [ ] Given DATE type, then date picker shown
- [ ] Given DATETIME type, then datetime picker shown
- [ ] Given BOOLEAN type, then checkbox/toggle shown
- [ ] Given required parameter, then validation enforced

## Technical Notes

### Files to Create
- `src/main/resources/templates/fragments/parameter-form.html`
- `src/main/java/com/ivamare/domain/ParameterType.java`

### Parameter Type Enum
```java
public enum ParameterType {
    STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, ENUM, LIST_STRING, LIST_INTEGER
}
```

### Form Fragment
```html
<div th:fragment="parameter-input(param)" class="mb-4">
    <label th:for="${param.name}" class="block text-sm font-medium text-gray-700 mb-1">
        <span th:text="${param.label ?: param.name}">Parameter</span>
        <span th:if="${param.required}" class="text-red-500">*</span>
    </label>

    <!-- STRING -->
    <input th:if="${param.type.name() == 'STRING'}"
           type="text" th:id="${param.name}" th:name="${param.name}"
           th:required="${param.required}" th:pattern="${param.regex}"
           th:value="${param.defaultValue}"
           class="w-full border rounded-lg px-3 py-2">

    <!-- INTEGER -->
    <input th:if="${param.type.name() == 'INTEGER'}"
           type="number" step="1" th:id="${param.name}" th:name="${param.name}"
           th:required="${param.required}"
           class="w-full border rounded-lg px-3 py-2">

    <!-- DATE -->
    <input th:if="${param.type.name() == 'DATE'}"
           type="date" th:id="${param.name}" th:name="${param.name}"
           th:required="${param.required}"
           class="w-full border rounded-lg px-3 py-2">

    <!-- DATETIME -->
    <input th:if="${param.type.name() == 'DATETIME'}"
           type="datetime-local" th:id="${param.name}" th:name="${param.name}"
           th:required="${param.required}"
           class="w-full border rounded-lg px-3 py-2">

    <!-- BOOLEAN -->
    <input th:if="${param.type.name() == 'BOOLEAN'}"
           type="checkbox" th:id="${param.name}" th:name="${param.name}"
           class="h-4 w-4 text-rbc-blue border-gray-300 rounded">
</div>
```

## Test Plan

- [ ] Unit test: Each parameter type renders correctly
- [ ] Integration test: Required validation works
- [ ] Integration test: Regex validation works

## Parent Feature

Relates to F007-select-execution
