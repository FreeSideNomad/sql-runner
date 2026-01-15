# F010-S005: Build Import Page with File Upload

## User Story

**As an** administrator
**I want** to upload and import configuration files
**So that** I can promote queries from another environment

## Acceptance Criteria

- [ ] Given import page, then file upload field available
- [ ] Given file upload, then validation runs first
- [ ] Given validation errors, then displayed to user
- [ ] Given validation pass, then preview shown
- [ ] Given confirmation, then import executes
- [ ] Given success, then summary displayed

## Technical Notes

### Files to Create
- `src/main/resources/templates/admin/import.html`

### Controller
```java
@GetMapping("/import")
public String importPage(Model model) {
    model.addAttribute("pageTitle", "Import Configuration");
    return "admin/import";
}

@PostMapping("/import/validate")
public String validateImport(@RequestParam("file") MultipartFile file,
                            Model model,
                            RedirectAttributes redirectAttributes) throws IOException {
    if (file.isEmpty()) {
        redirectAttributes.addFlashAttribute("error", "Please select a file");
        return "redirect:/admin/config/import";
    }

    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    ValidationResult validation = importService.validateImport(content);

    if (!validation.isValid()) {
        model.addAttribute("errors", validation.getErrors());
        model.addAttribute("pageTitle", "Import Configuration");
        return "admin/import";
    }

    // Store content in session for confirmation
    session.setAttribute("importContent", content);

    ImportPreview preview = importService.calculatePreview(validation.getParsedConfig());
    model.addAttribute("pageTitle", "Confirm Import");
    model.addAttribute("preview", preview);
    model.addAttribute("warnings", validation.getWarnings());
    return "admin/import-confirm";
}

@PostMapping("/import/execute")
public String executeImport(Authentication auth, RedirectAttributes redirectAttributes) {
    String content = (String) session.getAttribute("importContent");
    if (content == null) {
        redirectAttributes.addFlashAttribute("error", "Session expired. Please upload the file again.");
        return "redirect:/admin/config/import";
    }

    ImportResult result = importService.importConfig(content, auth.getName());
    session.removeAttribute("importContent");

    redirectAttributes.addFlashAttribute("result", result);
    return "redirect:/admin/config/import/complete";
}
```

### Import Page Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">Import Configuration</h1>

    <!-- Error Messages -->
    <div th:if="${errors}" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <h3 class="font-semibold text-red-800 mb-2">Validation Errors</h3>
        <ul class="list-disc list-inside text-red-700">
            <li th:each="error : ${errors}" th:text="${error}">Error message</li>
        </ul>
    </div>

    <div class="bg-white rounded-lg shadow p-6">
        <form th:action="@{/admin/config/import/validate}" method="post"
              enctype="multipart/form-data">
            <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Select YAML Configuration File
                </label>
                <input type="file" name="file" accept=".yaml,.yml" required
                       class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4
                              file:rounded-lg file:border-0 file:text-sm file:font-semibold
                              file:bg-rbc-blue file:text-white hover:file:bg-rbc-blue-light">
            </div>

            <button type="submit" class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
                Validate & Preview
            </button>
        </form>
    </div>

    <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mt-6">
        <h3 class="font-medium text-yellow-800 mb-2">Import Notes</h3>
        <ul class="text-sm text-yellow-700 list-disc list-inside">
            <li>File must be a valid YAML export from SQL Runner</li>
            <li>New queries will be added; existing queries will get new versions</li>
            <li>Connection names must match this environment's configuration</li>
            <li>Import does not delete existing queries</li>
        </ul>
    </div>
</div>
</html>
```

### Confirm Page Template
```html
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">Confirm Import</h1>

    <!-- Warnings -->
    <div th:if="${!warnings.isEmpty()}" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
        <h3 class="font-semibold text-yellow-800 mb-2">Warnings</h3>
        <ul class="list-disc list-inside text-yellow-700">
            <li th:each="warning : ${warnings}" th:text="${warning}">Warning</li>
        </ul>
    </div>

    <!-- Preview Table -->
    <div class="bg-white rounded-lg shadow overflow-hidden mb-6">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
                <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Query</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                    <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
                <tr th:each="item : ${preview.items}">
                    <td class="px-4 py-3" th:text="${item.name}">Query Name</td>
                    <td class="px-4 py-3">
                        <span th:class="${item.action == 'ADD' ? 'bg-green-100 text-green-800' :
                                         item.action == 'UPDATE' ? 'bg-blue-100 text-blue-800' :
                                         'bg-gray-100 text-gray-600'}"
                              class="px-2 py-1 rounded text-xs font-medium"
                              th:text="${item.action}">ADD</span>
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600" th:text="${item.details}">Details</td>
                </tr>
            </tbody>
        </table>
    </div>

    <form th:action="@{/admin/config/import/execute}" method="post" class="flex gap-4">
        <a href="/admin/config/import" class="px-6 py-2 border rounded-lg">Cancel</a>
        <button type="submit" class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
            Execute Import
        </button>
    </form>
</div>
```

## Test Plan

- [ ] Integration test: File upload works
- [ ] Integration test: Validation errors shown
- [ ] Integration test: Preview displays correctly
- [ ] Integration test: Import executes successfully

## Parent Feature

Relates to F010-config-export-import
