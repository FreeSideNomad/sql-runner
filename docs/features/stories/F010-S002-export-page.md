# F010-S002: Build Export Page with Download

## User Story

**As an** administrator
**I want** to export all query configurations
**So that** I can promote them to another environment

## Acceptance Criteria

- [ ] Given admin page, then Export link available
- [ ] Given export page, then summary of queries shown
- [ ] Given Export button click, then YAML file downloads
- [ ] Given filename, then includes timestamp
- [ ] Given ADMIN role, then access allowed

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/ConfigController.java`
- `src/main/resources/templates/admin/export.html`

### Controller
```java
@Controller
@RequestMapping("/admin/config")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConfigController {
    private final ConfigExportService exportService;
    private final QueryRepository queryRepository;

    @GetMapping("/export")
    public String exportPage(Model model) {
        List<Query> queries = queryRepository.findByIsActiveTrue();
        Map<String, Long> categoryCounts = queries.stream()
            .collect(Collectors.groupingBy(Query::getCategory, Collectors.counting()));

        model.addAttribute("pageTitle", "Export Configuration");
        model.addAttribute("queryCount", queries.size());
        model.addAttribute("categoryCounts", categoryCounts);
        return "admin/export";
    }

    @PostMapping("/export/download")
    public ResponseEntity<byte[]> downloadExport(Authentication auth) {
        String yaml = exportService.exportAll(auth.getName());

        String filename = String.format("sqlrunner-export-%s.yaml",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/x-yaml"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(yaml.getBytes(StandardCharsets.UTF_8));
    }
}
```

### Export Page Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">Export Configuration</h1>

    <div class="bg-white rounded-lg shadow p-6 mb-6">
        <h2 class="text-lg font-semibold mb-4">Export Summary</h2>

        <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="bg-gray-50 rounded-lg p-4">
                <span class="text-3xl font-bold text-rbc-blue" th:text="${queryCount}">0</span>
                <span class="block text-gray-600">Total Queries</span>
            </div>
            <div class="bg-gray-50 rounded-lg p-4">
                <span class="text-3xl font-bold text-rbc-blue" th:text="${categoryCounts.size()}">0</span>
                <span class="block text-gray-600">Categories</span>
            </div>
        </div>

        <h3 class="font-medium mb-2">Queries by Category</h3>
        <ul class="list-disc list-inside text-gray-600 mb-6">
            <li th:each="entry : ${categoryCounts}">
                <span th:text="${entry.key}">Category</span>:
                <span th:text="${entry.value}">0</span> queries
            </li>
        </ul>

        <form th:action="@{/admin/config/export/download}" method="post">
            <button type="submit" class="px-6 py-3 bg-rbc-blue text-white rounded-lg hover:bg-rbc-blue-light">
                Download YAML Export
            </button>
        </form>
    </div>

    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h3 class="font-medium text-blue-800 mb-2">Export Notes</h3>
        <ul class="text-sm text-blue-700 list-disc list-inside">
            <li>Export includes all active queries with their version history</li>
            <li>Connection names are preserved - ensure target environment has matching connections</li>
            <li>No credentials or secrets are included in the export</li>
            <li>Format version: 1.0</li>
        </ul>
    </div>
</div>
</html>
```

## Test Plan

- [ ] Integration test: Export page loads
- [ ] Integration test: Download returns YAML file
- [ ] Integration test: Non-admin cannot access

## Parent Feature

Relates to F010-config-export-import
