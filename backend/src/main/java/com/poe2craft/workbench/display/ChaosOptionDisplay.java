package com.poe2craft.workbench.display;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.QualityRoundingPolicy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 화폐를 사용하거나 수치를 바꾸지 않고 카오스 결과를 일반 표시로 투영한다.
 *
 * <p>기존 계약: Pure normal-view projection. Does not select matching units, apply currency, or alter
 * rolls.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "chaos-option-display-v1"} ({@code String}).
 *   <li>{@code MAX_SAFE_INTEGER}: 정수; 초기값 {@code 9007199254740991L} ({@code long}).
 *   <li>{@code RANGE}: 범위 ({@code Pattern}).
 *   <li>{@code PLACEHOLDER}: PLACEHOLDER ({@code Pattern}).
 *   <li>{@code BASIC_JEWEL}: 주얼 ({@code Pattern}).
 *   <li>{@code INTEGER_STATS}: 수치목록 ({@code Set<String>}).
 *   <li>{@code templates}: 템플릿목록 ({@code DisplayTemplates}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosOptionDisplay(templates)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code templates}(템플릿목록).
 *   <li>{@code format(state, catalog, locale)}: 지정 언어와 표시 규칙에 맞는 문자열을 만든다. 입력: {@code state}(상태),
 *       {@code catalog}(카탈로그), {@code locale}(언어코드). 반환: {@code Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code result(locale, groups, issues)}: 최종 또는 현재 계산 결과를 구성한다. 입력: {@code locale}(언어코드),
 *       {@code groups}(그룹목록), {@code issues}(문제목록). 반환: {@code Result}.
 *   <li>{@code project(state, definition, modifier, issues)}: 투영 처리에서 {@code
 *       ModifierMagnitudes.factor}, {@code values.replaceAll}을(를) 사용한다. 입력: {@code state}(상태),
 *       {@code definition}(정의), {@code modifier}(옵션), {@code issues}(문제목록). 반환: {@code Map<String,
 *       Double>}.
 *   <li>{@code baseProject(state, definition, modifier, issues)}: 베이스·투영 처리에서 {@code
 *       modifier.values}, {@code source.forEach}을(를) 사용한다. 입력: {@code state}(상태), {@code
 *       definition}(정의), {@code modifier}(옵션), {@code issues}(문제목록). 반환: {@code Map<String,
 *       Double>}.
 *   <li>{@code localized(definition, values, locale, issues)}: localized 처리에서 {@code
 *       templates.binding}, {@code templates.translation}을(를) 사용한다. 입력: {@code definition}(정의),
 *       {@code values}(값목록), {@code locale}(언어코드), {@code issues}(문제목록). 반환: {@code String}.
 *   <li>{@code rolled(definition, values)}: rolled 처리에서 {@code templates.englishBinding}, {@code
 *       BASIC_JEWEL.matcher}을(를) 사용한다. 입력: {@code definition}(정의), {@code values}(값목록). 반환: {@code
 *       Rendered}.
 *   <li>{@code rangeMatches(match, stat)}: 범위·일치결과 처리에서 {@code Double.parseDouble}, {@code
 *       match.group}을(를) 사용한다. 입력: {@code match}(일치), {@code stat}(수치). 반환: {@code boolean}.
 *   <li>{@code substitute(template, numbers)}: substitute 처리에서 {@code PLACEHOLDER.matcher}, {@code
 *       matcher.find}을(를) 사용한다. 입력: {@code template}(템플릿), {@code numbers}(numbers). 반환: {@code
 *       String}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Code}: {@code ChaosOptionDisplay}의 코드 구분값을 정의한다.
 *   <li>{@code Issue}: {@code ChaosOptionDisplay}에서 사용하는 문제의 불변 값이다.
 *   <li>{@code Group}: {@code ChaosOptionDisplay}에서 사용하는 그룹의 불변 값이다.
 *   <li>{@code Result}: {@code ChaosOptionDisplay}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Rendered}: {@code ChaosOptionDisplay}에서 사용하는 Rendered의 불변 값이다.
 * </ul>
 */
