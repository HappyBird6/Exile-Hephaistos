package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class CoupledStatRollModelTest {
  ModifierDefinition fixture() {
    return new ModifierDefinition(
        "proof:hybrid",
        "Oyster's",
        ModifierDefinition.Layer.EXPLICIT,
        ModifierDefinition.AffixType.PREFIX,
        Set.of("BaseLocalDefencesAndLife"),
        8,
        1000,
        1,
        "(6-13)% increased Armour; +(7-10) to maximum Life",
        List.of(
            new ModifierDefinition.StatRange("local physical damage reduction rating +%", 6, 13),
            new ModifierDefinition.StatRange("base maximum life", 7, 10)),
        Set.of(),
        "https://poe2db.tw/us/hover?s=Data%5CMods%2FLocalIncreasedArmourAndLife1");
  }

  @Test
  void sharedMidpointProducesCoupledValuesAndExplicitConjectureLedger() {
    var assumptions = new ArrayList<WorkbenchSimulator.Assumption>();
    var random =
        new Random() {
          @Override
          public int nextInt(int bound) {
            assertThat(bound).isEqualTo(10001);
            return 5000;
          }
        };
    var rolled = CoupledStatRollModel.roll(fixture(), random, assumptions);
    assertThat(rolled.values())
        .containsEntry("local physical damage reduction rating +%", 10L)
        .containsEntry("base maximum life", 9L);
    assertThat(assumptions)
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.id()).isEqualTo(CoupledStatRollModel.ID);
              assertThat(a.n()).isEqualTo(10001);
              assertThat(a.reason())
                  .contains("Unverified user conjecture", "not a game-verified", "HALF_UP");
            });
  }

  @Test
  void endpointsFixedStatsNegativeTiesAndFullLongRangeRemainExactAndBounded() {
    for (var range :
        List.of(
            new ModifierDefinition.StatRange("signed", -3, 2),
            new ModifierDefinition.StatRange("fixed", -7, -7),
            new ModifierDefinition.StatRange("extreme", Long.MIN_VALUE, Long.MAX_VALUE))) {
      assertThat(CoupledStatRollModel.value(range, 0)).isEqualTo(range.min());
      assertThat(CoupledStatRollModel.value(range, 10000)).isEqualTo(range.max());
      long previous = range.min();
      for (int tick = 0; tick <= 10000; tick++) {
        long actual = CoupledStatRollModel.value(range, tick);
        assertThat(actual).isBetween(range.min(), range.max()).isGreaterThanOrEqualTo(previous);
        previous = actual;
      }
    }
    assertThat(
            CoupledStatRollModel.value(new ModifierDefinition.StatRange("negative", -2, -1), 5000))
        .isEqualTo(-2);
    assertThat(CoupledStatRollModel.value(new ModifierDefinition.StatRange("positive", 1, 2), 5000))
        .isEqualTo(2);
    assertThatThrownBy(() -> CoupledStatRollModel.value(fixture().stats().getFirst(), -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void coupledDomainDoesNotBecomeCartesianOrUniformRoundedTuples() {
    var tuples = new HashMap<List<Long>, Integer>();
    for (int tick = 0; tick <= 10000; tick++) {
      int ratioTick = tick;
      var pair =
          fixture().stats().stream().map(s -> CoupledStatRollModel.value(s, ratioTick)).toList();
      tuples.merge(pair, 1, Integer::sum);
    }
    assertThat(tuples).doesNotContainKey(List.of(6L, 10L));
    assertThat(tuples.size()).isLessThan(32);
    assertThat(new HashSet<>(tuples.values())).hasSizeGreaterThan(1);
  }

  @Test
  void simulatorRejectsUnknownOrSingleStatOptInsAndDefaultSolarHasNoCoupledModel() {
    var catalog = ItemCatalogLoader.loadDefault();
    var engine = new CraftingEngine(catalog);
    assertThatThrownBy(() -> new WorkbenchSimulator(catalog, engine, Set.of("missing")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new WorkbenchSimulator(catalog, engine, Set.of("amulet:prefix:hale")))
        .isInstanceOf(IllegalArgumentException.class);
    var result =
        new WorkbenchSimulator(catalog, engine)
            .apply(SolarAmulet.initial(catalog), CraftingAction.TRANSMUTATION, new Random(1));
    assertThat(result.assumptions()).noneMatch(a -> a.id().equals(CoupledStatRollModel.ID));
  }
}
