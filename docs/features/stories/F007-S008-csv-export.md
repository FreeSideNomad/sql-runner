# F007-S008: Implement CSV Export (Streaming)

## User Story

**As a** user
**I want** to export query results to CSV
**So that** I can analyze data in spreadsheets

## Acceptance Criteria

- [ ] Given export button, then CSV file downloads
- [ ] Given large results, then streaming response used
- [ ] Given CSV format, then UTF-8 with BOM for Excel
- [ ] Given CSV, then headers from column names
- [ ] Given special characters, then properly escaped
- [ ] Given filename, then includes query name and timestamp

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/ExportController.java`
- `src/main/java/com/ivamare/service/CsvExportService.java`

### Controller
```java
@Controller
@RequestMapping("/queries")
public class ExportController {

    @GetMapping("/{id}/export")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @PathVariable String id,
            @RequestParam Map<String, String> params) {

        Query query = queryService.getQueryById(id);
        ExecutionResult result = executionService.execute(id, params, getCurrentUser());

        String filename = String.format("%s_%s.csv",
            sanitizeFilename(query.getName()),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        StreamingResponseBody stream = outputStream -> {
            csvExportService.writeCsv(result, outputStream);
        };

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(stream);
    }
}
```

### CSV Export Service
```java
@Service
public class CsvExportService {
    private static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    public void writeCsv(ExecutionResult result, OutputStream outputStream) throws IOException {
        // Write UTF-8 BOM for Excel compatibility
        outputStream.write(UTF8_BOM);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            // Write header
            writer.println(result.getColumns().stream()
                .map(this::escapeCsv)
                .collect(Collectors.joining(",")));

            // Write data rows
            for (Map<String, Object> row : result.getRows()) {
                String line = result.getColumns().stream()
                    .map(col -> escapeCsv(formatValue(row.get(col))))
                    .collect(Collectors.joining(","));
                writer.println(line);
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

    private String formatValue(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.toString();
    }
}
```

## Test Plan

- [ ] Unit test: CSV escaping works correctly
- [ ] Integration test: CSV downloads successfully
- [ ] Integration test: Large file streams without memory issues

## Parent Feature

Relates to F007-select-execution
