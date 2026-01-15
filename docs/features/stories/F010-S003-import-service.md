# F010-S003: Create Import Service (YAML Parsing)

## User Story

**As a** developer
**I want** an import service
**So that** exported configurations can be loaded

## Acceptance Criteria

- [ ] Given valid YAML, then queries parsed
- [ ] Given import, then format version validated
- [ ] Given new queries, then created with versions
- [ ] Given existing queries, then new versions added
- [ ] Given import, then connection names not validated (just stored)

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/ConfigImportService.java`
- `src/main/java/com/ivamare/dto/ImportResult.java`

### Import Service
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ConfigImportService {
    private final QueryRepository queryRepository;
    private final QueryVersionRepository versionRepository;
    private final Yaml yaml;

    public ImportResult importConfig(String yamlContent, String importedBy) {
        ExportedConfig config;
        try {
            config = yaml.loadAs(yamlContent, ExportedConfig.class);
        } catch (Exception e) {
            return ImportResult.failure("Invalid YAML format: " + e.getMessage());
        }

        // Validate format version
        if (!"1.0".equals(config.getFormatVersion())) {
            return ImportResult.failure("Unsupported format version: " + config.getFormatVersion());
        }

        List<String> added = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (ExportedQuery eq : config.getQueries()) {
            try {
                processQuery(eq, importedBy, added, updated, skipped);
            } catch (Exception e) {
                errors.add(eq.getName() + ": " + e.getMessage());
            }
        }

        return ImportResult.success(added, updated, skipped, errors);
    }

    private void processQuery(ExportedQuery eq, String importedBy,
                             List<String> added, List<String> updated, List<String> skipped) {
        Optional<Query> existing = queryRepository.findById(eq.getId());

        if (existing.isEmpty()) {
            // Create new query
            Query query = createQueryFromExport(eq, importedBy);
            queryRepository.save(query);
            added.add(eq.getName());
        } else {
            // Check if newer version exists
            Query query = existing.get();
            if (eq.getCurrentVersion() > query.getCurrentVersion()) {
                // Add new versions
                addVersionsFromExport(query, eq, importedBy);
                updated.add(eq.getName());
            } else {
                skipped.add(eq.getName() + " (no newer version)");
            }
        }
    }

    private Query createQueryFromExport(ExportedQuery eq, String importedBy) {
        Query query = Query.builder()
            .id(eq.getId())
            .name(eq.getName())
            .description(eq.getDescription())
            .category(eq.getCategory())
            .connectionName(eq.getConnectionName())
            .queryType(QueryType.valueOf(eq.getQueryType()))
            .currentVersion(eq.getCurrentVersion())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .createdBy(importedBy)
            .build();

        // Add all versions
        for (ExportedVersion ev : eq.getVersions()) {
            QueryVersion version = QueryVersion.builder()
                .id(UUID.randomUUID().toString())
                .query(query)
                .version(ev.getVersion())
                .configYaml(ev.getConfig())
                .createdAt(ev.getCreatedAt())
                .createdBy(ev.getCreatedBy())
                .build();
            query.getVersions().add(version);
        }

        return query;
    }
}
```

### Import Result
```java
@Data
@Builder
public class ImportResult {
    private boolean success;
    private String errorMessage;
    private List<String> added;
    private List<String> updated;
    private List<String> skipped;
    private List<String> errors;

    public static ImportResult success(List<String> added, List<String> updated,
                                       List<String> skipped, List<String> errors) {
        return ImportResult.builder()
            .success(errors.isEmpty())
            .added(added)
            .updated(updated)
            .skipped(skipped)
            .errors(errors)
            .build();
    }

    public static ImportResult failure(String message) {
        return ImportResult.builder()
            .success(false)
            .errorMessage(message)
            .added(List.of())
            .updated(List.of())
            .skipped(List.of())
            .errors(List.of())
            .build();
    }
}
```

## Test Plan

- [ ] Unit test: Valid YAML parsed correctly
- [ ] Unit test: Invalid YAML returns error
- [ ] Integration test: New queries created
- [ ] Integration test: Existing queries updated

## Parent Feature

Relates to F010-config-export-import
