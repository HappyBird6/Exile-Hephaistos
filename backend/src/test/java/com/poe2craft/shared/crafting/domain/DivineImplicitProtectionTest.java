package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DivineImplicitProtectionTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  private final Random maximum =
      new Random() {
        @Override
        public long nextLong(long bound) {
          return bound - 1;
        }
      };

  private ItemState item(List<ModifierInstance> explicits) {
    var root = SolarAmulet.initial(catalog, 82, 10);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        ItemState.Rarity.RARE,
        root.implicits(),
        explicits,
        Set.of());
  }

  @Test
  void ordinaryDivineRerollsExplicitRangesAndPreservesUnfracturedBaseImplicit() {
    var before =
        item(
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().implicits()).isEqualTo(before.implicits());
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_maximum_life", 29L);
    assertThat(result.events()).allMatch(e -> e.kind().equals("REROLL_EXPLICIT"));
  }

  @Test
  void aPublishedFixedExplicitRollStaysFixedBesideAVariableExplicitRoll() {
    var fixed =
        new ModifierInstance(
            "amulet:prefix:essence-maximum-quality", Map.of("local_maximum_quality_+", 20L));
    var before =
        item(
            List.of(
                fixed,
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().implicits()).isEqualTo(before.implicits());
    assertThat(result.state().explicits().getFirst()).isEqualTo(fixed);
    assertThat(result.events()).hasSize(1);
    assertThat(result.events().getFirst().modifierId()).isEqualTo("amulet:prefix:healthy");
  }

  @Test
  void variableImplicitAloneDoesNotMakeOrdinaryDivineAvailable() {
    var before = item(List.of());
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.events()).isEmpty();
  }

  @Test
  void fractureProtectionIsSeparateAndNeverMakesImplicitEligible() {
    var before =
        item(
            List.of(
                new ModifierInstance(
                    "amulet:prefix:healthy", Map.of("base_maximum_life", 20L), true)));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void explicitBlessedOmenKeepsItsExistingImplicitOnlyContract() {
    var before =
        item(
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result =
        simulator.apply(
            before, WorkbenchCurrency.DIVINE, Set.of(WorkbenchOmen.BLESSED.id()), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).isEqualTo(before.explicits());
    assertThat(result.state().implicits()).isNotEqualTo(before.implicits());
    assertThat(result.events()).allMatch(e -> e.kind().equals("REROLL_IMPLICIT"));
    assertThat(result.consumedOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
  }
}
