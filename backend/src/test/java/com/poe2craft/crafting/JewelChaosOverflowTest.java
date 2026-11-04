package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.application.chaosrepeat.ChaosRepeatRunner;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.ModifierDefinition.AffixType;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JewelChaosOverflowTest {
  private final JewelFiveAffixTest fixtures = new JewelFiveAffixTest();

  private List<ModifierInstance> ordinary(ItemCatalog catalog, int p, int s) {
    return fixtures.ordinary(catalog, p, s);
  }

  private ItemState item(ItemCatalog catalog, List<ModifierInstance> modifiers) {
    return fixtures.item(catalog, modifiers);
  }

  private static final List<WorkbenchCurrency> CHAOS =
      List.of(
          WorkbenchCurrency.CHAOS,
          WorkbenchCurrency.GREATER_CHAOS,
          WorkbenchCurrency.PERFECT_CHAOS);

  private List<ModifierInstance> side(ItemState item, ItemCatalog catalog, AffixType side) {
    return item.explicits().stream()
        .filter(m -> catalog.find(m.modifierId()).orElseThrow().affixType() == side)
        .toList();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ruby",
        "emerald",
        "sapphire",
        "diamond",
        "time-lost-ruby",
        "time-lost-emerald",
        "time-lost-sapphire",
        "time-lost-diamond"
      })
  void allChaosTiersReplaceOnlyTheSideWithinItsCurrentCap(String base) {
    var c =
        base.equals("sapphire")
            ? ItemCatalogLoader.loadSapphire()
            : ItemCatalogLoader.loadBasicJewel(base);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var locked : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      var before =
          item(
              c,
              ordinary(c, locked == AffixType.PREFIX ? 3 : 2, locked == AffixType.SUFFIX ? 3 : 2));
      assertThat(BasicJewel.chaosLockedSides(before, c)).containsExactly(locked);
      for (var action : CHAOS) {
        for (int seed = 0; seed < 8; seed++) {
          var result = sim.apply(before, action, Set.of(), new Random(seed));
          assertThat(result.applied()).as(base + " " + action + " " + locked).isTrue();
          assertThat(result.state().explicits()).hasSize(5);
          assertThat(side(result.state(), c, locked))
              .containsExactlyInAnyOrderElementsOf(side(before, c, locked));
          assertThat(result.events()).hasSize(2);
          var removal = result.events().getFirst();
          assertThat(c.find(removal.modifierId()).orElseThrow().affixType()).isNotEqualTo(locked);
          assertThat(removal.selectionProbability()).isEqualTo(0.5);
          assertThat(result.consumedOmens()).isEmpty();
          assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
        }
        String conflicting =
            locked == AffixType.PREFIX ? "Omen_of_Sinistral_Erasure" : "Omen_of_Dextral_Erasure";
        var rejected = sim.apply(before, action, Set.of(conflicting), new Random(1));
        assertThat(rejected.applied()).isFalse();
        assertThat(rejected.state()).isEqualTo(before);
        assertThat(rejected.consumedOmens()).isEmpty();
        assertThat(rejected.remainingOmens()).containsExactly(conflicting);
      }
    }
  }

  @Test
  void hiddenOverflowAndItsSlotIdentityAreProtectedBeforeChaosSelection() {
    var c = ItemCatalogLoader.loadSapphire();
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var executor = new AbyssCurrencyExecutor(c, sim);
    for (var locked : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      var slots = new ArrayList<>(AbyssCraftingState.from(item(c, ordinary(c, 2, 2)), c).slots());
      var hidden = new AbyssCraftingState.Slot("preserved-overflow", locked, null, true);
      slots.add(hidden);
      var start = new AbyssCraftingState(item(c, List.of()), slots, null);
      start.validate(c);
      for (var action : CHAOS) {
        var result = executor.apply(start, action, Set.of(), new Random(2));
        assertThat(result.applied()).as(action + " " + locked).isTrue();
        assertThat(result.state().slots()).hasSize(5).contains(hidden);
        assertThat(result.state().slots().stream().filter(s -> s.side() == locked))
            .containsExactlyInAnyOrderElementsOf(
                start.slots().stream().filter(s -> s.side() == locked).toList());
      }
    }
  }

  @Test
  void repeatsThousandTimesAfterAnOmenIsConsumedWithoutTouchingTheOverflow() {
    var c = ItemCatalogLoader.loadSapphire();
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var locked : List.of(AffixType.PREFIX, AffixType.SUFFIX)) {
      var before =
          item(
              c,
              ordinary(c, locked == AffixType.PREFIX ? 3 : 2, locked == AffixType.SUFFIX ? 3 : 2));
      String opposite =
          locked == AffixType.PREFIX ? "Omen_of_Dextral_Erasure" : "Omen_of_Sinistral_Erasure";
      for (var action : CHAOS) {
        var outcome =
            new ChaosRepeatRunner(sim::apply)
                .run(before, action, Set.of(opposite), 1000, new Random(13), ignored -> false);
        assertThat(outcome.termination()).isEqualTo(ChaosRepeatRunner.Termination.LIMIT);
        assertThat(outcome.completedUses()).isEqualTo(1000);
        assertThat(outcome.consumedOmens()).containsExactlyEntriesOf(Map.of(opposite, 1));
        assertThat(outcome.state().explicits()).hasSize(5);
        assertThat(side(outcome.state(), c, locked))
            .containsExactlyInAnyOrderElementsOf(side(before, c, locked));
      }
    }
  }

  @Test
  void whittlingChoosesWithinTheUnlockedSideAndAnnulmentStillCanRemoveOverflow() {
    var c = ItemCatalogLoader.loadSapphire();
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var before = item(c, ordinary(c, 3, 2));
    var result =
        sim.apply(before, WorkbenchCurrency.CHAOS, Set.of("Omen_of_Whittling"), new Random(3));
    assertThat(result.applied()).isTrue();
    assertThat(side(result.state(), c, AffixType.PREFIX))
        .containsExactlyInAnyOrderElementsOf(side(before, c, AffixType.PREFIX));
    int lowest =
        side(before, c, AffixType.SUFFIX).stream()
            .mapToInt(m -> c.find(m.modifierId()).orElseThrow().requiredItemLevel())
            .min()
            .orElseThrow();
    assertThat(c.find(result.events().getFirst().modifierId()).orElseThrow().requiredItemLevel())
        .isEqualTo(lowest);
    var annulled =
        sim.apply(
            before,
            WorkbenchCurrency.ANNULMENT,
            Set.of("Omen_of_Sinistral_Annulment"),
            new Random(1));
    assertThat(annulled.applied()).isTrue();
    assertThat(side(annulled.state(), c, AffixType.PREFIX)).hasSize(2);
    assertThat(BasicJewel.chaosLockedSides(annulled.state(), c)).isEmpty();
  }
}
