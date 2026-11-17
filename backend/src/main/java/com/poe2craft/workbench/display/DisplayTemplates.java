package com.poe2craft.workbench.display;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.ModifierDefinition;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 호출자가 제공한 불변 옵션 문장과 언어별 수치 결합을 조회한다.
 *
 * <p>기존 계약: Immutable, caller-supplied modifierTemplates.json. No filesystem, classpath or network
 * loader.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code bindings}: 결합목록 ({@code Map<String, Binding>}).
 *   <li>{@code templates}: 템플릿목록 ({@code Map<DisplayLocale, Map<String, Translation>>}).
 *   <li>{@code sha256}: SHA·256 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code DisplayTemplates(bindings, templates, sha256)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       bindings}(결합목록), {@code templates}(템플릿목록), {@code sha256}(SHA·256).
 *   <li>{@code sha256()}: {@code sha256} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code parse(bytes, expectedSha256, mapper)}: 외부 표현을 내부 값으로 해석한다. 입력: {@code bytes}(바이트수),
 *       {@code expectedSha256}(기대값·SHA·256), {@code mapper}(변환기). 반환: {@code DisplayTemplates}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code binding(definition)}: 결합 처리에서 {@code bindings.get}, {@code definition.id}을(를) 사용한다.
 *       입력: {@code definition}(정의). 반환: {@code Binding}.
 *   <li>{@code englishBinding(definition)}: english·결합 처리에서 {@code bindings.get}, {@code
 *       definition.id}을(를) 사용한다. 입력: {@code definition}(정의). 반환: {@code Binding}.
 *   <li>{@code usesEnglishRevealFallback(definition, binding, locale)}: 사용량·English·공개·Fallback
 *       처리에서 {@code definition.id}, {@code templates.get}을(를) 사용한다. 입력: {@code definition}(정의),
 *       {@code binding}(결합), {@code locale}(언어코드). 반환: {@code boolean}.
 *   <li>{@code translation(definition, binding, locale)}: translation 처리에서 {@code templates.get},
 *       {@code binding.template}을(를) 사용한다. 입력: {@code definition}(정의), {@code binding}(결합), {@code
 *       locale}(언어코드). 반환: {@code Translation}.
 *   <li>{@code object(node)}: object 처리에서 {@code node.isObject}을(를) 사용한다. 입력: {@code node}(노드). 반환:
 *       {@code JsonNode}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code array(node)}: array 처리에서 {@code node.isArray}을(를) 사용한다. 입력: {@code node}(노드). 반환:
 *       {@code JsonNode}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code string(node)}: string 처리에서 {@code node.isTextual}, {@code node.textValue}을(를) 사용한다.
 *       입력: {@code node}(노드). 반환: {@code String}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code integer(node)}: 정수 처리에서 {@code node.isIntegralNumber}, {@code
 *       node.canConvertToLong}을(를) 사용한다. 입력: {@code node}(노드). 반환: {@code long}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code ValueStat}: {@code DisplayTemplates}에서 사용하는 값·수치의 불변 값이다.
 *   <li>{@code Binding}: {@code DisplayTemplates}에서 사용하는 결합의 불변 값이다.
 *   <li>{@code Translation}: {@code DisplayTemplates}에서 사용하는 Translation의 불변 값이다.
 * </ul>
 */
public final class DisplayTemplates {
  /**
   * {@code DisplayTemplates}에서 사용하는 값·수치의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code divisor}: 나누는수 ({@code double}).
   * </ul>
   */
  record ValueStat(String id, double divisor) {}

  /**
   * {@code DisplayTemplates}에서 사용하는 결합의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code englishText}: english·텍스트 ({@code String}).
   *   <li>{@code stats}: 수치목록 ({@code List<ModifierDefinition.StatRange>}).
   *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code List<String>}).
   *   <li>{@code template}: 템플릿 ({@code String}).
   *   <li>{@code valueStats}: 값·수치목록 ({@code List<ValueStat>}).
   * </ul>
   */
  record Binding(
      String englishText,
      List<ModifierDefinition.StatRange> stats,
      List<String> values,
      String template,
      List<ValueStat> valueStats) {}

  /**
   * {@code DisplayTemplates}에서 사용하는 Translation의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code template}: 템플릿 ({@code String}).
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   * </ul>
   */
  record Translation(String template, String name) {}

  private final Map<String, Binding> bindings;
  private final Map<DisplayLocale, Map<String, Translation>> templates;
  private final String sha256;

