# F001-S008: Implement Test Data Generator

## User Story

**As a** developer
**I want** realistic test data generated for customers, accounts, and transactions
**So that** E2E tests have meaningful data to work with

## Acceptance Criteria

- [ ] Given the data generator, then 10,000 customers are created
- [ ] Given the data generator, then ~25,000 accounts are created (avg 2.5 per customer)
- [ ] Given the data generator, then ~100,000 transactions are created (avg 4 per account)
- [ ] Given customer data, then status is distributed (70% ACTIVE, 20% INACTIVE, 10% PENDING)
- [ ] Given customer data, then region is distributed (40% NA, 35% EU, 25% APAC)
- [ ] Given account data, then types are distributed (50% CHECKING, 30% SAVINGS, 20% CREDIT)
- [ ] Given transaction data, then dates span the last 2 years
- [ ] Given the generator, then it can run against any of the three databases

## Technical Notes

### Files to Create
- `src/test/java/com/sqlrunner/testdata/TestDataGenerator.java`
- `src/test/java/com/sqlrunner/testdata/CustomerGenerator.java`
- `src/test/java/com/sqlrunner/testdata/AccountGenerator.java`
- `src/test/java/com/sqlrunner/testdata/TransactionGenerator.java`

### Data Generation Strategy
```java
// Use Faker library for realistic data
// Batch inserts for performance (500-1000 per batch)
// Use PreparedStatement for efficiency
```

### Dependencies
```xml
<dependency>
    <groupId>net.datafaker</groupId>
    <artifactId>datafaker</artifactId>
    <version>2.1.0</version>
    <scope>test</scope>
</dependency>
```

### Data Distribution
| Table | Count | Distribution |
|-------|-------|--------------|
| customers | 10,000 | 70% ACTIVE, 20% INACTIVE, 10% PENDING |
| accounts | ~25,000 | 50% CHECKING, 30% SAVINGS, 20% CREDIT |
| transactions | ~100,000 | 60% COMPLETED, 30% PENDING, 10% FAILED |

### Regions
- NA (North America): 40%
- EU (Europe): 35%
- APAC (Asia Pacific): 25%

### Currencies
- USD: 50%
- EUR: 30%
- GBP: 15%
- Other: 5%

### Usage
```bash
# Generate data during test initialization
# Or run standalone:
mvn exec:java -Dexec.mainClass="com.sqlrunner.testdata.TestDataGenerator" \
  -Dexec.args="--db=sqlserver --url=jdbc:sqlserver://..."
```

## Test Plan

- [ ] Integration test: Generated data passes referential integrity checks
- [ ] Integration test: Data distributions match specifications
- [ ] Integration test: Generator completes in reasonable time (<60 seconds)

## Parent Feature

Relates to F001-dev-setup
