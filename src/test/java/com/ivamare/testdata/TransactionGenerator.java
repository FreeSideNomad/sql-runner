package com.ivamare.testdata;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.datafaker.Faker;

/** Generates transaction test data with specified distributions. */
public class TransactionGenerator {

  private static final int BATCH_SIZE = 1000;

  // Transaction type distribution
  private static final String[] TRANSACTION_TYPES = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"};
  private static final int[] TRANSACTION_TYPE_WEIGHTS = {40, 35, 25};

  // Status distribution: 60% COMPLETED, 30% PENDING, 10% FAILED
  private static final String[] STATUSES = {"COMPLETED", "PENDING", "FAILED"};
  private static final int[] STATUS_WEIGHTS = {60, 30, 10};

  private final Faker faker;
  private int generatedCount = 0;

  public TransactionGenerator(Faker faker) {
    this.faker = faker;
  }

  /**
   * Generates transactions for given accounts.
   *
   * @param conn database connection
   * @param accountIds list of account IDs to create transactions for
   * @param avgTransactionsPerAccount average number of transactions per account
   * @throws SQLException if database operations fail
   */
  public void generate(Connection conn, List<String> accountIds, int avgTransactionsPerAccount)
      throws SQLException {

    String sql =
        "INSERT INTO transactions (id, account_id, transaction_type, amount, description, reference_id, executed_at, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      int batchCount = 0;
      int totalCount = 0;

      for (String accountId : accountIds) {
        // Vary transactions per account (1-8 range)
        int transactionCount = faker.number().numberBetween(1, avgTransactionsPerAccount * 2);

        for (int i = 0; i < transactionCount; i++) {
          String id = UUID.randomUUID().toString();
          String type = weightedRandom(TRANSACTION_TYPES, TRANSACTION_TYPE_WEIGHTS);

          stmt.setString(1, id);
          stmt.setString(2, accountId);
          stmt.setString(3, type);
          stmt.setBigDecimal(4, generateAmount(type));
          stmt.setString(5, generateDescription(type));
          stmt.setString(6, generateReferenceId());
          stmt.setTimestamp(7, generateExecutedAt());
          stmt.setString(8, weightedRandom(STATUSES, STATUS_WEIGHTS));

          stmt.addBatch();
          batchCount++;
          totalCount++;

          if (batchCount % BATCH_SIZE == 0) {
            stmt.executeBatch();
          }
        }
      }

      // Execute remaining batch
      stmt.executeBatch();
      generatedCount = totalCount;
    }
  }

  public int getGeneratedCount() {
    return generatedCount;
  }

  private BigDecimal generateAmount(String type) {
    double amount;
    if ("TRANSFER".equals(type)) {
      amount = faker.number().randomDouble(2, 100, 10000);
    } else {
      amount = faker.number().randomDouble(2, 10, 5000);
    }
    return BigDecimal.valueOf(amount);
  }

  private String generateDescription(String type) {
    return switch (type) {
      case "DEPOSIT" ->
          faker
              .options()
              .option(
                  "Direct deposit",
                  "Check deposit",
                  "Mobile deposit",
                  "Wire transfer in",
                  "Cash deposit");
      case "WITHDRAWAL" ->
          faker
              .options()
              .option("ATM withdrawal", "Cash withdrawal", "Check payment", "Wire transfer out");
      case "TRANSFER" ->
          faker
              .options()
              .option(
                  "Internal transfer", "External transfer", "Bill payment", "Scheduled transfer");
      default -> "Transaction";
    };
  }

  private String generateReferenceId() {
    return String.format(
        "REF-%s-%d",
        faker.letterify("???").toUpperCase(), faker.number().numberBetween(100000, 999999));
  }

  private Timestamp generateExecutedAt() {
    // Generate timestamps within last 2 years
    LocalDateTime dateTime = LocalDateTime.now().minusDays(faker.number().numberBetween(0, 730));
    return Timestamp.valueOf(dateTime);
  }

  private String weightedRandom(String[] values, int[] weights) {
    int totalWeight = 0;
    for (int w : weights) {
      totalWeight += w;
    }

    int random = faker.number().numberBetween(0, totalWeight);
    int cumulative = 0;

    for (int i = 0; i < values.length; i++) {
      cumulative += weights[i];
      if (random < cumulative) {
        return values[i];
      }
    }

    return values[values.length - 1];
  }
}
