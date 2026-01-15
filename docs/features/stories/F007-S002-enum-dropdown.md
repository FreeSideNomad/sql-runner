# F007-S002: Implement ENUM Parameter Dropdown

## User Story

**As a** user
**I want** dropdown selection for ENUM parameters
**So that** I can select from predefined options

## Acceptance Criteria

- [ ] Given ENUM parameter, then dropdown displayed
- [ ] Given options in config, then all options in dropdown
- [ ] Given required ENUM, then selection required
- [ ] Given optional ENUM, then empty option available
- [ ] Given default value, then pre-selected

## Technical Notes

### Files to Modify
- `src/main/resources/templates/fragments/parameter-form.html`

### YAML Config Example
```yaml
parameters:
  - name: status
    type: ENUM
    label: "Status Filter"
    required: true
    options:
      - ACTIVE
      - INACTIVE
      - PENDING
    defaultValue: ACTIVE
```

### Dropdown Template
```html
<select th:if="${param.type.name() == 'ENUM'}"
        th:id="${param.name}" th:name="${param.name}"
        th:required="${param.required}"
        class="w-full border rounded-lg px-3 py-2">
    <option th:unless="${param.required}" value="">-- Select --</option>
    <option th:each="opt : ${param.options}"
            th:value="${opt}"
            th:text="${opt}"
            th:selected="${opt == param.defaultValue}">
        Option
    </option>
</select>
```

## Test Plan

- [ ] Integration test: ENUM dropdown renders options
- [ ] Integration test: Default value pre-selected
- [ ] Integration test: Required validation works

## Parent Feature

Relates to F007-select-execution
