# F010-S004: Implement Import Validation

## User Story

**As an** administrator
**I want** imports to be validated before applying
**So that** invalid configurations are caught early

## Acceptance Criteria

- [ ] Given YAML upload, then structure validated
- [ ] Given validation, then preview of changes shown
- [ ] Given missing required fields, then error reported
- [ ] Given invalid query config, then error reported
- [ ] Given validation pass, then can proceed to import

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/ConfigImportService.java`
- `src/main/java/com/ivamare/dto/ValidationResult.java`

### Validation Service
```java
public ValidationResult validateImport(String yamlContent) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    ExportedConfig config;
    try {
        config = yaml.loadAs(yamlContent, ExportedConfig.class);
    } catch (Exception e) {
        return ValidationResult.invalid("YAML parse error: " + e.getMessage());
    }

    // Format version check
    if (config.getFormatVersion() == null) {
        errors.add("Missing formatVersion field");
    } else if (!"1.0".equals(config.getFormatVersion())) {
        errors.add("Unsupported format version: " + config.getFormatVersion());
    }

    // Validate each query
    for (int i = 0; i < config.getQueries().size(); i++) {
        ExportedQuery eq = config.getQueries().get(i);
        String prefix = "Query #" + (i + 1) + " (" + eq.getName() + ")";

        // Required fields
        if (StringUtils.isBlank(eq.getId())) {
            errors.add(prefix + ": missing id");
        }
        if (StringUtils.isBlank(eq.getName())) {
            errors.add(prefix + ": missing name");
        }
        if (StringUtils.isBlank(eq.getConnectionName())) {
            errors.add(prefix + ": missing connectionName");
        }
        if (eq.getQueryType() == null) {
            errors.add(prefix + ": missing queryType");
        }

        // Validate versions
        if (eq.getVersions() == null || eq.getVersions().isEmpty()) {
            errors.add(prefix + ": no versions defined");
        } else {
            for (ExportedVersion ev : eq.getVersions()) {
                if (StringUtils.isBlank(ev.getConfig())) {
                    errors.add(prefix + " v" + ev.getVersion() + ": missing config");
                } else {
                    // Validate config YAML
                    try {
                        QueryConfig qc = yaml.loadAs(ev.getConfig(), QueryConfig.class);
                        validateQueryConfig(qc, QueryType.valueOf(eq.getQueryType()), errors, prefix + " v" + ev.getVersion());
                    } catch (Exception e) {
                        errors.add(prefix + " v" + ev.getVersion() + ": invalid config YAML");
                    }
                }
            }
        }

        // Warnings
        if (!connectionRegistry.hasConnection(eq.getConnectionName())) {
            warnings.add(prefix + ": connection '" + eq.getConnectionName() + "' not found in this environment");
        }
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings, config);
}
```

### Validation Result
```java
@Data
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private ExportedConfig parsedConfig;

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error), List.of(), null);
    }
}
```

### Preview Calculation
```java
public ImportPreview calculatePreview(ExportedConfig config) {
    List<PreviewItem> items = new ArrayList<>();

    for (ExportedQuery eq : config.getQueries()) {
        Optional<Query> existing = queryRepository.findById(eq.getId());

        if (existing.isEmpty()) {
            items.add(new PreviewItem(eq.getName(), "ADD", "New query with " + eq.getVersions().size() + " version(s)"));
        } else {
            Query query = existing.get();
            if (eq.getCurrentVersion() > query.getCurrentVersion()) {
                int newVersions = eq.getCurrentVersion() - query.getCurrentVersion();
                items.add(new PreviewItem(eq.getName(), "UPDATE", newVersions + " new version(s)"));
            } else {
                items.add(new PreviewItem(eq.getName(), "SKIP", "No newer versions"));
            }
        }
    }

    return new ImportPreview(items);
}
```

## Test Plan

- [ ] Unit test: Invalid YAML caught
- [ ] Unit test: Missing required fields caught
- [ ] Unit test: Connection warnings generated
- [ ] Integration test: Preview shows correct actions

## Parent Feature

Relates to F010-config-export-import
