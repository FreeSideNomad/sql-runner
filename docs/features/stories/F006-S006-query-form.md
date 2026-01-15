# F006-S006: Build Query Create/Edit Form

## User Story

**As an** administrator
**I want** a form to create and edit queries
**So that** I can manage query templates

## Acceptance Criteria

- [ ] Given create form, then all metadata fields present
- [ ] Given edit form, then existing values populated
- [ ] Given form submit, then validation applied
- [ ] Given successful save, then redirect to query list
- [ ] Given validation error, then error messages displayed
- [ ] Given ADMIN role, then form accessible

## Technical Notes

### Files to Create/Modify
- `src/main/java/com/ivamare/controller/QueryController.java`
- `src/main/resources/templates/queries/form.html`
- `src/main/java/com/ivamare/dto/QueryFormDto.java`

### Controller Methods
```java
@GetMapping("/new")
@PreAuthorize("hasRole('ADMIN')")
public String newQueryForm(Model model) {
    model.addAttribute("pageTitle", "New Query");
    model.addAttribute("query", new QueryFormDto());
    model.addAttribute("isEdit", false);
    return "queries/form";
}

@GetMapping("/{id}/edit")
@PreAuthorize("hasRole('ADMIN')")
public String editQueryForm(@PathVariable String id, Model model) {
    QueryFormDto dto = queryService.getQueryForEdit(id);
    model.addAttribute("pageTitle", "Edit Query");
    model.addAttribute("query", dto);
    model.addAttribute("isEdit", true);
    return "queries/form";
}

@PostMapping("/save")
@PreAuthorize("hasRole('ADMIN')")
public String saveQuery(@Valid @ModelAttribute("query") QueryFormDto dto,
                        BindingResult result,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
        return "queries/form";
    }

    if (dto.getId() == null) {
        queryService.createQuery(dto, auth.getName());
        redirectAttributes.addFlashAttribute("message", "Query created successfully");
    } else {
        queryService.updateQuery(dto.getId(), dto, auth.getName());
        redirectAttributes.addFlashAttribute("message", "Query updated successfully");
    }

    return "redirect:/queries";
}
```

### Form Template
```html
<form th:action="@{/queries/save}" method="post" th:object="${query}">
    <input type="hidden" th:field="*{id}">

    <div class="grid grid-cols-2 gap-6 mb-6">
        <div>
            <label class="block text-sm font-medium mb-1">Name *</label>
            <input type="text" th:field="*{name}" required
                   class="w-full border rounded-lg px-3 py-2">
        </div>
        <div>
            <label class="block text-sm font-medium mb-1">Category *</label>
            <input type="text" th:field="*{category}" required
                   id="category" list="categories"
                   class="w-full border rounded-lg px-3 py-2">
        </div>
    </div>

    <div class="mb-6">
        <label class="block text-sm font-medium mb-1">Description</label>
        <textarea th:field="*{description}" rows="2"
                  class="w-full border rounded-lg px-3 py-2"></textarea>
    </div>

    <div class="grid grid-cols-2 gap-6 mb-6">
        <div>
            <label class="block text-sm font-medium mb-1">Connection *</label>
            <select th:field="*{connectionName}" required>
                <!-- Connection options -->
            </select>
        </div>
        <div>
            <label class="block text-sm font-medium mb-1">Query Type *</label>
            <select th:field="*{queryType}" required>
                <option value="SELECT">SELECT</option>
                <option value="UPDATE_WORKFLOW">UPDATE Workflow</option>
            </select>
        </div>
    </div>

    <div class="mb-6">
        <label class="block text-sm font-medium mb-1">Configuration (YAML) *</label>
        <!-- CodeMirror editor here -->
    </div>

    <div class="flex justify-end gap-4">
        <a href="/queries" class="px-4 py-2 border rounded-lg">Cancel</a>
        <button type="submit" class="px-4 py-2 bg-rbc-blue text-white rounded-lg">
            Save Query
        </button>
    </div>
</form>
```

## Test Plan

- [ ] Integration test: Create form submits successfully
- [ ] Integration test: Edit form populates existing data
- [ ] Integration test: Validation errors display correctly

## Parent Feature

Relates to F006-query-management
