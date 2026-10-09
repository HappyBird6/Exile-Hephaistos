package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.BasicCurrencyTransitions.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.infrastructure.RulesetManifestLoader;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Real sealed Solar integration cases, separate from the small synthetic oracle. */
class BasicCurrencySolarTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator workbench = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final BasicCurrencyTransitions kernel =
      new BasicCurrencyTransitions(catalog, RulesetManifestLoader.load().identity());

  ItemState input(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    var root = SolarAmulet.initial(catalog);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        rarity,
        root.implicits(),
        mods,
        Set.of());
  }

  ModifierInstance first() {
    var definition =
        catalog.modifiers().values().stream()
            .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
            .findFirst()
            .orElseThrow();
    var range = definition.stats().getFirst();
    return new ModifierInstance(definition.id(), Map.of(range.id(), range.min()));
  }

  @Test
  void sixteenActionsAgreeWithWorkbenchEligibilityRetainProvenanceAndNormalizeExactly() {
    for (var action : BasicCurrencyTransitions.supportedActions()) {
      var rarity =
          switch (action.baseAction()) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var mods =
          action == WorkbenchCurrency.ANNULMENT || action.baseAction() == CraftingAction.CHAOS
              ? List.of(first())
              : List.<ModifierInstance>of();
      var root = input(rarity, mods);
      var source = new BasicCurrencyState(root, kernel.provenance());
      var expanded = kernel.expand(source, action, Set.of(), 100000);
      assertThat(expanded.status()).as(action.name()).isEqualTo(Status.COMPLETE);
      assertThat(
              workbench.actions(root, Set.of()).stream()
                  .filter(a -> a.action() == action)
                  .findFirst()
                  .orElseThrow()
                  .available())
          .isTrue();
      assertThat(
              expanded.outcomes().stream()
                  .map(Outcome::probability)
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      assertThat(expanded.unresolved()).isEqualTo(Fraction.ZERO);
      assertThat(expanded.outcomes())
          .allSatisfy(
              o -> {
                assertThat(o.state().item().snapshotId()).isEqualTo(root.snapshotId());
                assertThat(o.state().item().implicits()).isEqualTo(root.implicits());
                assertThat(o.state().provenance()).isEqualTo(kernel.provenance());
                assertThat(o.state().provenance().rawSha256())
                    .isEqualTo(catalog.metadata().rawSha256());
                assertThat(new ItemStateValidator(catalog).validate(o.state().item())).isEmpty();
              });
    }
  }

  @Test
  void ordinarySixAffixMarginalsMatchLegacyDistributionWithoutLosingNumericStates() {
    var engine = new CraftingEngine(catalog);
    for (var action : CraftingAction.values()) {
      var rarity =
          switch (action) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var mods =
          action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS
              ? List.of(first())
              : List.<ModifierInstance>of();
      var root = input(rarity, mods);
      var full =
          kernel.expand(
              new BasicCurrencyState(root, kernel.provenance()),
              WorkbenchCurrency.valueOf(action.name()),
              Set.of(),
              100000);
      var marginalized = new HashMap<StateBucket, Fraction>();
      full.outcomes()
          .forEach(
              o ->
                  marginalized.merge(
                      StateBucket.from(o.state().item()), o.probability(), Fraction::add));
      var legacy = engine.transition(StateBucket.from(root), action);
      assertThat(marginalized).hasSize(legacy.outcomes().size());
      for (var outcome : legacy.outcomes()) {
        var fraction = marginalized.get(outcome.state());
        assertThat(fraction).isNotNull();
        double probability =
            fraction.numerator().doubleValue() / fraction.denominator().doubleValue();
        assertThat(probability).isCloseTo(outcome.probability(), within(1e-12));
      }
    }
  }

  @Test
  void higherLevelCurrenciesAndFracturedInputsUseExistingRulesRatherThanNewTierRecipes() {
    var normal = SolarAmulet.initial(catalog, 1, 15);
    var low =
        kernel.expand(
            new BasicCurrencyState(normal, kernel.provenance()),
            WorkbenchCurrency.PERFECT_TRANSMUTATION,
            Set.of(),
            100000);
    assertThat(low.status()).isEqualTo(Status.UNAVAILABLE);
    var selected = first();
    var fractured = new ModifierInstance(selected.modifierId(), selected.values(), true);
    var root = input(ItemState.Rarity.RARE, List.of(fractured));
    var blocked =
        kernel.expand(
            new BasicCurrencyState(root, kernel.provenance()),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            100000);
    assertThat(blocked.status()).isEqualTo(Status.UNAVAILABLE);
    var added =
        kernel.expand(
            new BasicCurrencyState(root, kernel.provenance()),
            WorkbenchCurrency.PERFECT_EXALTED,
            Set.of(),
            100000);
    assertThat(added.status()).isEqualTo(Status.COMPLETE);
    assertThat(added.outcomes())
        .allSatisfy(o -> assertThat(o.state().item().explicits()).contains(fractured));
  }

  @Test
  void otherBaseAndSpecialConditionsRemainUnsupportedWithoutChangingExistingWorkbenchSupport() {
    var other = ItemCatalogLoader.loadSapphire();
    var otherKernel = new BasicCurrencyTransitions(other, RulesetManifestLoader.load().identity());
    var otherRoot =
        new ItemState(
            other.metadata().snapshotId(),
            other.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of(),
            0);
    var unsupported =
        otherKernel.expand(
            new BasicCurrencyState(otherRoot, otherKernel.provenance()),
            WorkbenchCurrency.TRANSMUTATION,
            Set.of(),
            100);
    assertThat(unsupported.status()).isEqualTo(Status.UNSUPPORTED);
    assertThat(unsupported.reason()).isEqualTo("NUMERIC_BASE_NOT_IMPLEMENTED");
    assertThat(unsupported.source().item()).isSameAs(otherRoot);
    var root = SolarAmulet.initial(catalog);
    var corrupted =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            root.implicits(),
            root.explicits(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThat(
            kernel
                .expand(
                    new BasicCurrencyState(corrupted, kernel.provenance()),
                    WorkbenchCurrency.TRANSMUTATION,
                    Set.of(),
                    100)
                .status())
        .isEqualTo(Status.UNSUPPORTED);
  }
}
