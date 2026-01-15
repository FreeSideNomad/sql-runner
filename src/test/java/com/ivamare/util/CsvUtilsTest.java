package com.ivamare.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for CsvUtils - RFC 4180 compliant CSV generation. */
class CsvUtilsTest {

  @Nested
  class EscapeField {

    @Test
    void returnsEmptyString_forNull() {
      assertThat(CsvUtils.escapeField(null)).isEqualTo("");
    }

    @Test
    void returnsUnchanged_forSimpleString() {
      assertThat(CsvUtils.escapeField("hello")).isEqualTo("hello");
    }

    @Test
    void returnsUnchanged_forNumber() {
      assertThat(CsvUtils.escapeField(123)).isEqualTo("123");
      assertThat(CsvUtils.escapeField(45.67)).isEqualTo("45.67");
    }

    @Test
    void quotesString_withComma() {
      assertThat(CsvUtils.escapeField("hello, world")).isEqualTo("\"hello, world\"");
    }

    @Test
    void quotesString_withNewline() {
      assertThat(CsvUtils.escapeField("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    @Test
    void quotesString_withCarriageReturn() {
      assertThat(CsvUtils.escapeField("line1\rline2")).isEqualTo("\"line1\rline2\"");
    }

    @Test
    void escapesDoubleQuotes_byDoubling() {
      assertThat(CsvUtils.escapeField("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"");
    }

    @Test
    void handlesComplexString_withMultipleSpecialChars() {
      String input = "Name: \"John, Jr.\"\nAge: 30";
      String expected = "\"Name: \"\"John, Jr.\"\"\nAge: 30\"";
      assertThat(CsvUtils.escapeField(input)).isEqualTo(expected);
    }

    @Test
    void doesNotQuote_singleQuotes() {
      // Single quotes don't need special handling in CSV
      assertThat(CsvUtils.escapeField("it's fine")).isEqualTo("it's fine");
    }
  }

  @Nested
  class ToRow {

    @Test
    void createsSingleValue() {
      assertThat(CsvUtils.toRow("hello")).isEqualTo("hello");
    }

    @Test
    void joinsMultipleValues() {
      assertThat(CsvUtils.toRow("a", "b", "c")).isEqualTo("a,b,c");
    }

    @Test
    void escapesValues() {
      assertThat(CsvUtils.toRow("hello", "world, there", "test"))
          .isEqualTo("hello,\"world, there\",test");
    }

    @Test
    void handlesNullValues() {
      assertThat(CsvUtils.toRow("a", null, "c")).isEqualTo("a,,c");
    }

    @Test
    void handlesMixedTypes() {
      assertThat(CsvUtils.toRow("name", 123, 45.67, true)).isEqualTo("name,123,45.67,true");
    }

    @Test
    void fromList() {
      List<Object> values = List.of("a", "b", "c");
      assertThat(CsvUtils.toRow(values)).isEqualTo("a,b,c");
    }
  }

  @Nested
  class ToCsv {

    @Test
    void generatesHeaderAndData() {
      List<Map<String, Object>> data =
          List.of(Map.of("name", "John", "age", 30), Map.of("name", "Jane", "age", 25));
      List<String> columns = List.of("name", "age");
      List<String> headers = List.of("Name", "Age");

      String csv = CsvUtils.toCsv(data, columns, headers);

      String[] lines = csv.split(System.lineSeparator());
      assertThat(lines).hasSize(3);
      assertThat(lines[0]).isEqualTo("Name,Age");
      assertThat(lines[1]).isEqualTo("John,30");
      assertThat(lines[2]).isEqualTo("Jane,25");
    }

    @Test
    void escapesSpecialCharactersInData() {
      List<Map<String, Object>> data = List.of(Map.of("name", "John, Jr.", "note", "Says \"hi\""));
      List<String> columns = List.of("name", "note");
      List<String> headers = List.of("Name", "Note");

      String csv = CsvUtils.toCsv(data, columns, headers);

      String[] lines = csv.split(System.lineSeparator());
      assertThat(lines[1]).isEqualTo("\"John, Jr.\",\"Says \"\"hi\"\"\"");
    }

    @Test
    void handlesNullValues() {
      List<Map<String, Object>> data = List.of(Map.of("name", "John")); // missing "age" key
      List<String> columns = List.of("name", "age");
      List<String> headers = List.of("Name", "Age");

      String csv = CsvUtils.toCsv(data, columns, headers);

      String[] lines = csv.split(System.lineSeparator());
      assertThat(lines[1]).isEqualTo("John,");
    }
  }

  @Nested
  class WriteCsv {

    @Test
    void writesUtf8Bom() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      List<Map<String, Object>> data = List.of(Map.of("col", "val"));

      CsvUtils.writeCsv(out, data, List.of("col"), List.of("Col"));

      byte[] bytes = out.toByteArray();
      // Check UTF-8 BOM
      assertThat(bytes[0]).isEqualTo((byte) 0xEF);
      assertThat(bytes[1]).isEqualTo((byte) 0xBB);
      assertThat(bytes[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void writesDataCorrectly() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      List<Map<String, Object>> data =
          List.of(Map.of("name", "Alice", "score", 95), Map.of("name", "Bob", "score", 87));

      CsvUtils.writeCsv(out, data, List.of("name", "score"), List.of("Name", "Score"));

      String output = out.toString("UTF-8");
      // Skip BOM
      output = output.substring(1);
      String[] lines = output.trim().split("\n");
      assertThat(lines).hasSize(3);
      assertThat(lines[0].trim()).isEqualTo("Name,Score");
      assertThat(lines[1].trim()).isEqualTo("Alice,95");
      assertThat(lines[2].trim()).isEqualTo("Bob,87");
    }

    @Test
    void writesWithCustomRowWriter() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      CsvUtils.writeCsv(
          out,
          CsvUtils.toRow("Header1", "Header2"),
          writer -> {
            writer.println(CsvUtils.toRow("value1", "value2"));
            writer.println(CsvUtils.toRow("value3", "value4"));
          });

      String output = out.toString("UTF-8").substring(1); // Skip BOM
      String[] lines = output.trim().split("\n");
      assertThat(lines).hasSize(3);
      assertThat(lines[0].trim()).isEqualTo("Header1,Header2");
      assertThat(lines[1].trim()).isEqualTo("value1,value2");
      assertThat(lines[2].trim()).isEqualTo("value3,value4");
    }
  }

  @Nested
  class Rfc4180Compliance {

    @Test
    void handlesFieldWithOnlyQuotes() {
      assertThat(CsvUtils.escapeField("\"")).isEqualTo("\"\"\"\"");
    }

    @Test
    void handlesEmptyString() {
      assertThat(CsvUtils.escapeField("")).isEqualTo("");
    }

    @Test
    void handlesMultilineWithAllSpecialChars() {
      String input = "Line 1, has comma\n\"Line 2\" has quotes\rLine 3";
      String result = CsvUtils.escapeField(input);
      // Should be quoted and internal quotes doubled
      assertThat(result).startsWith("\"");
      assertThat(result).endsWith("\"");
      assertThat(result).contains("\"\"Line 2\"\"");
    }
  }
}
