package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/** A local real-catalog equivalence check, not a proof of the entire reachable Solar quotient. */
class SolarRollClassTest {
  @Test
  void spirit47Through50HaveEqualProjectedBasicLawsButDifferentFullGoalArrivals() {
    var catalog = ItemCatalogLoader.loadDefault();
    var transitions = new BasicCurrencyTransitions(catalog, "solar-roll-class-proof");
    var initial = SolarAmulet.initial(catalog, 82, 15);
    var reference = new EnumMap<WorkbenchCurrency, Map<Object, Fraction>>(WorkbenchCurrency.class);
    var fullArrivals = new HashMap<Long, Set<BasicCurrencyState>>();
    for (long spirit = 47; spirit <= 50; spirit++) {
      var source =
          new BasicCurrencyState(
              new ItemState(
                  initial.snapshotId(),
                  initial.baseItemId(),
                  82,
                  ItemState.Rarity.RARE,
                  initial.implicits(),
                  List.of(
                      new ModifierInstance(
                          "amulet:prefix:countess", Map.of("base_spirit_from_equipment", spirit))),
                  Set.of()),
              transitions.provenance());
      for (var action : BasicCurrencyTransitions.supportedActions()) {
        var cursor = transitions.primitiveCursor(source, action, Set.of());
        var projected = new HashMap<Object, Fraction>();
        if (cursor.blocked()) {
          projected.put(List.of("blocked", cursor.reason(), cursor.status()), Fraction.ONE);
        } else
          while (!cursor.complete()) {
            var outcome = cursor.next();
            projected.merge(shape(outcome.state().item()), outcome.probability(), Fraction::add);
            if (action == WorkbenchCurrency.EXALTED && goal(outcome.state().item()))
              fullArrivals.computeIfAbsent(spirit, ignored -> new HashSet<>()).add(outcome.state());
          }
        if (spirit == 47) reference.put(action, Map.copyOf(projected));
        else
          assertThat(projected)
              .as("spirit %s, action %s", spirit, action)
              .isEqualTo(reference.get(action));
      }
    }
    assertThat(fullArrivals).hasSize(4);
    for (var arrivals : fullArrivals.values()) assertThat(arrivals).isNotEmpty();
    assertThat(Collections.disjoint(fullArrivals.get(47L), fullArrivals.get(50L))).isTrue();
  }

  private static boolean goal(ItemState item) {
    return item.explicits().stream()
            .anyMatch(m -> m.values().getOrDefault("projectile_skill_gem_level_+", 0L) >= 3)
        && item.explicits().stream()
            .anyMatch(m -> m.values().getOrDefault("base_spirit_from_equipment", 0L) >= 47);
  }

  private static Object shape(ItemState item) {
    // Collapse only this one known same-tier Spirit interval; preserve every other roll exactly.
    var explicits =
        item.explicits().stream()
            .map(
                m ->
                    m.modifierId().equals("amulet:prefix:countess")
                        ? new ModifierInstance(
                            m.modifierId(),
                            Map.of("base_spirit_from_equipment", 47L),
                            m.fractured())
                        : m)
            .toList();
    return List.of(
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            explicits,
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality()),
        goal(item));
  }
}
