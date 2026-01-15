# F003-S005: Set Up Embedded LDAP for Testing

## User Story

**As a** developer
**I want** embedded LDAP for testing
**So that** authentication tests don't require external AD

## Acceptance Criteria

- [ ] Given test profile, then UnboundID embedded LDAP starts
- [ ] Given embedded LDAP, then test users and groups available
- [ ] Given test user in admin group, then ADMIN role assigned
- [ ] Given test user in update group, then UPDATE_RUNNER role assigned
- [ ] Given test user in select group, then SELECT_RUNNER role assigned

## Technical Notes

### Files to Create
- `src/test/java/com/ivamare/config/EmbeddedLdapConfig.java`
- `src/test/resources/ldap-test-data.ldif`
- `src/test/resources/application-test.yml`

### Maven Dependency
```xml
<dependency>
    <groupId>com.unboundid</groupId>
    <artifactId>unboundid-ldapsdk</artifactId>
    <scope>test</scope>
</dependency>
```

### Test LDIF Data
```ldif
dn: dc=test,dc=com
objectClass: domain
dc: test

dn: ou=Users,dc=test,dc=com
objectClass: organizationalUnit
ou: Users

dn: uid=admin,ou=Users,dc=test,dc=com
objectClass: inetOrgPerson
uid: admin
cn: Admin User
sn: User
userPassword: admin123

dn: ou=Groups,dc=test,dc=com
objectClass: organizationalUnit
ou: Groups

dn: cn=SQLRunner-Admins,ou=Groups,dc=test,dc=com
objectClass: groupOfNames
cn: SQLRunner-Admins
member: uid=admin,ou=Users,dc=test,dc=com
```

### Test Users
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| updater | updater123 | UPDATE_RUNNER |
| reader | reader123 | SELECT_RUNNER |

## Test Plan

- [ ] Integration test: Embedded LDAP starts on test profile
- [ ] Integration test: Test users authenticate successfully
- [ ] Integration test: Test groups map to correct roles

## Parent Feature

Relates to F003-authentication
