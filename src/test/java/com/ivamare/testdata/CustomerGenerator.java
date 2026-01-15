package com.ivamare.testdata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.datafaker.Faker;

/** Generates customer test data with specified distributions. */
public class CustomerGenerator {

  private static final int BATCH_SIZE = 500;

  // Status distribution: 70% ACTIVE, 20% INACTIVE, 10% PENDING
  private static final String[] STATUSES = {"ACTIVE", "INACTIVE", "PENDING"};
  private static final int[] STATUS_WEIGHTS = {70, 20, 10};

  // Region distribution: 40% NA, 35% EU, 25% APAC
  private static final String[] REGIONS = {"NA", "EU", "APAC"};
  private static final int[] REGION_WEIGHTS = {40, 35, 25};

  private final Faker faker;
  private int generatedCount = 0;

  public CustomerGenerator(Faker faker) {
    this.faker = faker;
  }

  /**
   * Generates customers and inserts them into the database.
   *
   * @param conn database connection
   * @param count number of customers to generate
   * @return list of generated customer IDs
   * @throws SQLException if database operations fail
   */
  public List<String> generate(Connection conn, int count) throws SQLException {
    List<String> customerIds = new ArrayList<>(count);

    String sql =
        "INSERT INTO customers (id, name, email, status, region, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < count; i++) {
        String id = UUID.randomUUID().toString();
        customerIds.add(id);

        stmt.setString(1, id);
        stmt.setString(2, faker.name().fullName());
        stmt.setString(3, faker.internet().emailAddress());
        stmt.setString(4, weightedRandom(STATUSES, STATUS_WEIGHTS));
        stmt.setString(5, weightedRandom(REGIONS, REGION_WEIGHTS));
        stmt.setTimestamp(
            6,
            Timestamp.valueOf(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 730))));

        stmt.addBatch();

        if ((i + 1) % BATCH_SIZE == 0) {
          stmt.executeBatch();
        }
      }

      // Execute remaining batch
      stmt.executeBatch();
    }

    generatedCount = count;
    return customerIds;
  }

  public int getGeneratedCount() {
    return generatedCount;
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
