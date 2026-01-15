# F006-S004: Build Query List Page with Category Grouping

## User Story

**As a** user
**I want** to see all queries grouped by category
**So that** I can find the query I need

## Acceptance Criteria

- [ ] Given queries page, then queries displayed grouped by category
- [ ] Given category header, then collapsible section
- [ ] Given query card, then name, description, type shown
- [ ] Given query card, then click navigates to execute/view
- [ ] Given ADMIN role, then create new query button visible
- [ ] Given query type, then visual indicator (SELECT vs UPDATE)

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/QueryController.java`
- `src/main/resources/templates/queries/list.html`

### Controller
```java
@Controller
@RequestMapping("/queries")
@RequiredArgsConstructor
public class QueryController {
    private final QueryService queryService;

    @GetMapping
    public String listQueries(Model model) {
        model.addAttribute("pageTitle", "Queries");
        model.addAttribute("queriesByCategory", queryService.getQueriesGroupedByCategory());
        return "queries/list";
    }
}
```

### Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold text-rbc-blue">Queries</h1>
        <a sec:authorize="hasRole('ADMIN')" href="/queries/new"
           class="bg-rbc-yellow text-rbc-blue px-4 py-2 rounded-lg font-semibold hover:bg-rbc-yellow-light">
            + New Query
        </a>
    </div>

    <div th:each="entry : ${queriesByCategory}" class="mb-6">
        <button class="w-full text-left bg-gray-100 px-4 py-3 rounded-t-lg font-semibold flex justify-between"
                onclick="toggleCategory(this)">
            <span th:text="${entry.key}">Category Name</span>
            <span th:text="${entry.value.size()} + ' queries'">0 queries</span>
        </button>
        <div class="bg-white border border-t-0 rounded-b-lg p-4 grid gap-4">
            <div th:each="query : ${entry.value}" class="border rounded-lg p-4 hover:shadow transition">
                <div class="flex justify-between items-start">
                    <div>
                        <h3 class="font-semibold" th:text="${query.name}">Query Name</h3>
                        <p class="text-sm text-gray-600" th:text="${query.description}">Description</p>
                    </div>
                    <span th:class="${query.queryType.name() == 'SELECT' ? 'bg-blue-100 text-blue-800' : 'bg-orange-100 text-orange-800'}"
                          class="px-2 py-1 rounded text-xs font-medium"
                          th:text="${query.queryType}">SELECT</span>
                </div>
                <a th:href="@{/queries/{id}(id=${query.id})}"
                   class="text-rbc-blue hover:underline text-sm mt-2 inline-block">
                    View Details →
                </a>
            </div>
        </div>
    </div>
</div>
</html>
```

## Test Plan

- [ ] Integration test: Queries grouped by category
- [ ] Integration test: Empty state when no queries
- [ ] Visual test: Category sections collapsible

## Parent Feature

Relates to F006-query-management
