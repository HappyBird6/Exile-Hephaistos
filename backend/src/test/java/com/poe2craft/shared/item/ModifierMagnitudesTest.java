package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 원래 제공자 수치를 기준으로 출처 태그별 옵션 효과를 한 번 보정한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code physical}: physical; 초기값 {@code "heist_enchantment_physical_mod_effect_+%"} ({@code
 *       String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code definition(stat, tags)}: 정의 처리에서 {@code ModifierDefinition.StatRange}을(를) 사용한다. 입력:
 *       {@code stat}(수치), {@code tags}(태그목록). 반환: {@code ModifierDefinition}.
 *   <li>{@code state(modifiers)}: 현재 상태 표현을 조회한다. 입력: {@code modifiers}(옵션목록). 반환: {@code
 *       ItemState}.
 *   <li>{@code sovereignAlloyAmplifiesAllExplicitResistancesButNeverImplicitsOrStoredRolls()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code allRulesSelectTheirSourceTagsAndFreezeProviderStrength()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code historicalSanctificationCannotAmplifyAFracturedProvider()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code overlappingIncreasesAddAndElementalRequiresTheDamageTag()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code plainGameProviderTextRestoresAnOriginalPercentageWithoutRecursiveFeedback()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class ModifierMagnitudesTest {
  private final String physical = "heist_enchantment_physical_mod_effect_+%";

  private ModifierDefinition definition(String stat, Set<String> tags) {
    return new ModifierDefinition(
        "test:" + stat,
        "Test",
        ModifierDefinition.Layer.EXPLICIT,
        ModifierDefinition.AffixType.PREFIX,
        Set.of("test"),
        1,
        1,
        1,
        "Test",
        List.of(new ModifierDefinition.StatRange(stat, 0, 1000)),
        tags,
        "https://poe2db.tw/us/Modifiers");
  }

  private ItemState state(List<ModifierInstance> modifiers) {
    return new ItemState(
        "test",
        "Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1",
        100,
        ItemState.Rarity.RARE,
        List.of(),
        modifiers,
        Set.of());
  }

  @Test
  void sovereignAlloyAmplifiesAllExplicitResistancesButNeverImplicitsOrStoredRolls() {
    var provider =
        new ModifierInstance(
            "equipment:prefix:alloy-effect-of-resistance-mods1",
            Map.of("heist_enchantment_resistance_mod_effect_+%", 30L));
    for (var stat :
        List.of(
            "base_fire_damage_resistance_%",
            "base_cold_damage_resistance_%",
            "base_lightning_damage_resistance_%",
            "base_chaos_damage_resistance_%",
            "base_resist_all_elements_%")) {
      var resistance = new ModifierInstance("test:" + stat, Map.of(stat, 30L));
      var item = state(List.of(provider, resistance));
      var target = definition(stat, Set.of("resistance"));
      assertThat(ModifierMagnitudes.value(item, target, resistance, stat, false))
          .isEqualByComparingTo("39");
      assertThat(ModifierMagnitudes.value(item, target, resistance, stat, true))
          .isEqualByComparingTo("30");
      assertThat(resistance.values().get(stat)).isEqualTo(30L);
      assertThat(
              ModifierMagnitudes.value(state(List.of(resistance)), target, resistance, stat, false))
          .isEqualByComparingTo("30");
    }
  }

  @Test
  void allRulesSelectTheirSourceTagsAndFreezeProviderStrength() {
    assertThat(AugmentCraftingRules.magnitudeRules()).hasSize(10);
    for (var rule : AugmentCraftingRules.magnitudeRules()) {
      var provider = new ModifierInstance("test:provider", Map.of(rule.stat(), 20L));
      var target = definition("target", Set.copyOf(rule.tags()));
      assertThat(ModifierMagnitudes.factor(state(List.of(provider)), target, false))
          .isEqualByComparingTo("1.2");
      assertThat(ModifierMagnitudes.factor(state(List.of(provider)), target, true))
          .isEqualByComparingTo("1");
      assertThat(
              ModifierMagnitudes.factor(
                  state(List.of(provider)), definition("none", Set.of()), false))
          .isEqualByComparingTo("1");
    }
    var provider = new ModifierInstance("test:provider", Map.of(physical, 15L));
    var item = state(List.of(provider));
    assertThat(
            ModifierMagnitudes.value(
                item, definition(physical, Set.of("physical")), provider, physical, false))
        .isEqualByComparingTo("17.25");
    assertThat(ModifierMagnitudes.factor(item, definition("target", Set.of("physical")), false))
        .isEqualByComparingTo("1.15");
  }

  @Test
  void historicalSanctificationCannotAmplifyAFracturedProvider() {
    for (boolean fractured : List.of(false, true)) {
      var provider = new ModifierInstance("provider", Map.of(physical, 20L), fractured);
      var item =
          state(List.of(provider))
              .withSanctification(
                  new ItemState.Sanctification(
                      "explicit-stat-percent-v2",
                      Map.of(),
                      Map.of("provider", Map.of(physical, 122))));
      assertThat(ModifierMagnitudes.factor(item, definition("target", Set.of("physical")), false))
          .isEqualByComparingTo(fractured ? "1.2" : "1.24");
    }
  }

  @Test
  void overlappingIncreasesAddAndElementalRequiresTheDamageTag() {
    var item =
        state(
            List.of(
                new ModifierInstance("fire", Map.of("heist_enchantment_fire_mod_effect_+%", 20L)),
                new ModifierInstance(
                    "elemental", Map.of("local_explicit_elemental_damage_mod_effect_+%", 15L))));
    assertThat(
            ModifierMagnitudes.factor(
                item, definition("fire-damage", Set.of("fire", "elemental", "damage")), false))
        .isEqualByComparingTo("1.35");
    assertThat(
            ModifierMagnitudes.factor(
                item, definition("fire-resist", Set.of("fire", "elemental", "resistance")), false))
        .isEqualByComparingTo("1.2");
    assertThat(
            ModifierMagnitudes.factor(
                item, definition("grant", Set.of("unscalable", "fire")), false))
        .isEqualByComparingTo("1");
    assertThat(
            ModifierMagnitudes.factor(
                item.withConditions(Set.of(ItemState.Condition.SANCTIFIED)),
                definition("fire-damage", Set.of("fire", "elemental", "damage")),
                false))
        .isEqualByComparingTo("1");
  }

  @Test
  void plainGameProviderTextRestoresAnOriginalPercentageWithoutRecursiveFeedback()
      throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    try (var input =
        com.poe2craft.shared.ruleset.SealedResources.open("/catalog/augment-crafting.json")) {
      var d =
          json.readValue(input, AugmentCraftingRules.Document.class).modifiers().stream()
              .filter(v -> v.stats().stream().anyMatch(s -> s.id().equals(physical)))
              .findFirst()
              .orElseThrow();
      var parsed =
          ModifierMagnitudes.matchProvider(
              d, "17.25% increased Explicit Physical Modifier magnitudes", List.of());
      assertThat(parsed).isNotNull();
      assertThat(parsed.values()).containsEntry(physical, 15L);
    }
  }
}
