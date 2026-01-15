# F001-S001: Initialize Spring Boot Project

## User Story

**As a** developer
**I want** a properly configured Spring Boot 3.5.9 project with Java 21
**So that** I have a solid foundation for building the SQL Runner application

## Acceptance Criteria

- [ ] Given the project is cloned, when I run `mvn clean compile`, then the build succeeds
- [ ] Given the project structure, then it follows standard Maven layout (src/main/java, src/test/java, etc.)
- [ ] Given pom.xml, then Spring Boot 3.5.9 parent is configured
- [ ] Given pom.xml, then Java 21 is configured as the source/target version
- [ ] Given pom.xml, then required dependencies are included (Spring Web, Thymeleaf, Spring Data JPA, etc.)
- [ ] Given the main class, when I run the application, then it starts without errors

## Technical Notes

### Project Generation
**MUST use Spring Boot Initializr (start.spring.io)**

```bash
# Option 1: Web UI
# Visit https://start.spring.io and configure:
# - Project: Maven
# - Language: Java
# - Spring Boot: 3.5.9
# - Group: com.sqlrunner
# - Artifact: sql-runner
# - Name: sql-runner
# - Package name: com.sqlrunner
# - Packaging: Jar
# - Java: 21

# Option 2: curl command
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.5.9 \
  -d groupId=com.sqlrunner \
  -d artifactId=sql-runner \
  -d name=sql-runner \
  -d packageName=com.sqlrunner \
  -d javaVersion=21 \
  -d dependencies=web,thymeleaf,data-jpa,security,validation,actuator,lombok \
  -o sql-runner.zip
```

### Files Created by Initializr (then customize)
- `pom.xml` - Maven configuration with Spring Boot parent
- `src/main/java/com/sqlrunner/SqlRunnerApplication.java` - Main application class
- `src/main/resources/application.properties` - Convert to application.yml
- `src/main/resources/application-local.yml` - Add local development profile

### Additional Dependencies (add to pom.xml after generation)
```xml
- spring-boot-starter-web
- spring-boot-starter-thymeleaf
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-actuator
- mssql-jdbc (SQL Server driver)
- postgresql (PostgreSQL driver)
- jcc (DB2 driver - may need IBM repository)
- lombok
- snakeyaml (for YAML config parsing)
```

### Package Structure
```
com.sqlrunner/
├── SqlRunnerApplication.java
├── config/
├── domain/
├── repository/
├── service/
├── controller/
└── dto/
```

## Test Plan

- [ ] Unit test: Application context loads successfully
- [ ] Manual: `mvn clean compile` succeeds
- [ ] Manual: `mvn spring-boot:run` starts application on port 8080

## Parent Feature

Relates to F001-dev-setup
