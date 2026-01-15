# F007-S003: Implement LIST Parameter Textarea

## User Story

**As a** user
**I want** to enter multiple values for LIST parameters
**So that** I can filter by multiple IDs or values

## Acceptance Criteria

- [ ] Given LIST_STRING type, then textarea displayed
- [ ] Given LIST_INTEGER type, then textarea with number validation
- [ ] Given one value per line, then parsed correctly
- [ ] Given comma-separated values, then also parsed
- [ ] Given empty lines, then ignored
- [ ] Given IN clause, then values expanded correctly

## Technical Notes

### Files to Modify
- `src/main/resources/templates/fragments/parameter-form.html`
- `src/main/java/com/ivamare/service/QueryExecutionService.java`

### YAML Config Example
```yaml
parameters:
  - name: customerIds
    type: LIST_INTEGER
    label: "Customer IDs (one per line)"
    required: true

sql: |
  SELECT * FROM customers
  WHERE id IN (:customerIds)
```

### Textarea Template
```html
<div th:if="${param.type.name().startsWith('LIST_')}">
    <textarea th:id="${param.name}" th:name="${param.name}"
              th:required="${param.required}"
              rows="4"
              placeholder="Enter values (one per line or comma-separated)"
              class="w-full border rounded-lg px-3 py-2 font-mono text-sm"></textarea>
    <p class="text-xs text-gray-500 mt-1">Enter one value per line, or separate with commas</p>
</div>
```

### List Parsing Service
```java
public List<?> parseListParameter(String value, ParameterType type) {
    if (StringUtils.isBlank(value)) {
        return List.of();
    }

    String[] parts = value.split("[,\\n\\r]+");
    List<String> values = Arrays.stream(parts)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    if (type == ParameterType.LIST_INTEGER) {
        return values.stream()
            .map(Integer::parseInt)
            .toList();
    }
    return values;
}
```

### Named Parameter Handling
```java
// NamedParameterJdbcTemplate handles IN clause expansion automatically
Map<String, Object> params = new HashMap<>();
params.put("customerIds", parseListParameter(input, ParameterType.LIST_INTEGER));
// SELECT * FROM customers WHERE id IN (:customerIds)
// Expands to: SELECT * FROM customers WHERE id IN (1, 2, 3)
```

## Test Plan

- [ ] Unit test: List parsing with newlines
- [ ] Unit test: List parsing with commas
- [ ] Unit test: Integer list validation
- [ ] Integration test: IN clause expansion works

## Parent Feature

Relates to F007-select-execution
