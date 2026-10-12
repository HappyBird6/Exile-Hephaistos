package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrimitiveCursorTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final BasicCurrencyTransitions transitions =
      new BasicCurrencyTransitions(catalog, "cursor-fixture-v1");

  BasicCurrencyState state(List<ModifierInstance> explicits) {
    var initial = SolarAmulet.initial(catalog, 82, 15);
    return new BasicCurrencyState(
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            initial.implicits(),
            explicits,
            Set.of(),
            null),
        transitions.provenance());
  }

  ModifierInstance modifier(String stat) {
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.stats().size() == 1
                        && d.stats().getFirst().id().equals(stat))
            .findFirst()
            .orElseThrow();
    return new ModifierInstance(definition.id(), Map.of(stat, definition.stats().getFirst().max()));
  }

  @Test
  void streamingAndIndependentExistingEnumerationAgreeForAdditionChaosAndTwoWayRemoval() {
    var source =
        state(List.of(modifier("base_maximum_life"), modifier("base_chaos_damage_resistance_%")));
    for (var action :
        List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.CHAOS, WorkbenchCurrency.ANNULMENT)) {
      var exhaustive = transitions.expand(source, action, Set.of(), 1000000);
      assertThat(exhaustive.status()).isEqualTo(BasicCurrencyTransitions.Status.COMPLETE);
      var cursor = transitions.primitiveCursor(source, action, Set.of());
      var actual = new LinkedHashMap<BasicCurrencyState, Fraction>();
      assertThat(cursor.unresolved()).isEqualTo(Fraction.ONE);
      while (!cursor.complete()) {
        var next = cursor.next();
        actual.merge(next.state(), next.probability(), Fraction::add);
      }
      var expected = new LinkedHashMap<BasicCurrencyState, Fraction>();
      exhaustive.outcomes().forEach(o -> expected.merge(o.state(), o.probability(), Fraction::add));
      assertThat(actual).isEqualTo(expected);
      assertThat(cursor.unresolved()).isEqualTo(Fraction.ZERO);
      if (action == WorkbenchCurrency.ANNULMENT) {
        assertThat(actual).hasSize(2);
        assertThat(actual.values()).containsOnly(Fraction.of(1, 2));
      }
    }
  }

  @Test
  void existingLifeIsPreservedOnAdditionAndChaosResistanceIsDiscoverable() {
    var life = modifier("base_maximum_life");
    var cursor =
        transitions.primitiveCursor(state(List.of(life)), WorkbenchCurrency.EXALTED, Set.of());
    boolean found = false;
    while (!cursor.complete()) {
      var next = cursor.next();
      assertThat(next.state().item().explicits()).contains(life);
      found |=
          next.state().item().explicits().stream()
              .anyMatch(m -> m.values().containsKey("base_chaos_damage_resistance_%"));
    }
    assertThat(found).isTrue();
  }

  @Test
  void unsupportedOmensAreNotConvertedToEmptyCompleteKernels() {
    var cursor =
        transitions.primitiveCursor(state(List.of()), WorkbenchCurrency.EXALTED, Set.of("unknown"));
    assertThat(cursor.status()).isEqualTo(BasicCurrencyTransitions.Status.UNSUPPORTED);
    assertThat(cursor.complete()).isFalse();
    assertThat(cursor.unresolved()).isEqualTo(Fraction.ONE);
    assertThatThrownBy(cursor::next).isInstanceOf(IllegalStateException.class);
  }
}
