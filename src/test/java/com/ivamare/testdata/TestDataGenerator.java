package com.ivamare.testdata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import net.datafaker.Faker;

/**
 * Main orchestrator for generating test data across all tables. Generates ~10,000 customers,
 * ~25,000 accounts, and ~100,000 transactions.
 */
public class TestDataGenerator {

  private static final int CUSTOMER_COUNT = 10_000;
  private static final int AVG_ACCOUNTS_PER_CUSTOMER = 2; // ~25,000 total
  private static final int AVG_TRANSACTIONS_PER_ACCOUNT = 4; // ~100,000 total

  private final Faker faker;
  private final CustomerGenerator customerGenerator;
  private final AccountGenerator accountGenerator;
  private final TransactionGenerator transactionGenerator;

  public TestDataGenerator() {
    this.faker = new Faker();
    this.customerGenerator = new CustomerGenerator(faker);
    this.accountGenerator = new AccountGenerator(faker);
    this.transactionGenerator = new TransactionGenerator(faker);
  }

  /**
   * Generates all test data and inserts into the database.
   *
   * @param dataSource the data source to insert data into
   * @throws SQLException if database operations fail
   */
  public void generateAll(DataSource dataSource) throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);

      try {
        // Generate customers
        List<String> customerIds = customerGenerator.generate(conn, CUSTOMER_COUNT);

        // Generate accounts for each customer
        List<String> accountIds =
            accountGenerator.generate(conn, customerIds, AVG_ACCOUNTS_PER_CUSTOMER);

        // Generate transactions for each account
        transactionGenerator.generate(conn, accountIds, AVG_TRANSACTIONS_PER_ACCOUNT);

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    }
  }

  /**
   * Gets the statistics of generated data.
   *
   * @return summary string of generated data counts
   */
  public String getStats() {
    return String.format(
        "Generated: %d customers, %d accounts, %d transactions",
        customerGenerator.getGeneratedCount(),
        accountGenerator.getGeneratedCount(),
        transactionGenerator.getGeneratedCount());
  }
}
