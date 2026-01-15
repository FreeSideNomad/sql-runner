# F006-S009: Add YAML Validation for Query Config

## User Story

**As an** administrator
**I want** YAML validation before saving
**So that** invalid configurations are caught early

## Acceptance Criteria

- [ ] Given invalid YAML syntax, then error message displayed
- [ ] Given missing required fields, then validation error
- [ ] Given unknown field in YAML, then warning shown
- [ ] Given valid YAML, then form submits successfully
- [ ] Given SELECT type, then sql field required
- [ ] Given UPDATE_WORKFLOW type, then selectSql, updateSql required

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/YamlConfigValidator.java`
- `src/main/java/com/ivamare/dto/QueryConfig.java`

### Config DTOs
```java
@Data
public class QueryConfig {
    private String sql;
    private String selectSql;
    private String updateSql;
    private List<String> rollbackColumns;
    private List<ParameterConfig> parameters;
}

@Data
public class ParameterConfig {
    private String name;
    private ParameterType type;
    private String label;
    private boolean required;
    private String regex;
    private String defaultValue;
    private List<String> options; // for ENUM type
}
```

### Validator Service
```java
@Service
public class YamlConfigValidator {
    private final Yaml yaml = new Yaml();

    public ValidationResult validate(String yamlContent, QueryType queryType) {
        try {
            QueryConfig config = yaml.loadAs(yamlContent, QueryConfig.class);

            List<String> errors = new ArrayList<>();

            if (queryType == QueryType.SELECT) {
                if (StringUtils.isBlank(config.getSql())) {
                    errors.add("'sql' field is required for SELECT queries");
                }
            } else if (queryType == QueryType.UPDATE_WORKFLOW) {
                if (StringUtils.isBlank(config.getSelectSql())) {
                    errors.add("'selectSql' field is required for UPDATE workflows");
                }
                if (StringUtils.isBlank(config.getUpdateSql())) {
                    errors.add("'updateSql' field is required for UPDATE workflows");
                }
            }

            // Validate parameters
            if (config.getParameters() != null) {
                for (int i = 0; i < config.getParameters().size(); i++) {
                    ParameterConfig param = config.getParameters().get(i);
                    if (StringUtils.isBlank(param.getName())) {
                        errors.add("Parameter at index " + i + " missing 'name'");
                    }
                    if (param.getType() == null) {
                        errors.add("Parameter '" + param.getName() + "' missing 'type'");
                    }
                }
            }

            return new ValidationResult(errors.isEmpty(), errors);
        } catch (YAMLException e) {
            return new ValidationResult(false, List.of("Invalid YAML syntax: " + e.getMessage()));
        }
    }
}
```

### Controller Validation
```java
@PostMapping("/validate-yaml")
@ResponseBody
public ValidationResult validateYaml(@RequestBody YamlValidationRequest request) {
    return yamlConfigValidator.validate(request.getYaml(), request.getQueryType());
}
```

## Test Plan

- [ ] Unit test: Invalid YAML syntax detected
- [ ] Unit test: Missing required fields detected
- [ ] Unit test: Valid config passes validation
- [ ] Integration test: Validation endpoint works

## Parent Feature

Relates to F006-query-management
