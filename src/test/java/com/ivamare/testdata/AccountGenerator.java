package com.ivamare.testdata;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.datafaker.Faker;

/** Generates account test data with specified distributions. */
public class AccountGenerator {

  private static final int BATCH_SIZE = 500;

  // Account type distribution: 50% CHECKING, 30% SAVINGS, 20% CREDIT
  private static final String[] ACCOUNT_TYPES = {"CHECKING", "SAVINGS", "CREDIT"};
  private static final int[] ACCOUNT_TYPE_WEIGHTS = {50, 30, 20};

  // Currency distribution: 50% USD, 30% EUR, 15% GBP, 5% other
  private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "CAD", "AUD"};
  private static final int[] CURRENCY_WEIGHTS = {50, 30, 15, 3, 2};

  private final Faker faker;
  private int generatedCount = 0;

  public AccountGenerator(Faker faker) {
    this.faker = faker;
  }

  /**
   * Generates accounts for given customers.
   *
   * @param conn database connection
   * @param customerIds list of customer IDs to create accounts for
   * @param avgAccountsPerCustomer average number of accounts per customer
   * @return list of generated account IDs
   * @throws SQLException if database operations fail
   */
  public List<String> generate(
      Connection conn, List<String> customerIds, int avgAccountsPerCustomer) throws SQLException {
    List<String> accountIds = new ArrayList<>();

    String sql =
        "INSERT INTO accounts (id, customer_id, account_number, account_type, balance, currency, opened_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      int batchCount = 0;

      for (String customerId : customerIds) {
        // Vary accounts per customer (1-5 range, averaging to specified value)
        int accountCount = faker.number().numberBetween(1, avgAccountsPerCustomer * 2);

        for (int i = 0; i < accountCount; i++) {
          String id = UUID.randomUUID().toString();
          accountIds.add(id);

          stmt.setString(1, id);
          stmt.setString(2, customerId);
          stmt.setString(3, generateAccountNumber());
          stmt.setString(4, weightedRandom(ACCOUNT_TYPES, ACCOUNT_TYPE_WEIGHTS));
          stmt.setBigDecimal(5, generateBalance());
          stmt.setString(6, weightedRandom(CURRENCIES, CURRENCY_WEIGHTS));
          stmt.setTimestamp(
              7,
              Timestamp.valueOf(
                  LocalDateTime.now().minusDays(faker.number().numberBetween(0, 730))));

          stmt.addBatch();
          batchCount++;

          if (batchCount % BATCH_SIZE == 0) {
            stmt.executeBatch();
          }
        }
      }

      // Execute remaining batch
      stmt.executeBatch();
    }

    generatedCount = accountIds.size();
    return accountIds;
  }

  public int getGeneratedCount() {
    return generatedCount;
  }

  private String generateAccountNumber() {
    return String.format(
        "%04d-%04d-%04d",
        faker.number().numberBetween(1000, 9999),
        faker.number().numberBetween(1000, 9999),
        faker.number().numberBetween(1000, 9999));
  }

  private BigDecimal generateBalance() {
    // Generate balance between -5000 (credit) and 100000
    double balance = faker.number().randomDouble(2, -5000, 100000);
    return BigDecimal.valueOf(balance);
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
