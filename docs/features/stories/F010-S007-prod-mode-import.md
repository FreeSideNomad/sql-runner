# F010-S007: Implement Prod Mode Restrictions

## User Story

**As a** security administrator
**I want** import to be the only way to add queries in production
**So that** all changes go through proper promotion process

## Acceptance Criteria

- [ ] Given prod mode, then direct query creation disabled
- [ ] Given prod mode, then direct query editing disabled
- [ ] Given prod mode, then import still works for ADMIN
- [ ] Given prod mode, then clear indication in UI
- [ ] Given import in prod, then audit log entry created

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/config/AppProperties.java`
- `src/main/java/com/ivamare/controller/ConfigController.java`
- `src/main/java/com/ivamare/service/ConfigImportService.java`

### Configuration
```yaml
# application-prod.yml
sqlrunner:
  read-only-mode: true
  require-import-audit: true
```

### Import Audit Log
```java
@Entity
@Table(name = "import_audit", schema = "sqlrunner")
@Data
@Builder
public class ImportAudit {
    @Id
    private String id;

    @Column(name = "imported_by", nullable = false)
    private String importedBy;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "queries_added")
    private int queriesAdded;

    @Column(name = "queries_updated")
    private int queriesUpdated;

    @Column(name = "queries_skipped")
    private int queriesSkipped;

    @Column(name = "source_environment")
    private String sourceEnvironment;
}
```

### Enhanced Import with Audit
```java
@Transactional
public ImportResult importConfig(String yamlContent, String importedBy) {
    // Existing import logic...
    ImportResult result = performImport(yamlContent, importedBy);

    // Create audit record in prod mode
    if (appProperties.isRequireImportAudit()) {
        ImportAudit audit = ImportAudit.builder()
            .id(UUID.randomUUID().toString())
            .importedBy(importedBy)
            .importedAt(LocalDateTime.now())
            .fileHash(DigestUtils.sha256Hex(yamlContent))
            .queriesAdded(result.getAdded().size())
            .queriesUpdated(result.getUpdated().size())
            .queriesSkipped(result.getSkipped().size())
            .sourceEnvironment(extractSourceEnvironment(yamlContent))
            .build();

        importAuditRepository.save(audit);
        log.info("Import audit created: {} by {} - {} added, {} updated",
            audit.getId(), importedBy, audit.getQueriesAdded(), audit.getQueriesUpdated());
    }

    return result;
}
```

### Prod Mode UI Indicator
```html
<!-- In layout header or navigation -->
<div th:if="${@appProperties.readOnlyMode}"
     class="bg-green-600 text-white text-xs px-3 py-1 rounded-full">
    PROD
</div>

<!-- In import page -->
<div th:if="${@appProperties.readOnlyMode}"
     class="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
    <p class="text-green-800">
        <strong>Production Mode:</strong> Import is the only way to add or update queries.
        All imports are logged for audit purposes.
    </p>
</div>
```

### Access Control Enforcement
```java
@PostMapping("/save")
@PreAuthorize("hasRole('ADMIN')")
public String saveQuery(@Valid @ModelAttribute QueryFormDto dto, ...) {
    if (appProperties.isReadOnlyMode()) {
        throw new AccessDeniedException(
            "Direct query modification is disabled in production. Use Import instead.");
    }
    // ... save logic
}
```

### Import Audit View (ADMIN)
```java
@GetMapping("/import/audit")
@PreAuthorize("hasRole('ADMIN')")
public String importAuditLog(Model model) {
    List<ImportAudit> audits = importAuditRepository.findAllByOrderByImportedAtDesc();
    model.addAttribute("audits", audits);
    model.addAttribute("pageTitle", "Import Audit Log");
    return "admin/import-audit";
}
```

## Test Plan

- [ ] Integration test: Create blocked in prod mode
- [ ] Integration test: Edit blocked in prod mode
- [ ] Integration test: Import works in prod mode
- [ ] Integration test: Audit record created

## Parent Feature

Relates to F010-config-export-import
