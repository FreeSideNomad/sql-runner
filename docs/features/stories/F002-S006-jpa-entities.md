# F002-S006: Create JPA Entities for All Tables

## User Story

**As a** developer
**I want** JPA entities for all database tables
**So that** I can use Spring Data JPA for data access

## Acceptance Criteria

- [ ] Given Query entity, then all fields mapped correctly
- [ ] Given QueryVersion entity, then relationship to Query defined
- [ ] Given ExecutionLog entity, then all fields mapped
- [ ] Given BackupRecord entity, then relationship to ExecutionLog defined
- [ ] Given all entities, then Lombok annotations used
- [ ] Given all entities, then auditing fields populated automatically

## Technical Notes

### Files to Create
- `src/main/java/com/ivamare/domain/Query.java`
- `src/main/java/com/ivamare/domain/QueryVersion.java`
- `src/main/java/com/ivamare/domain/ExecutionLog.java`
- `src/main/java/com/ivamare/domain/BackupRecord.java`
- `src/main/java/com/ivamare/domain/QueryType.java` (enum)
- `src/main/java/com/ivamare/domain/ExecutionStatus.java` (enum)
- `src/main/java/com/ivamare/domain/ExecutionType.java` (enum)

### Entity Example
```java
@Entity
@Table(name = "queries", schema = "sqlrunner")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Query {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false)
    private QueryType queryType;

    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL)
    private List<QueryVersion> versions;

    // ... other fields
}
```

### Repositories to Create
- `src/main/java/com/ivamare/repository/QueryRepository.java`
- `src/main/java/com/ivamare/repository/QueryVersionRepository.java`
- `src/main/java/com/ivamare/repository/ExecutionLogRepository.java`
- `src/main/java/com/ivamare/repository/BackupRecordRepository.java`

## Test Plan

- [ ] Integration test: CRUD operations work for each entity
- [ ] Integration test: Relationships load correctly
- [ ] Integration test: Auditing fields populated

## Parent Feature

Relates to F002-database-schema
