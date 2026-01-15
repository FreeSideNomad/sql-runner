# SQL Runner - Development Rules

## Git Workflow

### Branching Strategy
- **main**: Protected branch, requires PR with CI passing + 1 reviewer
- **feature/F###-name**: Feature branches for development work

### Branch Naming Convention
```
feature/F001-dev-setup       # Feature implementation
feature/F002-authentication  # Feature implementation
bugfix/BUG-042-null-params   # Bug fixes
chore/update-dependencies    # Maintenance tasks
```

### Working on Features
1. Always create a feature branch from main:
   ```bash
   git checkout main && git pull
   git checkout -b feature/F###-feature-name
   ```
2. Never commit directly to main
3. Push feature branch and create PR when ready

### Commit Message Format (Conventional Commits)
```
<type>(<scope>): <description>

<optional body>

closes #X
closes #Y

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

**Types:**
- feat:     New feature
- fix:      Bug fix
- refactor: Code refactoring
- test:     Adding/updating tests
- docs:     Documentation only
- chore:    Build, CI, dependencies
- style:    Formatting (no code change)

**Closing Issues:**
- Each issue MUST be closed separately with `closes #X` on its own line
- Use `closes` (not `fixes` or `resolves`) for consistency
- Multiple issues = multiple `closes` lines

**Examples:**
```
feat(auth): implement LDAP bind authentication

- Added LDAP configuration
- Created login form

closes #2
closes #3

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

```
fix(query): handle null parameter values

closes #42

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

## Code Quality

### Before Committing
- Run `mvn spotless:apply` to format code
- Run `mvn test` to verify tests pass
- Ensure 80% line and branch coverage for new code

### Before Pushing
- Run `mvn verify -Pcoverage` to run full test suite with coverage check

### After Pushing
- ALWAYS check GitHub Actions for CI status after pushing
- Use `gh run list` to see recent workflow runs
- Use `gh run view <run-id>` to see details of a specific run
- If CI fails, fix the issues before creating a PR or requesting review
- Do not leave broken builds unattended

## Efficiency

### Parallel Execution
- Run independent tasks in parallel whenever possible
- Examples of parallelizable tasks:
  - Multiple file reads/writes that don't depend on each other
  - Multiple search/grep operations
  - Creating multiple GitHub issues
  - Running independent bash commands
- Use single message with multiple tool calls for parallel execution
- Only run sequentially when there are dependencies between tasks

## Project Structure

### Documentation
```
docs/
├── spec.md                    # Technical specification
├── dev-workflow.md            # Development workflow guide
└── features/                  # Feature documentation
    ├── F001-dev-setup.md      # Feature spec (mirrors GH issue)
    └── stories/               # User stories
        ├── F001-S001-*.md
        └── ...
```

### Source Code
```
src/main/java/com/ivamare/
├── SqlRunnerApplication.java
├── config/
├── domain/
├── repository/
├── service/
├── controller/
└── dto/
```

## GitHub Issues

### Issue References
- Feature: #1 (F001-dev-setup)
- Stories: #2-#9 (F001-S001 through F001-S008)

### Linking Commits to Issues
Always close issues in commit messages using `closes #X` format:
```
feat(project): initialize Spring Boot 3.5.9 project

closes #2

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

## Testing

### Test Databases
- SQL Server, PostgreSQL, DB2 via TestContainers
- Test data: ~10,000 customers, ~25,000 accounts, ~100,000 transactions

### Running Tests
```bash
mvn test                    # Unit tests only
mvn verify                  # All tests
mvn verify -Pcoverage       # Tests with coverage enforcement
```

## Spring Boot Project

### Creation Requirement
The Spring Boot project MUST be created using Spring Boot Initializr (start.spring.io), not manually.

### Key Dependencies
- Spring Boot 3.5.9
- Java 21
- Spring Web, Thymeleaf, Data JPA, Security, Validation, Actuator
- Lombok
- Database drivers: mssql-jdbc, postgresql, jcc (DB2)
