package com.poe2craft.workbench.display;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * 표시·Numbers·동일성 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code golden()}: golden 값을 조회·구성한다. 반환: {@code JsonNode}. 사용하는 예외: {@code Exception}.
 *   <li>{@code goldenWasGeneratedFromTheUnchangedFrontendSource()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code matchesActualFrontendIntlNumbersAndDecimalTextAcrossSupportedLocales()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 반환: {@code List<DynamicTest>}. 사용하는 예외: {@code Exception}.
 *   <li>{@code rejectsNonFiniteValuesAndUnknownLocales()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
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
  List<DynamicTest> matchesActualFrontendIntlNumbersAndDecimalTextAcrossSupportedLocales()
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