public final class ChaosOptionDisplay {
  public static final String VERSION = "chaos-option-display-v1";
  private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
  private static final Pattern RANGE =
      Pattern.compile("\\((-?[0-9]+(?:\\.[0-9]+)?)\\s*[—–?-]\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\)");
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{v([0-9]+)\\}");
  private static final Pattern BASIC_JEWEL = Pattern.compile("^(ruby|emerald|sapphire|diamond):");
  private static final Set<String> INTEGER_STATS =
      Set.of(
          "base_maximum_life",
          "base_maximum_mana",
          "base_maximum_energy_shield",
          "base_evasion_rating",
          "additional_strength",
          "additional_dexterity",
          "additional_intelligence",
          "additional_all_attributes",
          "base_fire_damage_resistance_%",
          "base_cold_damage_resistance_%",
          "base_lightning_damage_resistance_%",
          "base_chaos_damage_resistance_%",
          "base_resist_all_elements_%",
          "spell_damage_+%",
          "fire_damage_+%",
          "cold_damage_+%",
          "lightning_damage_+%",
          "chaos_damage_+%",
          "physical_damage_reduction_rating_+%",
          "maximum_energy_shield_+%",
          "evasion_rating_+%",
          "base_cast_speed_+%");

  private final DisplayTemplates templates;

  public ChaosOptionDisplay(DisplayTemplates templates) {
    this.templates = Objects.requireNonNull(templates);
  }

  /**
   * {@code ChaosOptionDisplay}의 코드 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code ENGLISH_FALLBACK}: FALLBACK 구분값.
   *   <li>{@code ENGLISH_REVEAL_TEMPLATE_FALLBACK}: FALLBACK 구분값.
   *   <li>{@code SOURCE_UNITS_FALLBACK}: FALLBACK 구분값.
   *   <li>{@code UNREVIEWED_QUALITY}: 품질 구분값.
   *   <li>{@code UNSAFE_DISPLAY_INTEGER}: 정수 구분값.
   *   <li>{@code UNRESOLVED_TEMPLATE_VALUE}: 값 구분값.
   *   <li>{@code HIDDEN_OPTIONS_OMITTED}: OMITTED 구분값.
   *   <li>{@code JEWEL_EFFECT_AND_QUALITY_PROVISIONAL}: PROVISIONAL 구분값.
   *   <li>{@code SANCTIFIED_UNSUPPORTED}: 미지원 구분값.
   * </ul>
   */
  public enum Code {
    ENGLISH_FALLBACK,
    ENGLISH_REVEAL_TEMPLATE_FALLBACK,
    SOURCE_UNITS_FALLBACK,
    UNREVIEWED_QUALITY,
    UNSAFE_DISPLAY_INTEGER,
    UNRESOLVED_TEMPLATE_VALUE,
    HIDDEN_OPTIONS_OMITTED,
    JEWEL_EFFECT_AND_QUALITY_PROVISIONAL,
    SANCTIFIED_UNSUPPORTED
  }

  /**
   * {@code ChaosOptionDisplay}에서 사용하는 문제의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code Code}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code blocking}: blocking ({@code boolean}).
   * </ul>
   */
  public record Issue(Code code, String modifierId, boolean blocking) {}

  /**
   * {@code ChaosOptionDisplay}에서 사용하는 그룹의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code affixType}: 접두접미옵션·타입 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code fractured}: 분열고정 ({@code boolean}).
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   *   <li>{@code lines}: 행목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Group(id, affixType, fractured, text, lines)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       id}(식별자), {@code affixType}(접두접미옵션·타입), {@code fractured}(분열고정), {@code text}(텍스트),
   *       {@code lines}(행목록).
   * </ul>
   */
  public record Group(
      String id,
      ModifierDefinition.AffixType affixType,
      boolean fractured,
      String text,
      List<String> lines) {
    public Group {
      lines = List.copyOf(lines);
    }
  }

