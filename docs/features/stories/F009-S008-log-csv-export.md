# F009-S008: Implement Log CSV Export

## User Story

**As a** user
**I want** to export execution logs to CSV
**So that** I can analyze execution history in spreadsheets

## Acceptance Criteria

- [ ] Given history page, then Export button available
- [ ] Given export, then current filters applied
- [ ] Given CSV, then all log fields included
- [ ] Given CSV, then filename includes date range
- [ ] Given large export, then streaming response used

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/service/LogExportService.java`

### Controller
```java
@GetMapping("/export")
public ResponseEntity<StreamingResponseBody> exportLogs(
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String queryId,
        @RequestParam(required = false) ExecutionStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        Authentication auth) {

    // Apply same access control
    if (!hasRole(auth, "ADMIN")) {
        user = auth.getName();
    }

    List<ExecutionLog> logs = logService.findAllWithFilters(user, queryId, status, startDate, endDate);

    String filename = String.format("execution-logs_%s_%s.csv",
        startDate != null ? startDate.toString() : "all",
        LocalDate.now().toString());

    StreamingResponseBody stream = outputStream -> {
        logExportService.writeCsv(logs, outputStream);
    };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(stream);
}
```

### Export Service
```java
@Service
@RequiredArgsConstructor
public class LogExportService {
    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private final QueryRepository queryRepository;

    public void writeCsv(List<ExecutionLog> logs, OutputStream outputStream) throws IOException {
        outputStream.write(UTF8_BOM);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            // Header
            writer.println("ID,Query,Version,Connection,User,Timestamp,Type,Status,Rows,Duration (ms),Error");

            // Cache query names
            Map<String, String> queryNames = new HashMap<>();

            for (ExecutionLog log : logs) {
                String queryName = queryNames.computeIfAbsent(log.getQueryId(),
                    id -> queryRepository.findById(id).map(Query::getName).orElse("Unknown"));

                writer.println(String.join(",",
                    escapeCsv(log.getId()),
                    escapeCsv(queryName),
                    String.valueOf(log.getQueryVersion()),
                    escapeCsv(log.getConnectionName()),
                    escapeCsv(log.getExecutedBy()),
                    log.getExecutedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    log.getExecutionType().name(),
                    log.getStatus().name(),
                    String.valueOf(log.getRowCount()),
                    String.valueOf(log.getExecutionTimeMs()),
                    escapeCsv(log.getErrorMessage() != null ? log.getErrorMessage() : "")
                ));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
```

### Export Button in UI
```html
<div class="flex gap-2">
    <a th:href="@{/history/export(user=${currentFilters.user},
                                  queryId=${currentFilters.queryId},
                                  status=${currentFilters.status},
                                  startDate=${currentFilters.startDate},
                                  endDate=${currentFilters.endDate})}"
       class="px-4 py-2 border rounded-lg hover:bg-gray-50">
        Export CSV
    </a>
</div>
```

## Test Plan

- [ ] Integration test: CSV downloads correctly
- [ ] Integration test: Filters applied to export
- [ ] Integration test: Large export streams correctly

## Parent Feature

Relates to F009-execution-logging
