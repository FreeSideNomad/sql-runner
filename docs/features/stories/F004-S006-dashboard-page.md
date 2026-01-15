# F004-S006: Create Dashboard Page (Placeholder)

## User Story

**As a** user
**I want** a dashboard as my landing page
**So that** I have a starting point after login

## Acceptance Criteria

- [ ] Given authenticated user, then dashboard displayed after login
- [ ] Given dashboard, then welcome message with user name
- [ ] Given dashboard, then quick links to common actions
- [ ] Given dashboard, then recent executions summary (placeholder)
- [ ] Given dashboard, then uses base layout template

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/DashboardController.java`
- `src/main/resources/templates/dashboard.html`

### Controller
```java
@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("username", auth.getName());
        return "dashboard";
    }
}
```

### Dashboard Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">
        Welcome, <span th:text="${username}">User</span>!
    </h1>

    <!-- Quick Actions -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <a href="/queries" class="bg-white p-6 rounded-lg shadow hover:shadow-lg transition">
            <h2 class="font-semibold text-lg">Browse Queries</h2>
            <p class="text-gray-600">View and execute saved queries</p>
        </a>
        <!-- More cards -->
    </div>

    <!-- Recent Activity Placeholder -->
    <div class="bg-white p-6 rounded-lg shadow">
        <h2 class="font-semibold text-lg mb-4">Recent Activity</h2>
        <p class="text-gray-500">Recent execution history will appear here.</p>
    </div>
</div>
</html>
```

## Test Plan

- [ ] Unit test: Dashboard controller returns correct view
- [ ] Integration test: Dashboard accessible after login
- [ ] Visual test: Layout renders correctly

## Parent Feature

Relates to F004-ui-layout
