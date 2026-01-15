package com.ivamare.testdata;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.sql.DataSource;
import net.datafaker.Faker;

/**
 * Generates realistic test data for SQL Runner E2E testing.
 *
 * <p>Scale parameter = number of customers: - scale=1: 1 customer, ~10 accounts, ~5,000
 * transactions (for quick testing) - scale=100: 100 customers, ~1,000 accounts, ~500,000
 * transactions - scale=10000: 10,000 customers, ~100,000 accounts, ~50,000,000 transactions (full
 * run)
 *
 * <p>Distribution: - Customers: 70% ACTIVE, 20% INACTIVE, 10% PENDING - Accounts per customer: 1-20
 * (avg ~10) - Transactions per account: 100-1000 (avg ~500)
 */
public class TestDataGenerator {

  private static final int BATCH_SIZE = 500;

  private final DataSource dataSource;
  private final Faker faker;
  private final Random random;
  private final int customerCount;

  // Status distributions
  private static final String[] CUSTOMER_STATUSES = {
    "ACTIVE",
    "ACTIVE",
    "ACTIVE",
    "ACTIVE",
    "ACTIVE",
    "ACTIVE",
    "ACTIVE",
    "INACTIVE",
    "INACTIVE",
    "PENDING"
  };
  private static final String[] REGIONS = {
    "NA", "NA", "NA", "NA", "EU", "EU", "EU", "APAC", "APAC", "APAC"
  };
  private static final String[] ACCOUNT_TYPES = {
    "CHECKING",
    "CHECKING",
    "CHECKING",
    "CHECKING",
    "CHECKING",
    "SAVINGS",
    "SAVINGS",
    "SAVINGS",
    "CREDIT",
    "CREDIT"
  };
  private static final String[] TRANSACTION_TYPES = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"};
  private static final String[] TRANSACTION_STATUSES = {
    "COMPLETED",
    "COMPLETED",
    "COMPLETED",
    "COMPLETED",
    "COMPLETED",
    "COMPLETED",
    "PENDING",
    "PENDING",
    "PENDING",
    "FAILED"
  };
  private static final String[] CURRENCIES = {
    "USD", "USD", "USD", "USD", "USD", "EUR", "EUR", "EUR", "GBP", "GBP"
  };

  /** Create a generator with default customer count (10,000). */
  public TestDataGenerator(DataSource dataSource) {
    this(dataSource, 10000);
  }

  /**
   * Create a generator with specified customer count (scale).
   *
   * @param dataSource the database connection
   * @param customerCount number of customers to generate (scale parameter)
   */
  public TestDataGenerator(DataSource dataSource, int customerCount) {
    this.dataSource = dataSource;
    this.faker = new Faker();
    this.random = new Random(42); // Fixed seed for reproducibility
    this.customerCount = customerCount;
  }

  /** Generate all test data. */
  public void generateAll() throws SQLException {
    long start = System.currentTimeMillis();
    System.out.println("Starting test data generation (customers=" + customerCount + ")...");

    List<String> customerIds = generateCustomers();
    System.out.println("Generated " + customerIds.size() + " customers");

    List<String> accountIds = generateAccounts(customerIds);
    System.out.println("Generated " + accountIds.size() + " accounts");

    int transactionCount = generateTransactions(accountIds);
    System.out.println("Generated " + transactionCount + " transactions");

    long duration = System.currentTimeMillis() - start;
    System.out.println("Test data generation complete in " + duration + "ms");
  }

  private List<String> generateCustomers() throws SQLException {
    List<String> customerIds = new ArrayList<>();

    String sql =
        "INSERT INTO customers (id, name, email, status, region, created_at) VALUES (?, ?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      conn.setAutoCommit(false);

      for (int i = 0; i < customerCount; i++) {
        String id = UUID.randomUUID().toString();
        customerIds.add(id);

        ps.setString(1, id);
        ps.setString(2, faker.name().fullName());
        ps.setString(3, faker.internet().emailAddress());
        ps.setString(4, CUSTOMER_STATUSES[random.nextInt(CUSTOMER_STATUSES.length)]);
        ps.setString(5, REGIONS[random.nextInt(REGIONS.length)]);
        ps.setTimestamp(6, Timestamp.valueOf(randomPastDate(730)));

        ps.addBatch();

        if ((i + 1) % BATCH_SIZE == 0) {
          ps.executeBatch();
          conn.commit();
        }
      }

      ps.executeBatch();
      conn.commit();
    }

