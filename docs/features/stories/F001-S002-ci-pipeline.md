# F001-S002: Configure GitHub Actions CI Pipeline

## User Story

**As a** developer
**I want** automated CI pipeline that runs on every push and PR
**So that** code quality is validated before merging to main

## Acceptance Criteria

- [ ] Given a push to main branch, then CI workflow triggers automatically
- [ ] Given a PR to main branch, then CI workflow triggers automatically
- [ ] Given the CI workflow, when build step runs, then Maven compiles the project
- [ ] Given the CI workflow, when test step runs, then all tests execute with coverage
- [ ] Given test failures, then the CI workflow fails and PR cannot be merged
- [ ] Given coverage below 80%, then the CI workflow fails
- [ ] Given successful CI, then coverage report is uploaded as artifact

## Technical Notes

### Files to Create
- `.github/workflows/ci.yml` - Main CI workflow

### Workflow Structure
```yaml
Jobs:
1. build - Compile project (fast feedback)
2. test - Run tests with coverage (depends on build)
```

### Required GitHub Settings
- Branch protection on main requires CI to pass
- Requires 1 reviewer approval (configured separately)

### Caching Strategy
- Cache Maven dependencies between runs
- Use `actions/setup-java@v4` with `cache: maven`

## Test Plan

- [ ] Integration test: Push to feature branch triggers workflow
- [ ] Integration test: PR to main triggers workflow
- [ ] Integration test: Failing test causes workflow to fail
- [ ] Integration test: Coverage report artifact is accessible

## Parent Feature

Relates to F001-dev-setup
