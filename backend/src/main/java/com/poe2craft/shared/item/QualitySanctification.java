package com.poe2craft.shared.item;

import java.math.BigDecimal;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 원래 수치를 편집하지 않고 독립 정수 배율의 앱 신성화 정책을 적용한다.
 *
 * <p>기존 계약: Explicit simulator policy: independent exact integer-percent factors, never edited
 * source rolls.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code POLICY}: 정책; 초기값 {@code "unfractured-explicit-stat-percent-v3"} ({@code String}).
 *   <li>{@code ASSUMPTION}: ASSUMPTION; 초기값 {@code "sanctification-unfractured-rounded-v3"} ({@code
 *       String}).
 *   <li>{@code READABLE_POLICIES}: 정책목록 ({@code Set<String>}).
 *   <li>{@code STRUCTURAL}: 구조적 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code recognizedPolicy(policy)}: recognized·정책 처리에서 {@code READABLE_POLICIES.contains}을(를)
 *       사용한다. 입력: {@code policy}(정책). 반환: {@code boolean}.
 *   <li>{@code QualitySanctification()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code targets(modifier, catalog)}: 대상목록 처리에서 {@code modifier.fractured}을(를) 사용한다. 입력:
 *       {@code modifier}(옵션), {@code catalog}(카탈로그). 반환: {@code Set<String>}.
 *   <li>{@code scalableStats(modifier, catalog)}: scalable·수치목록 처리에서 {@code catalog.find}, {@code
 *       modifier.modifierId}을(를) 사용한다. 입력: {@code modifier}(옵션), {@code catalog}(카탈로그). 반환: {@code
 *       Set<String>}.
 *   <li>{@code roll(state, catalog, random)}: 정의된 수치 범위와 분포 가정에 따라 난수로 결과를 정한다. 입력: {@code
 *       state}(상태), {@code catalog}(카탈로그), {@code random}(난수생성기). 반환: {@code
 *       ItemState.Sanctification}.
 *   <li>{@code rollLayer(modifiers, catalog, random)}: 수치굴림·옵션계층 처리에서 {@code factors.put}, {@code
 *       random.nextInt}을(를) 사용한다. 입력: {@code modifiers}(옵션목록), {@code catalog}(카탈로그), {@code
 *       random}(난수생성기). 반환: {@code Map<String, Map<String, Integer>>}.
 *   <li>{@code valid(state, catalog)}: 유효한 처리에서 {@code state.sanctification}, {@code
 *       value.implicits}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그). 반환: {@code
 *       boolean}.
 *   <li>{@code validLayer(modifiers, factors, catalog, legacy)}: 유효한·옵션계층 처리에서 {@code
 *       catalog.find}, {@code modifier.modifierId}을(를) 사용한다. 입력: {@code modifiers}(옵션목록), {@code
 *       factors}(배율목록), {@code catalog}(카탈로그), {@code legacy}(구버전). 반환: {@code boolean}.
 *   <li>{@code value(state, modifier, stat, implicit)}: 값 처리에서 {@code state.sanctification}, {@code
 *       modifier.fractured}을(를) 사용한다. 입력: {@code state}(상태), {@code modifier}(옵션), {@code
 *       stat}(수치), {@code implicit}(암시옵션). 반환: {@code BigDecimal}.
 * </ul>
 */
public final class QualitySanctification {
  public static final String POLICY = "unfractured-explicit-stat-percent-v3";
  public static final String ASSUMPTION = "sanctification-unfractured-rounded-v3";
  private static final Set<String> READABLE_POLICIES =
      Set.of("independent-stat-percent-v1", "explicit-stat-percent-v2", POLICY);

  public static boolean recognizedPolicy(String policy) {
    return policy != null && READABLE_POLICIES.contains(policy);
  }

  private static final Set<String> STRUCTURAL =
      Set.of(
          "local_maximum_quality_+",
          "local_maximum_prefixes_allowed_+",
          "local_maximum_suffixes_allowed_+",
          "local_attribute_requirements_+%",
          "local_charm_slots",
          "local_item_additional_skill_slots",
          "local_item_benefit_socketable_as_if_helmet",
          "local_display_grants_spear_throw_skill",
          "local_always_hit",
          "local_jewel_effect_base_radius");

  private QualitySanctification() {}

  public static Set<String> targets(ModifierInstance modifier, ItemCatalog catalog) {
    return modifier.fractured() ? Set.of() : scalableStats(modifier, catalog);
  }

  // Legacy validation retains stored factor maps; new rolls always use targets above.
  private static Set<String> scalableStats(ModifierInstance modifier, ItemCatalog catalog) {
    var definition = catalog.find(modifier.modifierId()).orElseThrow();
    if (definition.tags().contains("unscalable")) return Set.of();
    var targets = new TreeSet<>(modifier.values().keySet());
    targets.removeAll(STRUCTURAL);
    return Set.copyOf(targets);
  }

  public static ItemState.Sanctification roll(
      ItemState state, ItemCatalog catalog, RandomGenerator random) {
    return new ItemState.Sanctification(
        POLICY, Map.of(), rollLayer(state.explicits(), catalog, random));
  }

  private static Map<String, Map<String, Integer>> rollLayer(
      List<ModifierInstance> modifiers, ItemCatalog catalog, RandomGenerator random) {
    var result = new TreeMap<String, Map<String, Integer>>();
    for (var modifier : modifiers) {
      var factors = new TreeMap<String, Integer>();
      for (var stat : new TreeSet<>(targets(modifier, catalog)))
        factors.put(stat, random.nextInt(78, 123));
      if (!factors.isEmpty()) result.put(modifier.modifierId(), factors);
    }
    return result;
  }

  public static boolean valid(ItemState state, ItemCatalog catalog) {
    var value = state.sanctification();
    return value == null
        || ((value.implicits().isEmpty()
                || (value.policy().equals("independent-stat-percent-v1")
                    && validLayer(state.implicits(), value.implicits(), catalog, true)))
            && validLayer(
                state.explicits(), value.explicits(), catalog, !POLICY.equals(value.policy())));
  }

  private static boolean validLayer(
      List<ModifierInstance> modifiers,
      Map<String, Map<String, Integer>> factors,
      ItemCatalog catalog,
      boolean legacy) {
    var expected = new TreeMap<String, Set<String>>();
    for (var modifier : modifiers) {
      if (catalog.find(modifier.modifierId()).isEmpty()) return false;
      var targets = legacy ? scalableStats(modifier, catalog) : targets(modifier, catalog);
      if (!targets.isEmpty()) expected.put(modifier.modifierId(), targets);
    }
    return expected.keySet().equals(factors.keySet())
        && expected.entrySet().stream()
            .allMatch(e -> e.getValue().equals(factors.get(e.getKey()).keySet()));
  }

  public static BigDecimal value(
      ItemState state, ModifierInstance modifier, String stat, boolean implicit) {
    int factor =
        state.sanctification() == null || implicit || modifier.fractured()
            ? 100
            : state
                .sanctification()
                .explicits()
                .getOrDefault(modifier.modifierId(), Map.of())
                .getOrDefault(stat, 100);
    return BigDecimal.valueOf(modifier.values().get(stat))
        .multiply(BigDecimal.valueOf(factor, 2))
        .setScale(0, java.math.RoundingMode.HALF_UP);
  }
}
