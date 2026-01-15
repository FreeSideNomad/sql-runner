# F004-S007: Add Role-Based Navigation Visibility

## User Story

**As a** user
**I want** navigation items filtered by my role
**So that** I only see options I can access

## Acceptance Criteria

- [ ] Given ADMIN role, then all navigation items visible
- [ ] Given UPDATE_RUNNER role, then admin section hidden
- [ ] Given SELECT_RUNNER role, then admin section hidden
- [ ] Given any role, then Dashboard, Queries, History visible
- [ ] Given Spring Security integration, then sec:authorize works

## Technical Notes

### Files to Modify
- `pom.xml` (add thymeleaf-extras-springsecurity6)
- `src/main/resources/templates/fragments/sidebar.html`

### Maven Dependency
```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

### Security Dialect Usage
```html
<!-- Available to all authenticated users -->
<li sec:authorize="isAuthenticated()">
    <a href="/queries">Queries</a>
</li>

<!-- Admin only -->
<li sec:authorize="hasRole('ADMIN')">
    <a href="/admin/connections">Connections</a>
</li>

<!-- UPDATE_RUNNER and above -->
<li sec:authorize="hasAnyRole('ADMIN', 'UPDATE_RUNNER')">
    <a href="/queries/update">Update Workflows</a>
</li>
```

### Navigation Matrix
| Item | SELECT_RUNNER | UPDATE_RUNNER | ADMIN |
|------|---------------|---------------|-------|
| Dashboard | Yes | Yes | Yes |
| Queries | Yes | Yes | Yes |
| History | Yes | Yes | Yes |
| Admin Connections | No | No | Yes |
| Export/Import | No | No | Yes |

## Test Plan

- [ ] Integration test: Admin sees all navigation
- [ ] Integration test: SELECT_RUNNER doesn't see admin section
- [ ] Integration test: Navigation changes on role

## Parent Feature

Relates to F004-ui-layout
