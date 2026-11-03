package com.poe2craft.item;

import java.math.BigDecimal;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Explicit simulator policy: independent exact integer-percent factors, never edited source rolls.
 */
public final class QualitySanctification {
  public static final String POLICY = "independent-stat-percent-v1";
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
    var definition = catalog.find(modifier.modifierId()).orElseThrow();
    if (definition.tags().contains("unscalable")) return Set.of();
    var targets = new TreeSet<>(modifier.values().keySet());
    targets.removeAll(STRUCTURAL);
    return Set.copyOf(targets);
  }

  public static ItemState.Sanctification roll(
      ItemState state, ItemCatalog catalog, RandomGenerator random) {
    return new ItemState.Sanctification(
        POLICY,
        rollLayer(state.implicits(), catalog, random),
        rollLayer(state.explicits(), catalog, random));
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
        || (validLayer(state.implicits(), value.implicits(), catalog)
            && validLayer(state.explicits(), value.explicits(), catalog));
  }

  private static boolean validLayer(
      List<ModifierInstance> modifiers,
      Map<String, Map<String, Integer>> factors,
      ItemCatalog catalog) {
    var expected = new TreeMap<String, Set<String>>();
    for (var modifier : modifiers) {
      if (catalog.find(modifier.modifierId()).isEmpty()) return false;
      var targets = targets(modifier, catalog);
      if (!targets.isEmpty()) expected.put(modifier.modifierId(), targets);
    }
    return expected.keySet().equals(factors.keySet())
        && expected.entrySet().stream()
            .allMatch(e -> e.getValue().equals(factors.get(e.getKey()).keySet()));
  }

  public static BigDecimal value(
      ItemState state, ModifierInstance modifier, String stat, boolean implicit) {
    int factor =
        state.sanctification() == null
            ? 100
            : (implicit ? state.sanctification().implicits() : state.sanctification().explicits())
                .getOrDefault(modifier.modifierId(), Map.of())
                .getOrDefault(stat, 100);
    return BigDecimal.valueOf(modifier.values().get(stat)).multiply(BigDecimal.valueOf(factor, 2));
  }
}
