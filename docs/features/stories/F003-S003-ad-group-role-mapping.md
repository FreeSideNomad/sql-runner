# F003-S003: Implement AD Group to Role Mapping

## User Story

**As an** administrator
**I want** AD groups mapped to application roles
**So that** access is controlled via existing AD infrastructure

## Acceptance Criteria

- [ ] Given configuration, then AD group to role mapping defined
- [ ] Given user in ADMIN group, then ROLE_ADMIN granted
- [ ] Given user in UPDATE_RUNNER group, then ROLE_UPDATE_RUNNER granted
- [ ] Given user in SELECT_RUNNER group, then ROLE_SELECT_RUNNER granted
- [ ] Given user in multiple groups, then highest privilege role applied
- [ ] Given role hierarchy, then ADMIN > UPDATE_RUNNER > SELECT_RUNNER

## Technical Notes

### Files to Create/Modify
- `src/main/java/com/ivamare/config/RoleMapper.java`
- `src/main/java/com/ivamare/security/CustomLdapAuthoritiesMapper.java`
- `src/main/resources/application.yml` (role mapping section)

### Configuration Example
```yaml
sqlrunner:
  security:
    role-mapping:
      ADMIN: CN=SQLRunner-Admins,OU=Groups,DC=company,DC=com
      UPDATE_RUNNER: CN=SQLRunner-UpdateRunners,OU=Groups,DC=company,DC=com
      SELECT_RUNNER: CN=SQLRunner-SelectRunners,OU=Groups,DC=company,DC=com
```

### Role Hierarchy Bean
```java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy("ROLE_ADMIN > ROLE_UPDATE_RUNNER > ROLE_SELECT_RUNNER");
    return hierarchy;
}
```

## Test Plan

- [ ] Unit test: Role mapping configuration loads
- [ ] Integration test: AD group correctly mapped to role
- [ ] Integration test: Role hierarchy enforced

## Parent Feature

Relates to F003-authentication
