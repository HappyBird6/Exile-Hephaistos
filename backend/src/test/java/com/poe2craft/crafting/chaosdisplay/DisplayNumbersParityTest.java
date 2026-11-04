package com.poe2craft.crafting.chaosdisplay;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.chaosdisplay.DisplayLocale;
import com.poe2craft.crafting.application.chaosdisplay.DisplayNumbers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class DisplayNumbersParityTest {
  private static JsonNode golden() throws Exception {
    return new ObjectMapper()
        .readTree(Path.of("src/test/resources/chaosdisplay/numbers-golden.json").toFile());
  }

  @Test
  void goldenWasGeneratedFromTheUnchangedFrontendSource() throws Exception {
    var golden = golden();
    var bytes = Files.readAllBytes(Path.of("..").resolve(golden.path("sourcePath").textValue()));
    assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
        .isEqualTo(golden.path("sourceSha256").textValue());
  }

  @TestFactory
  List<DynamicTest> matchesActualFrontendIntlNumbersAndDecimalTextAcrossSixLocales()
      throws Exception {
    List<DynamicTest> tests = new ArrayList<>();
    for (var row : golden().path("numberCases")) {
      tests.add(
          DynamicTest.dynamicTest(
              "number " + row,
              () ->
                  assertThat(
                          DisplayNumbers.number(
                              row.path("value").doubleValue(),
                              DisplayLocale.fromCode(row.path("locale").textValue())))
                      .isEqualTo(row.path("expected").textValue())));
    }
    for (var row : golden().path("textCases")) {
      tests.add(
          DynamicTest.dynamicTest(
              "text " + row,
              () ->
                  assertThat(
                          DisplayNumbers.text(
                              row.path("text").textValue(),
                              DisplayLocale.fromCode(row.path("source").textValue()),
                              DisplayLocale.fromCode(row.path("output").textValue())))
                      .isEqualTo(row.path("expected").textValue())));
    }
    return tests;
  }

  @Test
  void rejectsNonFiniteValuesAndUnknownLocales() {
    assertThatThrownBy(() -> DisplayNumbers.number(Double.NaN, DisplayLocale.EN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> DisplayNumbers.number(Double.POSITIVE_INFINITY, DisplayLocale.EN))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> DisplayLocale.fromCode("de"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
