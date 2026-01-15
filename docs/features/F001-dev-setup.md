# F001: Development Environment Setup

## Description

Set up the complete development environment for SQL Runner, including project initialization, CI/CD pipeline, test database infrastructure, and code quality tooling. This foundational feature enables all subsequent development work.

## Acceptance Criteria

- [ ] Spring Boot 3.5.9 project builds successfully with Java 21
- [ ] GitHub Actions CI pipeline runs on push/PR to main
- [ ] Code coverage enforcement at 80% line and branch coverage
- [ ] Pre-commit and pre-push hooks validate code quality
- [ ] Test databases (SQL Server, DB2, PostgreSQL) available via TestContainers
- [ ] Sample test data (10,000+ rows) generated for E2E testing
- [ ] Google Java Style formatting enforced via Spotless

## User Stories

- [ ] F001-S001: Initialize Spring Boot project with Maven
- [ ] F001-S002: Configure GitHub Actions CI pipeline
- [ ] F001-S003: Set up JaCoCo coverage enforcement
- [ ] F001-S004: Configure Spotless code formatting
- [ ] F001-S005: Set up Git commit hooks
- [ ] F001-S006: Create TestContainers base configuration
- [ ] F001-S007: Create test database schemas
- [ ] F001-S008: Implement test data generator

## Dependencies

- GitHub account for repository hosting
- Docker for TestContainers (local development)

## Notes

- This feature must be completed before any functional development begins
- TestContainers approach chosen over docker-compose services for CI compatibility
- Coverage threshold may need adjustment as project matures (start enforced from day one)
- DB2 TestContainers may require privileged mode in some CI environments
