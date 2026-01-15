# F001-S005: Set Up Git Commit Hooks

## User Story

**As a** developer
**I want** pre-commit and pre-push hooks that validate code before commits
**So that** I catch issues early before pushing to the remote repository

## Acceptance Criteria

- [ ] Given `mvn compile`, then Git hooks are automatically installed
- [ ] Given a commit attempt, when code formatting fails, then commit is blocked
- [ ] Given a commit attempt, when unit tests fail, then commit is blocked
- [ ] Given a push attempt, when full test suite fails, then push is blocked
- [ ] Given a push attempt, when coverage < 80%, then push is blocked

## Technical Notes

### Files to Create
- `scripts/pre-commit.sh` - Pre-commit validation script
- `scripts/pre-push.sh` - Pre-push validation script

### Files to Modify
- `pom.xml` - Add git-build-hook plugin

### Pre-commit Script
```bash
#!/bin/bash
echo "Running pre-commit checks..."
mvn spotless:check || exit 1
mvn test -Dtest=*UnitTest || exit 1
echo "Pre-commit checks passed!"
```

### Pre-push Script
```bash
#!/bin/bash
echo "Running pre-push checks..."
mvn verify -Pcoverage || exit 1
echo "Pre-push checks passed!"
```

### Git Build Hook Plugin
```xml
<plugin>
    <groupId>com.rudikershaw.gitbuildhook</groupId>
    <artifactId>git-build-hook-maven-plugin</artifactId>
    <version>3.5.0</version>
    <configuration>
        <installHooks>
            <pre-commit>scripts/pre-commit.sh</pre-commit>
            <pre-push>scripts/pre-push.sh</pre-push>
        </installHooks>
    </configuration>
    <executions>
        <execution>
            <goals><goal>install</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Test Plan

- [ ] Manual: After `mvn compile`, `.git/hooks/pre-commit` exists
- [ ] Manual: After `mvn compile`, `.git/hooks/pre-push` exists
- [ ] Manual: Commit with bad formatting is blocked
- [ ] Manual: Push with failing tests is blocked

## Parent Feature

Relates to F001-dev-setup
