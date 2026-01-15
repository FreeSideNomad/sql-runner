# SQL Runner - Development Workflow

## 1. Overview

This document defines the development workflow for SQL Runner, including project management practices, CI/CD pipeline, testing infrastructure, and code quality standards.

### Key Principles
- **Feature-driven development**: Work is organized into features (epics) containing user stories
- **Test-first approach**: TDD recommended, 80% coverage enforced
- **Continuous integration**: All changes validated via GitHub Actions
- **Environment promotion**: Test → Prod via configuration export/import

---

## 2. Project Structure

### Documentation Hierarchy
```
docs/
├── spec.md                              # Technical specification
├── dev-workflow.md                      # This document
├── features/                            # Feature documentation
│   ├── F001-dev-setup.md                # Feature spec (mirrors GH issue)
│   ├── F002-authentication.md
│   ├── F003-query-management.md
│   └── stories/                         # User story documentation
│       ├── F001-S001-project-init.md
│       ├── F001-S002-ci-pipeline.md
│       └── ...
```

### Feature & Story Sizing
| Type | Scope | Duration |
|------|-------|----------|
| Feature | Epic-level, multiple stories | Days to weeks |
| Story | Single implementable unit | Hours to 1-2 days |

---

## 3. GitHub Issue Templates

### Feature Template
Location: `.github/ISSUE_TEMPLATE/feature.md`

```yaml
name: Feature
about: Propose a new feature (epic-level, contains multiple stories)
labels: ["feature"]
---
## Description
<!-- Clear description of the feature -->

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## User Stories
<!-- List of stories that make up this feature -->
- [ ] #XX - Story title
- [ ] #XX - Story title

## Dependencies
<!-- Other features or external dependencies -->

## Notes
<!-- Additional context, design decisions, risks -->
```

### User Story Template
Location: `.github/ISSUE_TEMPLATE/user-story.md`

```yaml
name: User Story
about: A single implementable unit of work (hours to 1-2 days)
labels: ["story"]
---
## User Story
**As a** [role]
**I want** [capability]
**So that** [benefit]

## Acceptance Criteria
- [ ] Given/When/Then scenario 1
- [ ] Given/When/Then scenario 2

## Technical Notes
<!-- Implementation approach, affected files, considerations -->

## Test Plan
- [ ] Unit tests for...
- [ ] Integration tests for...

## Parent Feature
<!-- Link to parent feature issue -->
Relates to #XX
```

### Bug Template
Location: `.github/ISSUE_TEMPLATE/bug.md`

```yaml
name: Bug Report
about: Report a defect
labels: ["bug"]
---
## Description
<!-- What's broken? -->

## Steps to Reproduce
1. Step 1
2. Step 2

## Expected Behavior
<!-- What should happen -->

## Actual Behavior
<!-- What actually happens -->

## Environment
- Browser:
- Database:
- Version:

## Screenshots/Logs
<!-- If applicable -->
```

### Chore Template
Location: `.github/ISSUE_TEMPLATE/chore.md`

```yaml
name: Chore
about: Technical tasks, refactoring, maintenance
labels: ["chore"]
---
## Description
<!-- What needs to be done -->

## Motivation
<!-- Why is this needed -->

## Tasks
- [ ] Task 1
- [ ] Task 2

## Impact
<!-- What parts of the codebase are affected -->
```

---

## 4. GitHub Repository Setup

### Create Repository
```bash
# Create repository
gh repo create sql-runner --public --description "Parameterized SQL execution with audit logging"

# Clone and initialize
git clone https://github.com/<username>/sql-runner.git
cd sql-runner
```

### Branch Protection Rules
```bash
# Set up branch protection via gh CLI
gh api repos/<username>/sql-runner/branches/main/protection -X PUT \
  -F required_status_checks='{"strict":true,"contexts":["build","test"]}' \
  -F enforce_admins=false \
  -F required_pull_request_reviews='{"required_approving_review_count":1}' \
  -F restrictions=null
```

### Required Settings
- **Default branch**: `main`
- **Branch protection on main**:
  - Require status checks to pass (build, test)
  - Require 1 reviewer approval
  - Require branches to be up to date

---

## 5. GitHub Actions CI/CD

### CI Workflow
Location: `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build
        run: mvn clean compile -DskipTests

  test:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Run tests with coverage
        run: mvn verify -Pcoverage
      - name: Check coverage thresholds
        run: mvn jacoco:check
      - name: Upload coverage report
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: target/site/jacoco/
```

---

## 6. Code Quality & Hooks

### Maven Plugins

#### Spotless (Google Java Style)
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

#### Git Build Hook
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

#### JaCoCo Coverage (80% minimum)
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

### Git Hooks

#### Pre-commit (`scripts/pre-commit.sh`)
```bash
#!/bin/bash
echo "Running pre-commit checks..."
mvn spotless:check || exit 1
mvn test -Dtest=*UnitTest || exit 1
echo "Pre-commit checks passed!"
```

#### Pre-push (`scripts/pre-push.sh`)
```bash
#!/bin/bash
echo "Running pre-push checks..."
mvn verify -Pcoverage || exit 1
echo "Pre-push checks passed!"
```

---

## 7. Development Workflow

