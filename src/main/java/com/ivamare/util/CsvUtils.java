package com.ivamare.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for CSV generation following RFC 4180 specification.
 *
 * <p>RFC 4180 rules:
 *
 * <ul>
 *   <li>Fields containing line breaks, double quotes, or commas must be enclosed in double quotes
 *   <li>Double quotes within fields must be escaped by preceding with another double quote
 *   <li>Each record is on a separate line, delimited by CRLF (we use system line separator)
 * </ul>
 *
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
 */
public final class CsvUtils {

  private static final char QUOTE = '"';
  private static final char COMMA = ',';
  private static final String ESCAPED_QUOTE = "\"\"";

  private CsvUtils() {
    // Utility class
  }

  /**
   * Escape a value according to RFC 4180 CSV specification.
   *
   * <p>Rules applied:
   *
   * <ul>
   *   <li>Null values become empty strings
   *   <li>Values containing comma, double quote, or newline are wrapped in double quotes
   *   <li>Double quotes within the value are escaped as two double quotes
   * </ul>
   *
   * @param value the value to escape
   * @return the escaped CSV field value
   */
  public static String escapeField(Object value) {
    if (value == null) {
      return "";
    }

    String str = value.toString();

    // Check if quoting is needed
    boolean needsQuoting =
        str.indexOf(COMMA) >= 0
            || str.indexOf(QUOTE) >= 0
            || str.indexOf('\n') >= 0
            || str.indexOf('\r') >= 0;

    if (needsQuoting) {
      // Escape double quotes by doubling them, then wrap in quotes
      return QUOTE + str.replace(String.valueOf(QUOTE), ESCAPED_QUOTE) + QUOTE;
    }

    return str;
  }

  /**
   * Create a CSV row from values.
   *
   * @param values the values for the row
   * @return comma-separated escaped values
   */
  public static String toRow(Object... values) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(COMMA);
      }
      sb.append(escapeField(values[i]));
    }
    return sb.toString();
  }

  /**
   * Create a CSV row from a list of values.
   *
   * @param values the values for the row
   * @return comma-separated escaped values
   */
  public static String toRow(List<?> values) {
    return values.stream().map(CsvUtils::escapeField).collect(Collectors.joining(","));
  }

  /**
   * Write UTF-8 BOM (Byte Order Mark) for Excel compatibility.
   *
   * @param outputStream the output stream to write to
   * @throws IOException if writing fails
   */
  public static void writeBom(OutputStream outputStream) throws IOException {
    outputStream.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
  }

  /**
   * Convert a list of maps to CSV format.
   *
   * @param data the list of row data as maps
   * @param columns ordered list of column names (keys in the maps)
   * @param headers display headers for the CSV (same order as columns)
   * @return complete CSV string including header row
   */
  public static String toCsv(
      List<Map<String, Object>> data, List<String> columns, List<String> headers) {
    StringBuilder sb = new StringBuilder();

    // Write header row
    sb.append(toRow(headers.toArray())).append(System.lineSeparator());

    // Write data rows
    for (Map<String, Object> row : data) {
      Object[] values = columns.stream().map(col -> row.get(col)).toArray();
      sb.append(toRow(values)).append(System.lineSeparator());
    }

    return sb.toString();
  }

  /**
   * Write CSV data to an output stream with UTF-8 BOM for Excel compatibility.
   *
   * @param outputStream the output stream to write to
   * @param data the list of row data as maps
   * @param columns ordered list of column names (keys in the maps)
   * @param headers display headers for the CSV (same order as columns)
   * @throws IOException if writing fails
   */
  public static void writeCsv(
      OutputStream outputStream,
      List<Map<String, Object>> data,
      List<String> columns,
      List<String> headers)
      throws IOException {
    writeBom(outputStream);
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        PrintWriter printWriter = new PrintWriter(writer)) {

      // Write header row
      printWriter.println(toRow(headers.toArray()));

      // Write data rows
      for (Map<String, Object> row : data) {
        Object[] values = columns.stream().map(col -> row.get(col)).toArray();
        printWriter.println(toRow(values));
      }
    }
  }

  /**
   * Write CSV data to an output stream with custom row writer.
   *
   * @param outputStream the output stream to write to
   * @param headerRow the header row
   * @param rowWriter consumer that writes each data row
   * @throws IOException if writing fails
   */
  public static void writeCsv(
      OutputStream outputStream,
      String headerRow,
      java.util.function.Consumer<PrintWriter> rowWriter)
      throws IOException {
    writeBom(outputStream);
    try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        PrintWriter printWriter = new PrintWriter(writer)) {
      printWriter.println(headerRow);
      rowWriter.accept(printWriter);
    }
  }
}