  private DisplayTemplates(
      Map<String, Binding> bindings,
      Map<DisplayLocale, Map<String, Translation>> templates,
      String sha256) {
    this.bindings = Map.copyOf(bindings);
    this.templates = Map.copyOf(templates);
    this.sha256 = sha256;
  }

  public String sha256() {
    return sha256;
  }

  /**
   * The integrator must supply bytes from the single original asset and its trusted manifest hash.
   */
  public static DisplayTemplates parse(byte[] bytes, String expectedSha256, ObjectMapper mapper) {
    try {
      String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!actual.equals(expectedSha256))
        throw new IllegalArgumentException("Display template hash mismatch");
      JsonNode root = mapper.readTree(bytes);
      var definitions = object(root.get("definitions"));
      Map<String, Binding> bindings = new LinkedHashMap<>();
      definitions
          .fields()
          .forEachRemaining(
              entry -> {
                JsonNode row = object(entry.getValue());
                List<ModifierDefinition.StatRange> stats = new ArrayList<>();
                for (var stat : array(row.get("stats"))) {
                  stats.add(
                      new ModifierDefinition.StatRange(
                          string(stat.get("id")),
                          integer(stat.get("min")),
                          integer(stat.get("max"))));
                }
                List<String> values = new ArrayList<>();
                for (var value : array(row.get("values"))) values.add(string(value));
                List<ValueStat> valueStats = null;
                if (row.has("valueStats")) {
                  valueStats = new ArrayList<>();
                  for (var stat : array(row.get("valueStats"))) {
                    var divisor = stat.get("divisor");
                    if (divisor == null
                        || !divisor.isNumber()
                        || !Double.isFinite(divisor.doubleValue())
                        || divisor.doubleValue() == 0)
                      throw new IllegalArgumentException("Invalid display divisor");
                    valueStats.add(new ValueStat(string(stat.get("id")), divisor.doubleValue()));
                  }
                  valueStats = List.copyOf(valueStats);
                }
                bindings.put(
                    entry.getKey(),
                    new Binding(
                        string(row.get("englishText")),
                        List.copyOf(stats),
                        List.copyOf(values),
                        string(row.get("template")),
                        valueStats));
              });
      Map<DisplayLocale, Map<String, Translation>> templates = new EnumMap<>(DisplayLocale.class);
      JsonNode locales = object(root.get("templates"));
      for (var locale : DisplayLocale.values()) {
        Map<String, Translation> translated = new LinkedHashMap<>();
        object(locales.get(locale.code()))
            .fields()
            .forEachRemaining(
                entry ->
                    translated.put(
                        entry.getKey(),
                        new Translation(
                            string(entry.getValue().get("template")),
                            string(entry.getValue().get("name")))));
        templates.put(locale, Map.copyOf(translated));
      }
      return new DisplayTemplates(bindings, templates, actual);
    } catch (IOException | NoSuchAlgorithmException invalid) {
      throw new IllegalArgumentException("Invalid display templates", invalid);
    }
  }

  Binding binding(ModifierDefinition definition) {
    Binding binding = bindings.get(definition.id());
    return binding != null
            && binding.englishText().equals(definition.text())
            && binding.stats().equals(definition.stats())
        ? binding
        : null;
  }

  Binding englishBinding(ModifierDefinition definition) {
    Binding binding = bindings.get(definition.id());
    return binding != null && binding.englishText().equals(definition.text()) ? binding : null;
  }

  boolean usesEnglishRevealFallback(
      ModifierDefinition definition, Binding binding, DisplayLocale locale) {
    return locale != DisplayLocale.EN
        && definition.id().startsWith("reveal:")
        && !templates.get(locale).containsKey(binding.template())
        && templates.get(DisplayLocale.EN).containsKey(binding.template());
  }

  Translation translation(ModifierDefinition definition, Binding binding, DisplayLocale locale) {
    var translation = templates.get(locale).get(binding.template());
    return translation == null && definition.id().startsWith("reveal:")
        ? templates.get(DisplayLocale.EN).get(binding.template())
        : translation;
  }

  private static JsonNode object(JsonNode node) {
    if (node == null || !node.isObject())
      throw new IllegalArgumentException("Display object required");
    return node;
  }

  private static JsonNode array(JsonNode node) {
    if (node == null || !node.isArray())
      throw new IllegalArgumentException("Display array required");
    return node;
  }

  private static String string(JsonNode node) {
    if (node == null || !node.isTextual())
      throw new IllegalArgumentException("Display string required");
    return node.textValue();
  }

  private static long integer(JsonNode node) {
    if (node == null || !node.isIntegralNumber() || !node.canConvertToLong())
      throw new IllegalArgumentException("Display integer required");
    return node.longValue();
  }
}
