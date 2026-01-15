# F003: Authentication & Authorization

## Description

Implement LDAP-based authentication against Active Directory with role-based access control. Use embedded LDAP (UnboundID) for testing. Map AD groups to application roles (ADMIN, UPDATE_RUNNER, SELECT_RUNNER).

## Acceptance Criteria

- [ ] Form-based login page with username/password
- [ ] LDAP bind authentication against AD
- [ ] 20-minute session timeout
- [ ] AD group to role mapping configurable
- [ ] ADMIN role has full access
- [ ] UPDATE_RUNNER can execute SELECT and UPDATE workflows
- [ ] SELECT_RUNNER can only execute SELECT queries
- [ ] All authenticated users can view execution history
- [ ] Embedded LDAP for testing
- [ ] CSRF protection enabled

## User Stories

- [ ] F003-S001: Configure Spring Security with LDAP
- [ ] F003-S002: Create login page (styled)
- [ ] F003-S003: Implement AD group to role mapping
- [ ] F003-S004: Configure session management (20min timeout)
- [ ] F003-S005: Set up embedded LDAP for testing
- [ ] F003-S006: Create role-based access control filters
- [ ] F003-S007: Add logout functionality

## Technical Notes

- Use Spring Security LDAP module
- UnboundID for embedded LDAP in tests
- Role hierarchy: ADMIN > UPDATE_RUNNER > SELECT_RUNNER
- No "remember me" functionality

## Dependencies

- F002 (database schema for user-related queries)
