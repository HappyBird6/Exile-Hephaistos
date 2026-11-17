package com.poe2craft.shared.item;

import java.math.BigDecimal;
import java.util.*;

/**
 * 원래 제공자 수치를 기준으로 출처 태그별 옵션 효과를 한 번 보정한다.
 *
 * <p>기존 계약: Source-tagged explicit effects; original provider rolls select a single nonrecursive
 * pass.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ModifierMagnitudes()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code provider(definition)}: provider 처리에서 {@code definition.stats}, {@code
 *       AugmentCraftingRules.magnitudeRules}을(를) 사용한다. 입력: {@code definition}(정의). 반환: {@code
 *       boolean}.
 *   <li>{@code factor(providers, definition)}: 배율 처리에서 {@code definition.layer}, {@code
 *       definition.tags}을(를) 사용한다. 입력: {@code providers}(providers), {@code definition}(정의). 반환:
 *       {@code BigDecimal}.
 *   <li>{@code factor(state, definition, implicit)}: 배율 처리에서 {@code state.observedSanctified},
 *       {@code state.explicits}을(를) 사용한다. 입력: {@code state}(상태), {@code definition}(정의), {@code
 *       implicit}(암시옵션). 반환: {@code BigDecimal}.
 *   <li>{@code value(state, definition, modifier, stat, implicit)}: 값 처리에서 {@code
 *       QualitySanctification.value}을(를) 사용한다. 입력: {@code state}(상태), {@code definition}(정의),
 *       {@code modifier}(옵션), {@code stat}(수치), {@code implicit}(암시옵션). 반환: {@code BigDecimal}.
 *   <li>{@code rawMetadata(metadata)}: 원문·메타데이터 처리에서 {@code Pattern.compile}, {@code
 *       metadata.raw}을(를) 사용한다. 입력: {@code metadata}(메타데이터). 반환: {@code boolean}.
 *   <li>{@code clipboardFactor(definition, providers, metadata, finalValues)}: clipboard·배율의 입력에 따른
 *       분기·결과를 처리한다. 입력: {@code definition}(정의), {@code providers}(providers), {@code
 *       metadata}(메타데이터), {@code finalValues}(최종·값목록). 반환: {@code BigDecimal}.
 *   <li>{@code matchProvider(definition, text, providers)}: 일치·Provider 처리에서 {@code
 *       definition.stats}, {@code range.max}을(를) 사용한다. 입력: {@code definition}(정의), {@code
 *       text}(텍스트), {@code providers}(providers). 반환: {@code ModifierInstance}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Rule}: {@code ModifierMagnitudes}에서 사용하는 규칙의 불변 값이다.
 * </ul>
 */
public final class ModifierMagnitudes {
  private ModifierMagnitudes() {}

  /**
   * {@code ModifierMagnitudes}에서 사용하는 규칙의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code tags}: 태그목록 ({@code List<String>}).
   *   <li>{@code description}: 설명 ({@code String}).
   *   <li>{@code consumers}: consumers ({@code List<String>}).
   * </ul>
   */
  public record Rule(String stat, List<String> tags, String description, List<String> consumers) {}

  public static boolean provider(ModifierDefinition definition) {
    return definition.stats().stream()
        .anyMatch(
            s ->
                AugmentCraftingRules.magnitudeRules().stream()
                    .anyMatch(r -> r.stat().equals(s.id())));
  }

  public static BigDecimal factor(List<ModifierInstance> providers, ModifierDefinition definition) {
    if (definition.layer() != ModifierDefinition.Layer.EXPLICIT
        || definition.tags().contains("unscalable")
        || definition.text().lines().anyMatch(l -> l.startsWith("Grants Skill:")))
      return BigDecimal.ONE;
    long amount = 0;
    for (var rule : AugmentCraftingRules.magnitudeRules())
      if (definition.tags().containsAll(rule.tags()))
        for (var modifier : providers) amount += modifier.values().getOrDefault(rule.stat(), 0L);
    return BigDecimal.ONE.add(BigDecimal.valueOf(amount, 2));
  }

  public static BigDecimal factor(
      ItemState state, ModifierDefinition definition, boolean implicit) {
    if (implicit || state.observedSanctified()) return BigDecimal.ONE;
    var raw = factor(state.explicits(), definition);
    if (state.sanctification() == null || raw.compareTo(BigDecimal.ONE) == 0) return raw;
    BigDecimal amount = BigDecimal.ZERO;
    for (var rule : AugmentCraftingRules.magnitudeRules())
      if (definition.tags().containsAll(rule.tags()))
        for (var provider : state.explicits())
          if (provider.values().containsKey(rule.stat()))
            amount = amount.add(QualitySanctification.value(state, provider, rule.stat(), false));
    return BigDecimal.ONE.add(amount.movePointLeft(2));
  }

  public static BigDecimal value(
      ItemState state,
      ModifierDefinition definition,
      ModifierInstance modifier,
      String stat,
      boolean implicit) {
    return QualitySanctification.value(state, modifier, stat, implicit)
        .multiply(factor(state, definition, implicit));
  }

  public static boolean rawMetadata(com.poe2craft.shared.item.text.ItemModels.TextLine metadata) {
    return metadata != null
        && java.util.regex.Pattern.compile("(?:—|-)\\s+[0-9]+(?:\\.[0-9]+)?% Increased\\s*}")
            .matcher(metadata.raw())
            .find();
  }

  public static BigDecimal clipboardFactor(
      ModifierDefinition definition,
      List<ModifierInstance> providers,
      com.poe2craft.shared.item.text.ItemModels.TextLine metadata,
      boolean finalValues) {
    return finalValues || rawMetadata(metadata) ? BigDecimal.ONE : factor(providers, definition);
  }

  /** Provider percentages may amplify their own displayed value, but never recursively. */
  static ModifierInstance matchProvider(
      ModifierDefinition definition, String text, List<ModifierInstance> providers) {
    var range = definition.stats().getFirst();
    if (definition.stats().size() != 1 || range.max() - range.min() > 100) return null;
    var numbers = java.util.regex.Pattern.compile("[+-]?\\d+(?:\\.\\d+)?").matcher(text);
    if (!numbers.find()) return null;
    var observed = new BigDecimal(numbers.group());
    ModifierInstance best = null;
    BigDecimal bestError = null;
    for (long value = range.min(); value <= range.max(); value++) {
      var candidate = new ModifierInstance(definition.id(), Map.of(range.id(), value));
      var context = new ArrayList<>(providers);
      context.add(candidate);
      var scale = factor(context, definition);
      var matched = ModifierClipboardText.match(definition, text, scale, true, false);
      if (matched == null || matched.values().get(range.id()) != value) continue;
      var error = BigDecimal.valueOf(value).multiply(scale).subtract(observed).abs();
      if (bestError == null || error.compareTo(bestError) < 0) {
        best = matched;
        bestError = error;
      }
    }
    return best;
  }
}
