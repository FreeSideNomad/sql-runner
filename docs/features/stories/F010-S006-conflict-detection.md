# F010-S006: Add Conflict Detection and Reporting

## User Story

**As an** administrator
**I want** to be warned about conflicts during import
**So that** I don't accidentally overwrite important configurations

## Acceptance Criteria

- [ ] Given same query ID with different content, then conflict detected
- [ ] Given conflict, then side-by-side comparison available
- [ ] Given conflict, then user can choose: skip, overwrite, or cancel
- [ ] Given version number conflict, then import requires higher version
- [ ] Given connection mismatch, then warning displayed

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/service/ConfigImportService.java`
- `src/main/java/com/ivamare/dto/ImportConflict.java`

### Conflict Detection
```java
public List<ImportConflict> detectConflicts(ExportedConfig config) {
    List<ImportConflict> conflicts = new ArrayList<>();

    for (ExportedQuery eq : config.getQueries()) {
        Optional<Query> existing = queryRepository.findById(eq.getId());

        if (existing.isPresent()) {
            Query query = existing.get();

            // Version conflict - import has lower or equal version
            if (eq.getCurrentVersion() <= query.getCurrentVersion()) {
                conflicts.add(ImportConflict.builder()
                    .queryId(eq.getId())
                    .queryName(eq.getName())
                    .type(ConflictType.VERSION)
                    .message(String.format("Import version %d <= current version %d",
                        eq.getCurrentVersion(), query.getCurrentVersion()))
                    .existingValue("Version " + query.getCurrentVersion())
                    .importValue("Version " + eq.getCurrentVersion())
                    .build());
            }

            // Name mismatch
            if (!eq.getName().equals(query.getName())) {
                conflicts.add(ImportConflict.builder()
                    .queryId(eq.getId())
                    .queryName(eq.getName())
                    .type(ConflictType.NAME_MISMATCH)
                    .message("Query name differs from existing")
                    .existingValue(query.getName())
                    .importValue(eq.getName())
                    .build());
            }

            // Connection change
            if (!eq.getConnectionName().equals(query.getConnectionName())) {
                conflicts.add(ImportConflict.builder()
                    .queryId(eq.getId())
                    .queryName(eq.getName())
                    .type(ConflictType.CONNECTION_CHANGE)
                    .message("Connection name differs")
                    .existingValue(query.getConnectionName())
                    .importValue(eq.getConnectionName())
                    .build());
            }
        }
    }

    return conflicts;
}
```

### Conflict DTO
```java
@Data
@Builder
public class ImportConflict {
    private String queryId;
    private String queryName;
    private ConflictType type;
    private String message;
    private String existingValue;
    private String importValue;
}

public enum ConflictType {
    VERSION,
    NAME_MISMATCH,
    CONNECTION_CHANGE
}
```

### Conflict Resolution UI
```html
<div th:if="${!conflicts.isEmpty()}" class="bg-orange-50 border border-orange-200 rounded-lg p-6 mb-6">
    <h3 class="font-semibold text-orange-800 mb-4">Conflicts Detected</h3>

    <div th:each="conflict : ${conflicts}" class="bg-white rounded-lg p-4 mb-4 border border-orange-200">
        <div class="flex justify-between items-start mb-2">
            <span class="font-medium" th:text="${conflict.queryName}">Query Name</span>
            <span th:class="${conflict.type.name() == 'VERSION' ? 'bg-red-100 text-red-800' :
                             conflict.type.name() == 'NAME_MISMATCH' ? 'bg-yellow-100 text-yellow-800' :
                             'bg-blue-100 text-blue-800'}"
                  class="px-2 py-1 rounded text-xs font-medium"
                  th:text="${conflict.type}">TYPE</span>
        </div>

        <p class="text-sm text-orange-700 mb-3" th:text="${conflict.message}">Message</p>

        <div class="grid grid-cols-2 gap-4 text-sm">
            <div class="bg-gray-50 p-2 rounded">
                <span class="text-gray-500">Current:</span>
                <span th:text="${conflict.existingValue}">Value</span>
            </div>
            <div class="bg-orange-100 p-2 rounded">
                <span class="text-gray-500">Import:</span>
                <span th:text="${conflict.importValue}">Value</span>
            </div>
        </div>

        <!-- Resolution Options -->
        <div class="mt-3 flex gap-2">
            <label class="inline-flex items-center">
                <input type="radio" th:name="'resolution_' + ${conflict.queryId}" value="skip" checked
                       class="form-radio text-orange-600">
                <span class="ml-2 text-sm">Skip</span>
            </label>
            <label th:if="${conflict.type.name() != 'VERSION'}" class="inline-flex items-center">
                <input type="radio" th:name="'resolution_' + ${conflict.queryId}" value="overwrite"
                       class="form-radio text-orange-600">
                <span class="ml-2 text-sm">Use Import Value</span>
            </label>
        </div>
    </div>
</div>
```

## Test Plan

- [ ] Unit test: Version conflict detected
- [ ] Unit test: Name mismatch detected
- [ ] Integration test: Conflict UI shows options
- [ ] Integration test: Resolution options work

## Parent Feature

Relates to F010-config-export-import
