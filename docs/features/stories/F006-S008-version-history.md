# F006-S008: Build Version History Component

## User Story

**As an** administrator
**I want** to view version history of a query
**So that** I can see how configuration has changed

## Acceptance Criteria

- [ ] Given query detail page, then version history section present
- [ ] Given version list, then version number, date, author shown
- [ ] Given version entry, then click shows YAML diff (optional)
- [ ] Given version entry, then can view full YAML
- [ ] Given current version, then highlighted in list

## Technical Notes

### Files to Create
- `src/main/resources/templates/queries/detail.html`
- `src/main/resources/templates/fragments/version-history.html`

### Controller Method
```java
@GetMapping("/{id}")
public String queryDetail(@PathVariable String id, Model model) {
    Query query = queryService.getQueryById(id);
    List<QueryVersion> versions = queryVersionRepository.findByQueryIdOrderByVersionDesc(query.getId());

    model.addAttribute("pageTitle", query.getName());
    model.addAttribute("query", QueryDto.from(query));
    model.addAttribute("versions", versions);
    model.addAttribute("currentConfig", queryService.getCurrentConfig(id));

    return "queries/detail";
}
```

### Version History Fragment
```html
<div th:fragment="version-history" class="bg-white rounded-lg shadow p-6">
    <h3 class="font-semibold text-lg mb-4">Version History</h3>

    <div class="space-y-3">
        <div th:each="version : ${versions}"
             th:class="${version.version == query.currentVersion} ? 'border-l-4 border-rbc-blue bg-blue-50' : 'border-l-4 border-gray-200'"
             class="pl-4 py-2">
            <div class="flex justify-between items-center">
                <div>
                    <span class="font-medium">Version <span th:text="${version.version}">1</span></span>
                    <span th:if="${version.version == query.currentVersion}"
                          class="ml-2 text-xs bg-rbc-blue text-white px-2 py-0.5 rounded">Current</span>
                </div>
                <button onclick="viewVersion(this)"
                        th:data-id="${version.id}"
                        class="text-sm text-rbc-blue hover:underline">
                    View YAML
                </button>
            </div>
            <div class="text-sm text-gray-600">
                <span th:text="${version.createdBy}">user</span> •
                <span th:text="${#temporals.format(version.createdAt, 'MMM d, yyyy HH:mm')}">Jan 1, 2024 12:00</span>
            </div>
        </div>
    </div>
</div>

<!-- Modal for viewing YAML -->
<div id="yaml-modal" class="hidden fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
    <div class="bg-white rounded-lg p-6 w-3/4 max-h-3/4 overflow-auto">
        <pre id="yaml-content" class="bg-gray-100 p-4 rounded text-sm overflow-auto"></pre>
        <button onclick="closeModal()" class="mt-4 px-4 py-2 bg-gray-200 rounded">Close</button>
    </div>
</div>
```

### View Version API
```java
@GetMapping("/api/versions/{versionId}")
@ResponseBody
public QueryVersionDto getVersion(@PathVariable String versionId) {
    return queryVersionRepository.findById(versionId)
        .map(QueryVersionDto::from)
        .orElseThrow();
}
```

## Test Plan

- [ ] Integration test: Version history loads
- [ ] Integration test: Current version highlighted
- [ ] Integration test: Version YAML viewable

## Parent Feature

Relates to F006-query-management
