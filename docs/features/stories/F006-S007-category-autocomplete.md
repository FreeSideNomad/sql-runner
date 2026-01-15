# F006-S007: Implement Category Autocomplete

## User Story

**As an** administrator
**I want** category suggestions when typing
**So that** I can use existing categories or create new ones

## Acceptance Criteria

- [ ] Given category input, then autocomplete suggestions appear
- [ ] Given existing categories, then suggested in dropdown
- [ ] Given new category text, then can create new category
- [ ] Given category selection, then input populated
- [ ] Given empty categories, then no suggestions shown

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/QueryController.java`
- `src/main/resources/templates/queries/form.html`

### API Endpoint
```java
@GetMapping("/api/categories")
@ResponseBody
public List<String> getCategories() {
    return queryRepository.findDistinctCategories();
}
```

### HTML5 Datalist (Simple Approach)
```html
<input type="text" th:field="*{category}" list="categories"
       class="w-full border rounded-lg px-3 py-2">
<datalist id="categories">
    <option th:each="cat : ${existingCategories}" th:value="${cat}">
</datalist>
```

### JavaScript Autocomplete (Enhanced)
```javascript
const categoryInput = document.getElementById('category');
const datalist = document.getElementById('categories');

categoryInput.addEventListener('focus', async () => {
    const response = await fetch('/api/categories');
    const categories = await response.json();

    datalist.innerHTML = '';
    categories.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat;
        datalist.appendChild(option);
    });
});
```

### Controller Model Attribute
```java
@ModelAttribute("existingCategories")
public List<String> existingCategories() {
    return queryRepository.findDistinctCategories();
}
```

## Test Plan

- [ ] Integration test: API returns distinct categories
- [ ] Integration test: Datalist populated on form load
- [ ] Visual test: Autocomplete suggestions appear

## Parent Feature

Relates to F006-query-management
