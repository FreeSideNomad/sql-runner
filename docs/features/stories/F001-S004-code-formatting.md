# F001-S004: Configure Spotless Code Formatting

## User Story

**As a** developer
**I want** consistent code formatting enforced via Google Java Style
**So that** the codebase maintains consistent style without manual effort

## Acceptance Criteria

- [ ] Given pom.xml, then Spotless plugin is configured with Google Java Style
- [ ] Given `mvn spotless:check`, when code violates style, then build fails with details
- [ ] Given `mvn spotless:apply`, then code is automatically formatted
- [ ] Given Spotless, then unused imports are automatically removed

## Technical Notes

### Files to Modify
- `pom.xml` - Add Spotless plugin configuration

### Spotless Configuration
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.19.2</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
        </java>
    </configuration>
</plugin>
```

### IDE Integration
- IntelliJ: Install Google Java Format plugin
- VS Code: Configure Java formatter to match

## Test Plan

- [ ] Unit test: Properly formatted code passes `mvn spotless:check`
- [ ] Unit test: Incorrectly formatted code fails `mvn spotless:check`
- [ ] Manual: `mvn spotless:apply` fixes formatting issues

## Parent Feature

Relates to F001-dev-setup
