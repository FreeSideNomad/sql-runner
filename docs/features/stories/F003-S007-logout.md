# F003-S007: Add Logout Functionality

## User Story

**As a** user
**I want** to log out of the application
**So that** I can end my session securely

## Acceptance Criteria

- [ ] Given logout action, then session invalidated
- [ ] Given logout action, then redirect to login page
- [ ] Given logout action, then success message displayed
- [ ] Given logout button in header, then visible to all authenticated users
- [ ] Given CSRF protection, then logout uses POST method

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/config/SecurityConfig.java`
- `src/main/resources/templates/fragments/header.html`
- `src/main/resources/templates/login.html`

### Security Configuration
```java
http.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login?logout")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
);
```

### Logout Form in Header
```html
<form th:action="@{/logout}" method="post" class="inline">
    <button type="submit" class="text-gray-300 hover:text-white">
        Logout
    </button>
</form>
```

### Success Message on Login Page
```html
<div th:if="${param.logout}" class="text-green-600">
    You have been logged out successfully.
</div>
```

## Test Plan

- [ ] Integration test: Logout invalidates session
- [ ] Integration test: After logout, previous session token invalid
- [ ] Integration test: Redirect to login with message

## Parent Feature

Relates to F003-authentication
