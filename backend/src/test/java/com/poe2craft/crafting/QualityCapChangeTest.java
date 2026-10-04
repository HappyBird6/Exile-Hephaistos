package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class QualityCapChangeTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator sim = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ModifierInstance breach =
      new ModifierInstance(QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L));

  ItemState state(List<ModifierInstance> affixes, int quality) {
    var root = SolarAmulet.initial(catalog);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        82,
        ItemState.Rarity.RARE,
        root.implicits(),
        affixes,
        Set.of(),
        null,
        new CatalystQuality(CatalystQuality.Type.FLESH, quality));
  }

  @Test
  void selectedRemovalPreservesWithoutMutatingBeforeOrRefillingWhenCapGrows() {
    var before = state(List.of(breach), 40);
    for (var action : List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.CHAOS)) {
      var result = sim.apply(before, action, Set.of(), new Random(1));
      assertThat(result.applied()).isTrue();
      assertThat(result.state().catalystQuality())
          .isEqualTo(new CatalystQuality(CatalystQuality.Type.FLESH, 40));
      assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1.0);
      assertThat(result.assumptions())
          .noneMatch(a -> a.id().equals(QualityCapChangePolicy.VERSION));
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
      for (var catalyst :
          List.of(WorkbenchCurrency.CATALYST_FLESH, WorkbenchCurrency.CATALYST_NEURAL)) {
        var repeated = sim.apply(result.state(), catalyst, Set.of(), new Random(1));
        assertThat(repeated.applied()).isTrue();
        assertThat(repeated.state().catalystQuality().amount()).isEqualTo(40);
        assertThat(repeated.state().catalystQuality().type()).isEqualTo(catalyst.catalystType());
      }
    }
    assertThat(before.catalystQuality().amount()).isEqualTo(40);
    assertThat(before.explicits()).containsExactly(breach);
    var capAgain =
        sim.apply(
            state(
                List.of(
                    new ModifierInstance(
                        "amulet:prefix:healthy", Map.of("base_maximum_life", 29L))),
                20),
            WorkbenchCurrency.ESSENCE_BREACH,
            Set.of(),
            new Random(1));
    assertThat(capAgain.applied()).isTrue();
    assertThat(capAgain.qualityLimit().maximumQuality()).isEqualTo(40);
    assertThat(capAgain.state().catalystQuality().amount()).isEqualTo(20);
    assertThat(
            sim.apply(capAgain.state(), WorkbenchCurrency.CATALYST_FLESH, Set.of(), new Random(1))
                .state()
                .catalystQuality()
                .amount())
        .isEqualTo(40);
  }

  @Test
  void omenFilteredCandidatesKeepTheirOriginalOddsAndChosenCapLossPreserves() {
    var suffix =
        new ModifierInstance("amulet:suffix:of-the-wrestler", Map.of("additional_strength", 9L));
    var before = state(List.of(breach, suffix), 40);
    var suffixOnly =
        sim.apply(
            before,
            WorkbenchCurrency.ANNULMENT,
            Set.of("Omen_of_Dextral_Annulment"),
            new Random(1));
    assertThat(suffixOnly.state().catalystQuality().amount()).isEqualTo(40);
    assertThat(suffixOnly.state().explicits()).containsExactly(breach);
    var prefixOnly =
        sim.apply(
            before,
            WorkbenchCurrency.ANNULMENT,
            Set.of("Omen_of_Sinistral_Annulment"),
            new Random(1));
    assertThat(prefixOnly.state().catalystQuality().amount()).isEqualTo(40);
    assertThat(prefixOnly.state().explicits()).containsExactly(suffix);
    assertThat(prefixOnly.events().getFirst().selectionProbability()).isEqualTo(1.0);
    var both = sim.apply(before, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(1));
    assertThat(both.events().getFirst().selectionProbability()).isEqualTo(0.5);
    assertThat(both.assumptions())
        .anyMatch(
            a ->
                a.id().equals("uniform-removal-v1")
                    && a.candidates().contains(QualityLimitRules.BREACH_ID)
                    && a.n() == 2);
  }

  @Test
  void rejectsUnreachableQualityWithoutTreatingCurrentCapAsStoredCeiling() {
    assertThat(new ItemStateValidator(catalog).validate(state(List.of(), 40))).isEmpty();
    assertThat(new ItemStateValidator(catalog).validate(state(List.of(), 41))).isNotEmpty();
    assertThat(new ItemStateValidator(catalog).validate(state(List.of(), 100))).isNotEmpty();
  }

  @Test
  void legacyRefusalIsAnExplicitReversiblePolicyToggle() {
    var legacy =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            Set.of(),
            Map.of(),
            Map.of(),
            QualityCapChangePolicy.REJECT_OVERCAP);
    var before = state(List.of(breach), 40);
    var result = legacy.apply(before, WorkbenchCurrency.ANNULMENT, Set.of(), new Random(1));
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.events()).isEmpty();
  }
}