  /**
   * {@code ChaosOptionDisplay}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
   *   <li>{@code templateSha256}: 템플릿·SHA·256 ({@code String}).
   *   <li>{@code locale}: 언어코드 ({@code DisplayLocale}).
   *   <li>{@code groups}: 그룹목록 ({@code List<Group>}).
   *   <li>{@code issues}: 문제목록 ({@code List<Issue>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Result(version, templateSha256, locale, groups, issues)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code version}(버전), {@code templateSha256}(템플릿·SHA·256), {@code locale}(언어코드), {@code
   *       groups}(그룹목록), {@code issues}(문제목록).
   *   <li>{@code supported()}: 지원 값을 조회·구성한다. 반환: {@code boolean}.
   * </ul>
   */
  public record Result(
      String version,
      String templateSha256,
      DisplayLocale locale,
      List<Group> groups,
      List<Issue> issues) {
    public Result {
      groups = List.copyOf(groups);
      issues = List.copyOf(issues);
    }

    /** A raw source-ID/name fallback must never silently become an option-only matching target. */
    public boolean supported() {
      return issues.stream().noneMatch(Issue::blocking);
    }
  }

  public Result format(ItemState state, ItemCatalog catalog, DisplayLocale locale) {
    Objects.requireNonNull(locale);
    if (!new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Validated canonical item required for option display");
    List<Issue> issues = new ArrayList<>();
    if (state.conditions().contains(ItemState.Condition.SANCTIFIED)) {
      issues.add(new Issue(Code.SANCTIFIED_UNSUPPORTED, "", true));
      return result(locale, List.of(), issues);
    }
    List<Group> groups = new ArrayList<>();
    for (var modifier : state.explicits()) {
      var definition = catalog.find(modifier.modifierId()).orElseThrow();
      if (definition.layer() != ModifierDefinition.Layer.EXPLICIT
          || definition.affixType() == ModifierDefinition.AffixType.NONE) continue;
      if (modifier.values().values().stream()
          .anyMatch(v -> v < -MAX_SAFE_INTEGER || v > MAX_SAFE_INTEGER)) {
        issues.add(new Issue(Code.UNSAFE_DISPLAY_INTEGER, modifier.modifierId(), true));
        continue;
      }
      Map<String, Double> values = project(state, definition, modifier, issues);
      String text = localized(definition, values, locale, issues);
      // ItemCard.Lines performs this final pass too, including its existing English-fallback
      // behavior.
      text = DisplayNumbers.text(text, locale, locale);
      if (PLACEHOLDER.matcher(text).find())
        issues.add(new Issue(Code.UNRESOLVED_TEMPLATE_VALUE, definition.id(), true));
      groups.add(
          new Group(
              definition.id(),
              definition.affixType(),
              modifier.fractured(),
              text,
              List.of(text.split("\\n", -1))));
    }
    if (state.desecrated().stream().anyMatch(slot -> slot.modifierId() == null))
      issues.add(new Issue(Code.HIDDEN_OPTIONS_OMITTED, "", false));
    // CraftingPage normal view: fractures first, then prefixes, then suffixes; ties stay
    // source-stable.
    groups.sort(
        Comparator.comparingInt(
            g ->
                g.fractured() ? -1 : g.affixType() == ModifierDefinition.AffixType.PREFIX ? 1 : 2));
    return result(locale, groups, issues);
  }

  private Result result(DisplayLocale locale, List<Group> groups, List<Issue> issues) {
    return new Result(VERSION, templates.sha256(), locale, groups, issues);
  }

  private Map<String, Double> project(
      ItemState state,
      ModifierDefinition definition,
      ModifierInstance modifier,
      List<Issue> issues) {
    var values = baseProject(state, definition, modifier, issues);
    double factor =
        com.poe2craft.shared.item.ModifierMagnitudes.factor(state, definition, false).doubleValue();
    if (factor != 1) values.replaceAll((id, value) -> value * factor);
    return values;
  }

  private Map<String, Double> baseProject(
      ItemState state,
      ModifierDefinition definition,
      ModifierInstance modifier,
      List<Issue> issues) {
    var source = modifier.values();
    Map<String, Double> values = new LinkedHashMap<>();
    source.forEach((id, value) -> values.put(id, value.doubleValue()));
    var quality = state.catalystQuality();
    boolean basic = BASIC_JEWEL.matcher(definition.id()).find();
    if (definition.tags().contains("unscalable")
        || (!basic && source.containsKey("local_maximum_quality_+"))) return values;
    long q = quality != null && quality.type().matches(definition.tags()) ? quality.amount() : 0;
    long effect = basic ? BasicJewel.effect(state, definition.affixType()) : 0;
    if (basic
        ? q == 0 && effect == 0
        : quality == null || !quality.type().matches(definition.tags())) return values;
    if (basic && effect != 0)
      issues.add(new Issue(Code.JEWEL_EFFECT_AND_QUALITY_PROVISIONAL, definition.id(), false));
    // Unlike CatalystQualityDisplay, the frontend identifies jewel projection by modifier ID, and
    // display_cast_speed_percent is reviewed only for the specific sapphire:of-enchanting modifier.
    var binding = templates.binding(definition);
    boolean bound = binding != null && binding.valueStats() != null;
    boolean reviewed =
        bound
            || basic
            || definition.stats().size() == 1
                && (definition.id().startsWith("sapphire:")
                    || definition.id().equals("sapphire:suffix:of-enchanting")
                        && definition.stats().getFirst().id().equals("display_cast_speed_percent")
                    || definition.weight() > 0
                        && INTEGER_STATS.contains(definition.stats().getFirst().id()));
    if (!reviewed
        || !basic
            && !bound
            && (definition.stats().stream().anyMatch(s -> s.min() < 0)
                || source.values().stream().anyMatch(v -> v < 0))) {
      issues.add(new Issue(Code.UNREVIEWED_QUALITY, definition.id(), false));
      return values;
    }
    source.forEach(
        (id, value) -> {
          BigInteger numerator =
              BigInteger.valueOf(value)
                  .multiply(BigInteger.valueOf(100 + q))
                  .multiply(BigInteger.valueOf(100 + effect));
          long scaled = QualityRoundingPolicy.roundRatio(numerator, BigInteger.valueOf(10_000));
          if (scaled < -MAX_SAFE_INTEGER || scaled > MAX_SAFE_INTEGER)
            issues.add(new Issue(Code.UNSAFE_DISPLAY_INTEGER, definition.id(), true));
          values.put(id, (double) scaled);
        });
    return values;
  }

  /**
   * {@code ChaosOptionDisplay}에서 사용하는 Rendered의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   *   <li>{@code sourceUnits}: 출처·단위목록 ({@code boolean}).
   * </ul>
   */
  private record Rendered(String text, boolean sourceUnits) {}

  private String localized(
      ModifierDefinition definition,
      Map<String, Double> values,
      DisplayLocale locale,
      List<Issue> issues) {
    var english = rolled(definition, values);
    var binding = templates.binding(definition);
    var translation = binding == null ? null : templates.translation(definition, binding, locale);
    if (translation == null) {
      issues.add(new Issue(Code.ENGLISH_FALLBACK, definition.id(), false));
      if (english.sourceUnits())
        issues.add(new Issue(Code.SOURCE_UNITS_FALLBACK, definition.id(), true));
      return DisplayNumbers.text(english.text(), DisplayLocale.EN, DisplayLocale.EN);
    }
    if (templates.usesEnglishRevealFallback(definition, binding, locale))
      issues.add(new Issue(Code.ENGLISH_REVEAL_TEMPLATE_FALLBACK, definition.id(), false));
    List<String> numbers = new ArrayList<>();
    binding.values().forEach(v -> numbers.add(DisplayNumbers.text(v, DisplayLocale.EN, locale)));
    if (binding.valueStats() != null) {
      if (binding.valueStats().stream().anyMatch(s -> !values.containsKey(s.id()))) {
        issues.add(new Issue(Code.SOURCE_UNITS_FALLBACK, definition.id(), true));
        return english.text();
      }
      for (int i = 0; i < binding.valueStats().size(); i++) {
        var stat = binding.valueStats().get(i);
        double value = values.get(stat.id()) / stat.divisor();
        String prefix =
            i < numbers.size() && numbers.get(i).startsWith("+") && value >= 0 ? "+" : "";
        while (numbers.size() <= i) numbers.add("");
        numbers.set(i, prefix + DisplayNumbers.number(value, locale));
      }
      return substitute(translation.template(), numbers);
    }
    if (BASIC_JEWEL.matcher(definition.id()).find()) {
      for (int i = 0; i < numbers.size() && i < definition.stats().size(); i++) {
        Double value = values.get(definition.stats().get(i).id());
        if (value != null && value == Math.rint(value) && Math.abs(value) <= MAX_SAFE_INTEGER)
          numbers.set(
              i,
              (numbers.get(i).startsWith("+") ? "+" : "") + DisplayNumbers.number(value, locale));
      }
      return substitute(translation.template(), numbers);
    }
    if (!english.text().equals(definition.text())) {
      Matcher range = RANGE.matcher(definition.text());
      if (definition.stats().size() == 1
          && range.find()
          && numbers.size() == 1
          && rangeMatches(range, definition.stats().getFirst())) {
        String replaced =
            RANGE
                .matcher(binding.values().getFirst())
                .replaceFirst(
                    Matcher.quoteReplacement(
                        DisplayNumbers.sourceNumber(
                            values.get(definition.stats().getFirst().id()))));
        numbers.set(0, DisplayNumbers.text(replaced, DisplayLocale.EN, locale));
      } else {
        issues.add(new Issue(Code.SOURCE_UNITS_FALLBACK, definition.id(), true));
        return english.text().startsWith(definition.name() + ":")
            ? translation.name() + english.text().substring(definition.name().length())
            : english.text();
      }
    }
    return substitute(translation.template(), numbers);
  }

  private Rendered rolled(ModifierDefinition definition, Map<String, Double> values) {
    var binding = templates.englishBinding(definition);
    if (BASIC_JEWEL.matcher(definition.id()).find()
        && binding != null
        && !definition.tags().contains("unscalable")) {
      String text = definition.text();
      for (int i = 0; i < binding.values().size() && i < definition.stats().size(); i++) {
        Double value = values.get(definition.stats().get(i).id());
        if (value != null && value == Math.rint(value) && Math.abs(value) <= MAX_SAFE_INTEGER) {
          String previous = binding.values().get(i);
          String replacement =
              (previous.startsWith("+") ? "+" : "") + DisplayNumbers.sourceNumber(value);
          int index = text.indexOf(previous);
          if (index >= 0)
            text =
                text.substring(0, index) + replacement + text.substring(index + previous.length());
        }
      }
      return new Rendered(text, false);
    }
    if (definition.stats().size() > 1
        && values.size() == definition.stats().size()
        && definition.stats().stream()
            .allMatch(s -> s.min() == s.max() && values.get(s.id()) == s.min()))
      return new Rendered(definition.text(), false);
    if (definition.stats().size() == 1) {
      var stat = definition.stats().getFirst();
      Matcher range = RANGE.matcher(definition.text());
      if (range.find() && rangeMatches(range, stat))
        return new Rendered(
            range.replaceFirst(
                Matcher.quoteReplacement(DisplayNumbers.sourceNumber(values.get(stat.id())))),
            false);
      if (stat.min() == stat.max()) return new Rendered(definition.text(), false);
    }
    List<String> parts = new ArrayList<>();
    values.forEach(
        (id, value) ->
            parts.add(
                id.replace('_', ' ')
                    + " = "
                    + DisplayNumbers.sourceNumber(value)
                    + " (source units)"));
    return new Rendered(definition.name() + ": " + String.join(", ", parts), true);
  }

  private static boolean rangeMatches(Matcher match, ModifierDefinition.StatRange stat) {
    return Double.parseDouble(match.group(1)) == stat.min()
        && Double.parseDouble(match.group(2)) == stat.max();
  }

  private static String substitute(String template, List<String> numbers) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      matcher.appendReplacement(
          result,
          Matcher.quoteReplacement(index < numbers.size() ? numbers.get(index) : matcher.group()));
    }
    return matcher.appendTail(result).toString();
  }
}
