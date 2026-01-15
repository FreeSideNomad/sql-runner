# F003-S004: Configure Session Management (20min Timeout)

## User Story

**As a** security administrator
**I want** sessions to timeout after 20 minutes of inactivity
**So that** unattended sessions don't pose a security risk

## Acceptance Criteria

- [ ] Given 20 minutes of inactivity, then session expires
- [ ] Given expired session, then redirect to login with message
- [ ] Given concurrent sessions, then user can have multiple sessions
- [ ] Given session configuration, then cookie settings secure
- [ ] Given HTTPS environment, then secure cookie flag set

## Technical Notes

### Files to Modify
- `src/main/resources/application.yml`
- `src/main/java/com/ivamare/config/SecurityConfig.java`

### Configuration
```yaml
server:
  servlet:
    session:
      timeout: 20m
      cookie:
        http-only: true
        secure: true  # for production
        same-site: strict
```

### Session Management in SecurityConfig
```java
http.sessionManagement(session -> session
    .invalidSessionUrl("/login?expired")
    .maximumSessions(-1)  // unlimited concurrent sessions
    .expiredUrl("/login?expired")
);
```

### Expired Session Message
```html
<div th:if="${param.expired}" class="text-red-600">
    Your session has expired. Please log in again.
</div>
```

## Test Plan

- [ ] Unit test: Session configuration applied
- [ ] Integration test: Session expires after timeout
- [ ] Integration test: Expired session redirects to login

## Parent Feature

Relates to F003-authentication
