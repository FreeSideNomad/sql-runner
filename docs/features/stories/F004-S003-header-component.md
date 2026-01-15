# F004-S003: Build Header Component with User Info

## User Story

**As a** user
**I want** a header with application branding and my user info
**So that** I know where I am and who I'm logged in as

## Acceptance Criteria

- [ ] Given header, then SQL Runner logo/text displayed
- [ ] Given header, then current user's name displayed
- [ ] Given header, then user's role badge displayed
- [ ] Given header, then logout dropdown available
- [ ] Given header, then fixed to top of viewport
- [ ] Given header, then RBC Royal Blue background

## Technical Notes

### Files to Create
- `src/main/resources/templates/fragments/header.html`

### Header Fragment
```html
<header th:fragment="header" class="bg-rbc-blue text-white shadow-lg fixed top-0 left-0 right-0 z-50">
    <div class="flex items-center justify-between px-6 h-16">
        <!-- Logo -->
        <div class="flex items-center space-x-3">
            <span class="text-xl font-bold">SQL Runner</span>
        </div>

        <!-- User Info -->
        <div class="flex items-center space-x-4">
            <span th:text="${#authentication.name}" sec:authorize="isAuthenticated()">Username</span>
            <span class="px-2 py-1 text-xs rounded bg-rbc-yellow text-rbc-blue font-semibold"
                  th:text="${userRole}">ADMIN</span>

            <!-- Logout -->
            <form th:action="@{/logout}" method="post">
                <button type="submit" class="hover:text-rbc-yellow transition">
                    Logout
                </button>
            </form>
        </div>
    </div>
</header>
```

### Controller Model Attribute
```java
@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute("userRole")
    public String getUserRole(Authentication auth) {
        // Return highest role
    }
}
```

## Test Plan

- [ ] Unit test: Header fragment renders
- [ ] Integration test: User name displayed correctly
- [ ] Integration test: Role badge shows correct role

## Parent Feature

Relates to F004-ui-layout