    return customerIds;
  }

  private List<String> generateAccounts(List<String> customerIds) throws SQLException {
    List<String> accountIds = new ArrayList<>();

    String sql =
        "INSERT INTO accounts (id, customer_id, account_number, account_type, balance, currency, opened_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      conn.setAutoCommit(false);
      int batchCount = 0;

      for (String customerId : customerIds) {
        // 1-20 accounts per customer (avg ~10)
        int accountCount = 1 + random.nextInt(20);

        for (int j = 0; j < accountCount; j++) {
          String id = UUID.randomUUID().toString();
          accountIds.add(id);

          ps.setString(1, id);
          ps.setString(2, customerId);
          ps.setString(3, faker.finance().iban());
          ps.setString(4, ACCOUNT_TYPES[random.nextInt(ACCOUNT_TYPES.length)]);
          ps.setBigDecimal(5, randomBalance());
          ps.setString(6, CURRENCIES[random.nextInt(CURRENCIES.length)]);
          ps.setTimestamp(7, Timestamp.valueOf(randomPastDate(730)));

          ps.addBatch();
          batchCount++;

          if (batchCount % BATCH_SIZE == 0) {
            ps.executeBatch();
            conn.commit();
          }
        }
      }

      ps.executeBatch();
      conn.commit();
    }

    return accountIds;
  }

  private int generateTransactions(List<String> accountIds) throws SQLException {
    String sql =
        "INSERT INTO transactions (id, account_id, transaction_type, amount, description, reference_id, executed_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    int totalCount = 0;

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      conn.setAutoCommit(false);
      int batchCount = 0;

      for (String accountId : accountIds) {
        // 100-1000 transactions per account (avg ~500)
        int transactionCount = 100 + random.nextInt(901);

        for (int j = 0; j < transactionCount; j++) {
          String txnType = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];

          ps.setString(1, UUID.randomUUID().toString());
          ps.setString(2, accountId);
          ps.setString(3, txnType);
          ps.setBigDecimal(4, randomTransactionAmount());
          ps.setString(5, generateDescription(txnType));
          ps.setString(6, "REF-" + faker.number().digits(10));
          ps.setTimestamp(7, Timestamp.valueOf(randomPastDate(730)));
          ps.setString(8, TRANSACTION_STATUSES[random.nextInt(TRANSACTION_STATUSES.length)]);

          ps.addBatch();
          batchCount++;
          totalCount++;

          if (batchCount % BATCH_SIZE == 0) {
            ps.executeBatch();
            conn.commit();
          }
        }
      }

      ps.executeBatch();
      conn.commit();
    }

    return totalCount;
  }

  private LocalDateTime randomPastDate(int maxDaysAgo) {
    int daysAgo = random.nextInt(maxDaysAgo);
    int hoursAgo = random.nextInt(24);
    return LocalDateTime.now().minus(daysAgo, ChronoUnit.DAYS).minus(hoursAgo, ChronoUnit.HOURS);
  }

  private BigDecimal randomBalance() {
    double balance = -5000 + random.nextDouble() * 105000;
    return BigDecimal.valueOf(balance).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  private BigDecimal randomTransactionAmount() {
    double amount = 1 + random.nextDouble() * 9999;
    return BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  private String generateDescription(String txnType) {
    return switch (txnType) {
      case "DEPOSIT" -> "Deposit: " + faker.commerce().productName();
      case "WITHDRAWAL" -> "Withdrawal: " + faker.commerce().department();
      case "TRANSFER" -> "Transfer to " + faker.name().lastName();
      default -> "Transaction";
    };
  }

  /** Clear all test data from tables. */
  public void clearAll() throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      conn.createStatement().execute("DELETE FROM transactions");
      conn.createStatement().execute("DELETE FROM accounts");
      conn.createStatement().execute("DELETE FROM customers");
      conn.commit();
      System.out.println("Cleared all test data");
    }
  }
}
