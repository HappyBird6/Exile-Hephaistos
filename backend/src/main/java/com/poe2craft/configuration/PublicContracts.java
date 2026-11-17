package com.poe2craft.configuration;

import com.fasterxml.jackson.databind.*;
import org.springframework.core.io.ClassPathResource;

/**
 * 공개 API 한도와 지원 언어 등 실행 계약을 번들 JSON에서 읽고 검증한다.
 *
 * <p>기존 계약: Public transport contracts, distinct from environment overrides and sealed game rules.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code LOCALE_CODES}: CODES ({@code java.util.Set<String>}).
 *   <li>{@code REPEAT}: 반복 ({@code Repeat}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PublicContracts()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code read(name)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code name}(이름). 반환: {@code JsonNode}. 사용하는
 *       예외: {@code ExceptionInInitializerError}.
 *   <li>{@code loadLocales()}: 언어코드목록을(를) 읽어 검증한다. 반환: {@code java.util.Set<String>}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code loadRepeat()}: 반복을(를) 읽어 검증한다. 반환: {@code Repeat}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code positive(data, key)}: 양수 조건을 확인한다. 입력: {@code data}(데이터), {@code key}(키). 반환: {@code
 *       int}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Repeat}: {@code PublicContracts}에서 사용하는 반복의 불변 값이다.
 * </ul>
 */
public final class PublicContracts {
  private PublicContracts() {}

  private static JsonNode read(String name) {
    try (var input = new ClassPathResource("contracts/" + name + ".json").getInputStream()) {
      return new ObjectMapper().readTree(input);
    } catch (java.io.IOException failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  public static final java.util.Set<String> LOCALE_CODES = loadLocales();

  private static java.util.Set<String> loadLocales() {
    var data = read("locales");
    var codes = new java.util.TreeSet<String>();
    data.path("locales").fieldNames().forEachRemaining(codes::add);
    if (codes.isEmpty() || !codes.contains(data.path("defaultLocale").asText()))
      throw new IllegalStateException("Invalid locale contract");
    return java.util.Set.copyOf(codes);
  }

  public static final Repeat REPEAT = loadRepeat();

  /**
   * {@code PublicContracts}에서 사용하는 반복의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code defaultUses}: 기본값·사용량 ({@code int}).
   *   <li>{@code minimumUses}: 최소값·사용량 ({@code int}).
   *   <li>{@code maximumUses}: 최대값·사용량 ({@code int}).
   *   <li>{@code maximumBodyBytes}: 압축 전후 요청 본문의 허용 바이트 수 ({@code int}).
   *   <li>{@code patternLength}: 패턴·길이 ({@code int}).
   *   <li>{@code patternDepth}: 패턴·깊이 ({@code int}).
   *   <li>{@code patternNodes}: 패턴·노드목록 ({@code int}).
   *   <li>{@code patternProgram}: 패턴·Program ({@code int}).
   * </ul>
   */
  public record Repeat(
      int defaultUses,
      int minimumUses,
      int maximumUses,
      int maximumBodyBytes,
      int patternLength,
      int patternDepth,
      int patternNodes,
      int patternProgram) {}

  private static Repeat loadRepeat() {
    var data = read("chaos-repeat");
    var value =
        new Repeat(
            positive(data, "defaultUses"),
            positive(data, "minimumUses"),
            positive(data, "maximumUses"),
            positive(data, "maximumBodyBytes"),
            positive(data, "patternLength"),
            positive(data, "patternDepth"),
            positive(data, "patternNodes"),
            positive(data, "patternProgram"));
    if (value.minimumUses() > value.defaultUses() || value.defaultUses() > value.maximumUses())
      throw new IllegalStateException("Invalid repeat limits");
    return value;
  }

  private static int positive(JsonNode data, String key) {
    var value = data.path(key);
    if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1)
      throw new IllegalStateException("Invalid public contract: " + key);
    return value.intValue();
  }
}
