# F004-S002: Create Base Thymeleaf Layout Template

## User Story

**As a** developer
**I want** a base layout template
**So that** all pages have consistent structure

## Acceptance Criteria

- [ ] Given base layout, then header fragment included
- [ ] Given base layout, then sidebar fragment included
- [ ] Given base layout, then footer fragment included
- [ ] Given base layout, then content area replaceable
- [ ] Given child pages, then extend base layout
- [ ] Given layout, then responsive on desktop

## Technical Notes

### Files to Create
- `src/main/resources/templates/layout/base.html`
- `src/main/resources/templates/fragments/` (directory)

### Base Layout Structure
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${pageTitle} + ' - SQL Runner'">SQL Runner</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <!-- Tailwind config -->
</head>
<body class="bg-gray-100 min-h-screen flex flex-col">
    <!-- Header -->
    <div th:replace="~{fragments/header :: header}"></div>

    <div class="flex flex-1">
        <!-- Sidebar -->
        <div th:replace="~{fragments/sidebar :: sidebar}"></div>

        <!-- Main Content -->
        <main class="flex-1 p-6">
            <div th:replace="${content}"></div>
        </main>
    </div>

    <!-- Footer -->
    <div th:replace="~{fragments/footer :: footer}"></div>
</body>
</html>
```

### Child Page Example
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <!-- Page specific content -->
</div>
</html>
```

## Test Plan

- [ ] Unit test: Layout template renders
- [ ] Visual test: Header, sidebar, footer visible
- [ ] Visual test: Content area displays correctly

## Parent Feature

Relates to F004-ui-layout
