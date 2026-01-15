# F003-S006: Create Role-Based Access Control Filters

## User Story

**As a** security administrator
**I want** endpoints protected by role
**So that** users only access authorized functionality

## Acceptance Criteria

- [ ] Given ADMIN role, then full access to all endpoints
- [ ] Given UPDATE_RUNNER role, then access to SELECT and UPDATE execution
- [ ] Given SELECT_RUNNER role, then access to SELECT execution only
- [ ] Given any authenticated role, then access to execution history
- [ ] Given unauthorized access attempt, then 403 Forbidden returned
- [ ] Given Admin pages, then only ADMIN role can access

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/config/SecurityConfig.java`

### Endpoint Authorization Matrix
| Endpoint Pattern | ADMIN | UPDATE_RUNNER | SELECT_RUNNER |
|------------------|-------|---------------|---------------|
| /admin/** | Yes | No | No |
| /queries/*/execute (UPDATE) | Yes | Yes | No |
| /queries/*/execute (SELECT) | Yes | Yes | Yes |
| /history/** | Yes | Yes | Yes |
| /api/export | Yes | No | No |
| /api/import | Yes | No | No |

### Security Configuration
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/css/**", "/js/**").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/export", "/api/import").hasRole("ADMIN")
    .requestMatchers("/queries/*/execute/update/**").hasAnyRole("ADMIN", "UPDATE_RUNNER")
    .requestMatchers("/queries/**").authenticated()
    .requestMatchers("/history/**").authenticated()
    .anyRequest().authenticated()
);
```

### Method-Level Security (Optional)
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteQuery(String id) { ... }
```

## Test Plan

- [ ] Integration test: ADMIN can access all endpoints
- [ ] Integration test: UPDATE_RUNNER cannot access admin endpoints
- [ ] Integration test: SELECT_RUNNER cannot execute UPDATE queries
- [ ] Integration test: Unauthorized returns 403

## Parent Feature

Relates to F003-authentication
