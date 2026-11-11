package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

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
  void allNineRulesSelectTheirSourceTagsAndFreezeProviderStrength() {
    assertThat(AugmentCraftingRules.magnitudeRules()).hasSize(9);
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
