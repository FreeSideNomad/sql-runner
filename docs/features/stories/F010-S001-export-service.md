# F010-S001: Create Export Service (YAML Generation)

## User Story

**As a** developer
**I want** an export service
**So that** query configurations can be serialized to YAML

## Acceptance Criteria

- [ ] Given export request, then all queries included
- [ ] Given export, then metadata and config combined
- [ ] Given export, then version information preserved
- [ ] Given export, then format version included
- [ ] Given export, then credentials never included

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/ConfigExportService.java`
- `src/main/java/com/ivamare/dto/ExportedConfig.java`

### Export Format
```yaml
formatVersion: "1.0"
exportedAt: "2024-01-15T10:30:00"
exportedBy: "admin"
queries:
  - id: "uuid-1"
    name: "Customer Status Update"
    description: "Update customer status by region"
    category: "Customer Management"
    connectionName: "main-sqlserver"
    queryType: "UPDATE_WORKFLOW"
    currentVersion: 3
    versions:
      - version: 3
        createdAt: "2024-01-15T10:00:00"
        createdBy: "admin"
        config:
          selectSql: |
            SELECT id, name, status FROM customers WHERE region = :region
          updateSql: |
            UPDATE customers SET status = :newStatus WHERE region = :region
          parameters:
            - name: region
              type: STRING
              label: "Region"
              required: true
          rollbackColumns:
            - status
```

### Export Service
```java
@Service
@RequiredArgsConstructor
public class ConfigExportService {
    private final QueryRepository queryRepository;
    private final QueryVersionRepository versionRepository;
    private final Yaml yaml;

    public String exportAll(String exportedBy) {
        List<Query> queries = queryRepository.findByIsActiveTrue();

        ExportedConfig export = ExportedConfig.builder()
            .formatVersion("1.0")
            .exportedAt(LocalDateTime.now())
            .exportedBy(exportedBy)
            .queries(queries.stream()
                .map(this::toExportedQuery)
                .toList())
            .build();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        return yaml.dump(export);
    }

    private ExportedQuery toExportedQuery(Query query) {
        List<QueryVersion> versions = versionRepository.findByQueryIdOrderByVersionDesc(query.getId());

        return ExportedQuery.builder()
            .id(query.getId())
            .name(query.getName())
            .description(query.getDescription())
            .category(query.getCategory())
            .connectionName(query.getConnectionName())
            .queryType(query.getQueryType().name())
            .currentVersion(query.getCurrentVersion())
            .versions(versions.stream()
                .map(v -> ExportedVersion.builder()
                    .version(v.getVersion())
                    .createdAt(v.getCreatedAt())
                    .createdBy(v.getCreatedBy())
                    .config(v.getConfigYaml())
                    .build())
                .toList())
            .build();
    }
}
```

### DTOs
```java
@Data
@Builder
public class ExportedConfig {
    private String formatVersion;
    private LocalDateTime exportedAt;
    private String exportedBy;
    private List<ExportedQuery> queries;
}

@Data
@Builder
public class ExportedQuery {
    private String id;
    private String name;
    private String description;
    private String category;
    private String connectionName;
    private String queryType;
    private Integer currentVersion;
    private List<ExportedVersion> versions;
}

@Data
@Builder
public class ExportedVersion {
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private String config;
}
```

## Test Plan

- [ ] Unit test: YAML generated correctly
- [ ] Unit test: All queries included
- [ ] Integration test: Export file downloadable

## Parent Feature

Relates to F010-config-export-import
