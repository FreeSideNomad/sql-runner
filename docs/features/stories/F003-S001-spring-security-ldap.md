# F003-S001: Configure Spring Security with LDAP

## User Story

**As a** developer
**I want** Spring Security configured with LDAP authentication
**So that** users can authenticate against Active Directory

## Acceptance Criteria

- [ ] Given Spring Security LDAP dependency added, then LDAP authentication available
- [ ] Given application.yml with LDAP settings, then connection to AD configurable
- [ ] Given valid credentials, then LDAP bind authentication succeeds
- [ ] Given invalid credentials, then authentication fails with appropriate error
- [ ] Given LDAP connection failure, then graceful error handling

## Technical Notes

### Files to Create/Modify
- `src/main/java/com/ivamare/config/SecurityConfig.java`
- `src/main/resources/application.yml` (LDAP section)

### Configuration Example
```yaml
spring:
  ldap:
    urls: ldap://ad.company.com:389
    base: DC=company,DC=com
    username: CN=svc_sqlrunner,OU=Service Accounts,DC=company,DC=com
    password: ${LDAP_PASSWORD}
  security:
    ldap:
      user-search-base: OU=Users
      user-search-filter: (sAMAccountName={0})
      group-search-base: OU=Groups
      group-search-filter: (member={0})
```

### Security Config Skeleton
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        // Configure LDAP authentication
        // Enable CSRF protection
        // Configure form login
    }
}
```

## Test Plan

- [ ] Unit test: SecurityConfig loads correctly
- [ ] Integration test: Embedded LDAP authentication works
- [ ] Integration test: Invalid credentials rejected

## Parent Feature

Relates to F003-authentication
