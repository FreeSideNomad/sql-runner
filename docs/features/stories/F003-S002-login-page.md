# F003-S002: Create Login Page (Styled)

## User Story

**As a** user
**I want** a professional login page
**So that** I can securely access the application

## Acceptance Criteria

- [ ] Given unauthenticated access, then redirect to login page
- [ ] Given login page, then username and password fields displayed
- [ ] Given login page, then RBC-inspired styling applied
- [ ] Given invalid credentials, then error message displayed
- [ ] Given successful login, then redirect to dashboard
- [ ] Given login form, then CSRF token included

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/controller/AuthController.java`
- `src/main/resources/templates/login.html`

### UI Requirements
- Logo at top center
- Clean card-style form
- RBC Royal Blue (#003168) primary color
- Yellow (#FFD200) accent for buttons
- Error message area below form
- "Remember me" checkbox NOT included (per requirements)

### Template Example
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>SQL Runner - Login</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center">
    <div class="bg-white p-8 rounded-lg shadow-lg w-96">
        <h1 class="text-2xl font-bold text-center mb-6" style="color: #003168">SQL Runner</h1>
        <form th:action="@{/login}" method="post">
            <!-- Form fields -->
        </form>
    </div>
</body>
</html>
```

## Test Plan

- [ ] Unit test: Login page renders
- [ ] Integration test: Form submission works
- [ ] Integration test: Error messages display correctly

## Parent Feature

Relates to F003-authentication
