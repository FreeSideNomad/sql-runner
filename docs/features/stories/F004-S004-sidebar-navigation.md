# F004-S004: Build Sidebar Navigation Component

## User Story

**As a** user
**I want** a sidebar with navigation links
**So that** I can access different sections of the application

## Acceptance Criteria

- [ ] Given sidebar, then fixed to left side
- [ ] Given sidebar, then collapsible on mobile
- [ ] Given sidebar, then Dashboard link present
- [ ] Given sidebar, then Queries link present
- [ ] Given sidebar, then History link present
- [ ] Given sidebar, then Admin section present (ADMIN only)
- [ ] Given active page, then corresponding link highlighted

## Technical Notes

### Files to Create
- `src/main/resources/templates/fragments/sidebar.html`

### Navigation Items
| Label | URL | Roles | Icon |
|-------|-----|-------|------|
| Dashboard | / | All | Home |
| Queries | /queries | All | Database |
| History | /history | All | Clock |
| Connections | /admin/connections | ADMIN | Server |
| Export/Import | /admin/config | ADMIN | Download |

### Sidebar Fragment
```html
<aside th:fragment="sidebar" class="w-64 bg-white shadow-lg fixed top-16 bottom-0 left-0 overflow-y-auto">
    <nav class="p-4">
        <ul class="space-y-2">
            <li>
                <a href="/" th:classappend="${currentPage == 'dashboard'} ? 'bg-rbc-blue text-white' : 'hover:bg-gray-100'"
                   class="flex items-center px-4 py-2 rounded-lg transition">
                    <!-- Icon -->
                    <span>Dashboard</span>
                </a>
            </li>
            <!-- More items -->

            <!-- Admin Section -->
            <li sec:authorize="hasRole('ADMIN')" class="pt-4 border-t mt-4">
                <span class="text-xs text-gray-500 uppercase tracking-wider px-4">Admin</span>
                <!-- Admin links -->
            </li>
        </ul>
    </nav>
</aside>
```

### Active Page Detection
```java
@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute("currentPage")
    public String getCurrentPage(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Return page identifier
    }
}
```

## Test Plan

- [ ] Unit test: Sidebar fragment renders
- [ ] Integration test: Admin section hidden for non-admins
- [ ] Visual test: Active link highlighted

## Parent Feature

Relates to F004-ui-layout
