# F001-S003: Set Up JaCoCo Coverage Enforcement

## User Story

**As a** developer
**I want** code coverage enforced at 80% for both line and branch coverage
**So that** the codebase maintains high test quality throughout development

## Acceptance Criteria

- [ ] Given pom.xml, then JaCoCo plugin is configured with prepare-agent, report, and check goals
- [ ] Given coverage check, when line coverage < 80%, then build fails
- [ ] Given coverage check, when branch coverage < 80%, then build fails
- [ ] Given `mvn verify`, then coverage report is generated at `target/site/jacoco/index.html`
- [ ] Given a Maven profile `coverage`, then coverage checks run only when profile is active

## Technical Notes

### Files to Modify
- `pom.xml` - Add JaCoCo plugin configuration

### JaCoCo Configuration
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Exclusions (if needed later)
- Main application class
- Configuration classes
- DTOs with no logic

## Test Plan

- [ ] Unit test: With adequate coverage, `mvn verify -Pcoverage` passes
- [ ] Unit test: With inadequate coverage, `mvn verify -Pcoverage` fails
- [ ] Manual: Coverage report is readable in browser

## Parent Feature

Relates to F001-dev-setup