### Workflow Diagram
```
┌─────────────────────────────────────────────────────────────────┐
│                     DEVELOPMENT WORKFLOW                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. START FEATURE                                               │
│     git checkout main && git pull                               │
│     git checkout -b feature/F001-dev-setup                      │
│                                                                 │
│  2. IMPLEMENT STORY                                             │
│     - Write failing tests first (TDD recommended)               │
│     - Implement code to pass tests                              │
│     - Ensure 80% coverage for new code                          │
│     - Commit using Conventional Commits format                  │
│                                                                 │
│  3. REPEAT FOR EACH STORY                                       │
│     - Keep commits atomic (one story = one or few commits)      │
│     - Run full test suite before each commit                    │
│                                                                 │
│  4. CREATE PR                                                   │
│     gh pr create --title "Feature: Dev Setup" \                 │
│       --body "Implements #1 (F001-dev-setup)"                   │
│                                                                 │
│  5. REVIEW & MERGE                                              │
│     - CI must pass (build + tests + coverage)                   │
│     - 1 reviewer approval required                              │
│     - Squash and merge to main                                  │
│                                                                 │
│  6. NEXT FEATURE                                                │
│     git checkout main && git pull                               │
│     git checkout -b feature/F002-authentication                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Conventional Commits Format
```
<type>(<scope>): <description> [<issue-ref>]

Types:
- feat:     New feature
- fix:      Bug fix
- refactor: Code refactoring (no feature change)
- test:     Adding/updating tests
- docs:     Documentation only
- chore:    Build, CI, dependencies
- style:    Formatting (no code change)

Examples:
- feat(auth): implement LDAP bind authentication [F001-S001]
- fix(query): handle null parameter values [BUG-042]
- test(execution): add coverage for CSV export [F003-S002]
- chore(deps): upgrade Spring Boot to 3.5.9
```

---

## 8. Test Database Infrastructure

### Docker Compose for Testing
Location: `docker-compose.test.yml`

```yaml
version: '3.8'
services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_SA_PASSWORD: "TestPass123!"
    ports:
      - "1433:1433"
    volumes:
      - ./test-data/sqlserver-init.sql:/docker-entrypoint-initdb.d/init.sql

  db2:
    image: icr.io/db2_community/db2:latest
    privileged: true
    environment:
      LICENSE: accept
      DB2INST1_PASSWORD: "TestPass123!"
      DBNAME: testdb
    ports:
      - "50000:50000"
    volumes:
      - ./test-data/db2-init.sql:/var/custom/init.sql

  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: "TestPass123!"
      POSTGRES_DB: testdb
    ports:
      - "5432:5432"
    volumes:
      - ./test-data/postgres-init.sql:/docker-entrypoint-initdb.d/init.sql
```

### Sample Schema (Identical Across All DBs)

```sql
CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    status VARCHAR(20) NOT NULL,        -- ACTIVE, INACTIVE, PENDING
    region VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL REFERENCES customers(id),
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(20) NOT NULL,  -- CHECKING, SAVINGS, CREDIT
    balance DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP
);

CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL REFERENCES accounts(id),
    transaction_type VARCHAR(20) NOT NULL,  -- DEPOSIT, WITHDRAWAL, TRANSFER
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    reference_id VARCHAR(100),
    executed_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL         -- COMPLETED, PENDING, FAILED
);

-- Indexes
CREATE INDEX idx_customers_region ON customers(region);
CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_executed ON transactions(executed_at);
```

### Test Data Volume
- **10,000 customers** (distributed across regions: NA, EU, APAC)
- **25,000 accounts** (average 2.5 per customer)
- **100,000 transactions** (average 4 per account)

---

## 9. TestContainers Integration

### Abstract Integration Test Base
Location: `src/test/java/com/sqlrunner/AbstractIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(
            "mcr.microsoft.com/mssql/server:2022-latest")
        .acceptLicense()
        .withInitScript("test-data/sqlserver-init.sql");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withInitScript("test-data/postgres-init.sql");

    @Container
    static Db2Container db2 = new Db2Container("icr.io/db2_community/db2:latest")
        .acceptLicense()
        .withInitScript("test-data/db2-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure connection properties dynamically based on containers
    }
}
```

---

## 10. Coverage Requirements

| Metric | Minimum |
|--------|---------|
| Line Coverage | 80% |
| Branch Coverage | 80% |

### Exclusions (if needed)
- Configuration classes (`*Config.java`)
- Main application class
- DTOs (data classes with no logic)

---

## 11. Quick Reference

### Common Commands
```bash
# Build without tests
mvn clean compile

# Run unit tests only
mvn test -Dtest=*UnitTest

# Run all tests with coverage
mvn verify -Pcoverage

# Check code formatting
mvn spotless:check

# Apply code formatting
mvn spotless:apply

# Generate coverage report
mvn jacoco:report
# View at: target/site/jacoco/index.html
```

### Branch Naming Convention
```
feature/F001-dev-setup
feature/F002-authentication
bugfix/BUG-042-null-params
chore/update-dependencies
```

### PR Title Convention
```
Feature: Dev Setup [F001]
Feature: Authentication [F002]
Fix: Handle null parameters [BUG-042]
Chore: Update Spring Boot to 3.5.9
```
