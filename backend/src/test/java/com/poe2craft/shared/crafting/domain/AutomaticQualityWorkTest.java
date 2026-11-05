package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodExecution;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticQualityWorkTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          AutomaticCurrencyPolicyLoader.load(),
          catalog,
          new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
          "amulets");
  final ItemState initial = SolarAmulet.initial(catalog, 82, 15);
  final ItemState root =
      new ItemState(
          initial.snapshotId(),
          initial.baseItemId(),
          82,
          ItemState.Rarity.RARE,
          initial.implicits(),
          List.of(
              new ModifierInstance(
                  QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))),
          Set.of());

  @Test
  void abstractQualityWorkDoesNotConsumeASelectedCatalystOrRequireCapRemoval() {
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.ESSENCE_BREACH,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT);
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template("quality", List.of(), List.of(action)),
            kernel,
            item -> Optional.of(Exit.GOAL),
            CertifiedInterval.Precision.standard(),
            100)) {
      assertThat(execution.absorbed()).isFalse();
      execution.beginCycle();
      while (!execution.atBoundary()) execution.step(1);
      var point = execution.observation();
      assertThat(point.exits().get(Exit.GOAL)).hasSize(1);
      var state = point.exits().get(Exit.GOAL).keySet().iterator().next();
      assertThat(state.quality40()).isTrue();
      assertThat(AutomaticCraftingKernel.hasQualityCap(state.item())).isTrue();
      assertThat(state.item().catalystQuality()).isNull();
      assertThat(AutomaticMethodExecution.materialUses(point.cycleUses()))
          .containsOnlyKeys("WORK:QUALITY_40");
      assertThat(point.mass().contains(Fraction.ONE)).isTrue();
    }
  }

  @Test
  void removingCapBeforeWorkIsUnresolvedButAfterWorkPreservesTheCheckpoint() {
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    for (boolean complete : List.of(false, true))
      try (var execution =
          new AutomaticMethodExecution(
              root,
              new AutomaticMethodExecution.Template("remove", List.of(), List.of(action)),
              kernel,
              item ->
                  AutomaticCraftingKernel.hasQualityCap(item)
                      ? Optional.empty()
                      : Optional.of(Exit.GOAL),
              CertifiedInterval.Precision.standard(),
              100,
              AutomaticMethodExecution.PreparationSuccess.UNDECIDED,
              complete)) {
        execution.beginCycle();
        while (!execution.atBoundary()) execution.step(1);
        var point = execution.observation();
        var kind = complete ? Exit.GOAL : Exit.UNRESOLVED;
        assertThat(
                point.exits().get(kind).values().stream()
                    .reduce(CertifiedInterval.zero(64), CertifiedInterval::add)
                    .contains(Fraction.ONE))
            .isTrue();
        if (complete)
          assertThat(point.exits().get(Exit.GOAL).keySet())
              .allMatch(AutomaticMethodExecution.State::quality40);
        else assertThat(execution.unresolvedReasons()).contains("QUALITY_CAP_REMOVAL_BEFORE_40");
      }
  }
}
