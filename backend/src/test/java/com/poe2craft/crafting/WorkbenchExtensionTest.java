package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorkbenchExtensionTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  private ItemState root(int level) {
    return SolarAmulet.initial(catalog, level, 15);
  }

  private ItemState item(ItemState.Rarity rarity, String... families) {
    var initial = root(82);
    var mods =
        Arrays.stream(families)
            .map(
                f ->
                    catalog.modifiers().values().stream()
                        .filter(
                            d ->
                                d.layer() == ModifierDefinition.Layer.EXPLICIT
                                    && d.familyIds().contains(f))
                        .min(Comparator.comparingInt(ModifierDefinition::requiredItemLevel))
                        .orElseThrow())
            .map(
                d ->
                    new ModifierInstance(
                        d.id(), Map.of(d.stats().getFirst().id(), d.stats().getFirst().min())))
            .toList();
    return new ItemState(
        initial.snapshotId(),
        initial.baseItemId(),
        82,
        rarity,
        initial.implicits(),
        mods,
        Set.of());
  }

  @Test
  void minimumLevelKeepsTheHighestEligibleTypeInsteadOfDroppingSpiritAndRarity() {
    var normal = root(82);
    var magic =
        new ItemState(
            normal.snapshotId(),
            normal.baseItemId(),
            82,
            ItemState.Rarity.MAGIC,
            normal.implicits(),
            List.of(),
            Set.of());
    var pool = simulator.pool(magic, WorkbenchCurrency.PERFECT_TRANSMUTATION, null);
    assertThat(pool)
        .anyMatch(d -> d.familyIds().contains("BaseSpirit") && d.requiredItemLevel() == 54);
    assertThat(pool)
        .noneMatch(d -> d.familyIds().contains("BaseSpirit") && d.requiredItemLevel() < 54);
    assertThat(pool)
        .anyMatch(
            d -> d.familyIds().contains("ItemFoundRarityIncrease") && d.requiredItemLevel() == 40);
    assertThat(pool).allMatch(d -> d.requiredItemLevel() <= 82);
    for (var d : pool)
      if (d.requiredItemLevel() < 70) {
        assertThat(
                pool.stream()
                    .filter(
                        other ->
                            other.familyIds().equals(d.familyIds())
                                && other.affixType() == d.affixType()))
            .allMatch(other -> other.requiredItemLevel() == d.requiredItemLevel());
      }
  }

  @Test
  void allTenTieredCurrenciesRespectTheItemLevelFloorAndProduceValidStates() {
    for (var currency : WorkbenchCurrency.values()) {
      if (currency.minimumModifierLevel() == 0) continue;
      var low = root(currency.minimumModifierLevel() - 1);
      assertThat(simulator.apply(low, currency, Set.of(), new Random(1)).state()).isEqualTo(low);
      assertThat(simulator.apply(low, currency, Set.of(), new Random(1)).applied()).isFalse();
      var start =
          currency.baseAction() == CraftingAction.TRANSMUTATION
              ? root(82)
              : item(
                  currency.baseAction() == CraftingAction.AUGMENTATION
                          || currency.baseAction() == CraftingAction.REGAL
                      ? ItemState.Rarity.MAGIC
                      : ItemState.Rarity.RARE,
                  "IncreasedLife");
      var result = simulator.apply(start, currency, Set.of(), new Random(11));
      assertThat(result.applied()).isTrue();
      assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
    }
  }

  @Test
  void affixOmensRestrictTheCorrectOperationAndConsumeOnlyTheMatchingOmen() {
    var rare = item(ItemState.Rarity.RARE, "IncreasedLife", "IncreasedCastSpeed");
    for (var omen :
        List.of(
            WorkbenchOmen.SINISTRAL_EXALTATION,
            WorkbenchOmen.DEXTRAL_EXALTATION,
            WorkbenchOmen.SINISTRAL_ANNULMENT,
            WorkbenchOmen.DEXTRAL_ANNULMENT,
            WorkbenchOmen.SINISTRAL_ERASURE,
            WorkbenchOmen.DEXTRAL_ERASURE)) {
      var result =
          simulator.apply(
              rare, omen.trigger(), Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random(12));
      assertThat(result.applied()).isTrue();
      var event = result.events().getFirst();
      assertThat(catalog.find(event.modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      assertThat(result.consumedOmens()).containsExactly(omen.id());
      assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
      if (omen.trigger() == WorkbenchCurrency.CHAOS)
        assertThat(result.state().explicits()).hasSize(rare.explicits().size());
    }
  }

  @Test
  void conflictingAndUnverifiedCombinationsAreAtomicAndDoNotConsumeOmens() {
    var rare = item(ItemState.Rarity.RARE, "IncreasedLife", "IncreasedCastSpeed");
    var conflicting = Set.of(WorkbenchOmen.SINISTRAL_ERASURE.id(), WorkbenchOmen.WHITTLING.id());
    var result = simulator.apply(rare, WorkbenchCurrency.CHAOS, conflicting, new Random(1));
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(rare);
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyInAnyOrderElementsOf(conflicting);
    var tiered =
        simulator.apply(
            rare,
            WorkbenchCurrency.PERFECT_EXALTED,
            Set.of(WorkbenchOmen.SINISTRAL_EXALTATION.id()),
            new Random(1));
    assertThat(tiered.applied()).isFalse();
    assertThat(tiered.state()).isEqualTo(rare);
    var onlyPrefix = item(ItemState.Rarity.RARE, "IncreasedLife");
    var absent =
        simulator.apply(
            onlyPrefix,
            WorkbenchCurrency.ANNULMENT,
            Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT.id()),
            new Random(1));
    assertThat(absent.applied()).isFalse();
    assertThat(absent.consumedOmens()).isEmpty();
    assertThat(absent.state()).isEqualTo(onlyPrefix);
  }

  @Test
  void whittlingUsesModifierLevelRatherThanComparingTierNumbers() {
    var rare = item(ItemState.Rarity.RARE, "BaseSpirit", "IncreasedLife", "IncreasedCastSpeed");
    int lowest =
        rare.explicits().stream()
            .mapToInt(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel())
            .min()
            .orElseThrow();
    for (int seed = 0; seed < 20; seed++) {
      var result =
          simulator.apply(
              rare,
              WorkbenchCurrency.CHAOS,
              Set.of(WorkbenchOmen.WHITTLING.id()),
              new Random(seed));
      assertThat(
              catalog
                  .find(result.events().getFirst().modifierId())
                  .orElseThrow()
                  .requiredItemLevel())
          .isEqualTo(lowest);
      assertThat(result.assumptions().getFirst().n())
          .isEqualTo(
              rare.explicits().stream()
                  .filter(
                      m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel() == lowest)
                  .count());
    }
  }

  @Test
  void divinePreservesIdsAndBlessedPreservesAllExplicitValues() {
    var rare = item(ItemState.Rarity.RARE, "IncreasedLife", "IncreasedCastSpeed");
    var maximum =
        new Random() {
          @Override
          public long nextLong(long n) {
            return n - 1;
          }
        };
    var result = simulator.apply(rare, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactlyElementsOf(
            rare.explicits().stream().map(ModifierInstance::modifierId).toList());
    for (var m : result.state().explicits())
      assertThat(m.values().values())
          .containsExactly(catalog.find(m.modifierId()).orElseThrow().stats().getFirst().max());
    var blessed =
        simulator.apply(
            rare, WorkbenchCurrency.DIVINE, Set.of(WorkbenchOmen.BLESSED.id()), maximum);
    assertThat(blessed.state().explicits()).isEqualTo(rare.explicits());
    assertThat(blessed.events()).allMatch(e -> e.kind().equals("REROLL_IMPLICIT"));
    assertThat(blessed.consumedOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
  }
}
