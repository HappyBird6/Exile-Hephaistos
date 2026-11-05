package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalog;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.application.AutomaticKernelFactory;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.CoupledStatRollModel;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReviewedRingGoalTest {
  @Test
  void reviewedRingUsesItsExistingDeclaredJointModelAndConservesTheFullEligibleDenominator() {
    var cfg = new CatalogConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    var workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    var initial = workbench.initial("ring", 82).state();
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(),
            Set.of());
    var kernel =
        new AutomaticKernelFactory(workbench).create(item, AutomaticCurrencyPolicyLoader.load());
    var result =
        kernel.expandDeferred(
            item,
            Map.of(),
            new AutomaticCraftingAction(
                WorkbenchCurrency.EXALTED,
                Set.of(),
                AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL),
            5000);
    assertThat(result.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
    assertThat(result.outcomes())
        .anySatisfy(o -> assertThat(o.item().explicits().getFirst().values()).hasSize(2));
    assertThat(result.assumptions())
        .anySatisfy(
            a -> {
              assertThat(a.id()).isEqualTo(CoupledStatRollModel.ID);
              assertThat(a.reason()).contains("Unverified user conjecture");
            });
  }

  @Test
  void reviewedDirectRingStatsEvaluateTheirActualSourceValuesWithoutDroppingHybridComponents() {
    var cfg = new CatalogConfiguration();
    var solar = cfg.itemCatalog(cfg.workbenchDefinitions());
    var workbench =
        cfg.workbenchService(solar, cfg.workbenchSimulator(solar, cfg.craftingEngine(solar)));
    var service =
        new GoalFilterService(
            new GoalCatalogIndex(
                BundledGoalCatalogs.load(workbench), GoalDefinitionsLoader.load()));
    var initial = workbench.initial("ring", 82);
    var root = initial.state();
    var catalog =
        service.catalog(new Context(root.snapshotId(), root.baseItemId(), root.itemLevel()));
    var deferred =
        Set.of(
            "base_life_regeneration_rate_per_minute",
            "base_life_leech_from_physical_attack_damage_permyriad",
            "base_mana_leech_from_physical_attack_damage_permyriad");
    for (var raw : deferred) {
      var stat =
          catalog.stats().stream()
              .filter(s -> s.sourceStatIds().equals(List.of(raw)))
              .findFirst()
              .orElseThrow();
      assertThat(stat.support().evaluation()).isEqualTo(Capability.SUPPORTED);
    }
    int checked = 0;
    for (var stat : catalog.stats()) {
      if (stat.kind() != GoalCatalog.Kind.EXPLICIT
          || stat.support().evaluation() != Capability.SUPPORTED) continue;
      String raw = stat.sourceStatIds().getFirst();
      var definition =
          initial.modifiers().values().stream()
              .filter(
                  d ->
                      d.layer() == ModifierDefinition.Layer.EXPLICIT
                          && d.stats().stream().anyMatch(s -> s.id().equals(raw)))
              .findFirst()
              .orElseThrow();
      var values = new TreeMap<String, Long>();
      definition.stats().forEach(s -> values.put(s.id(), s.min()));
      var item =
          new ItemState(
              root.snapshotId(),
              root.baseItemId(),
              root.itemLevel(),
              ItemState.Rarity.RARE,
              root.implicits(),
              List.of(new ModifierInstance(definition.id(), values)),
              Set.of());
      var number =
          BigDecimal.valueOf(values.get(raw))
              .divide(BigDecimal.valueOf(stat.sourceDivisor() == null ? 1 : stat.sourceDivisor()));
      var goal =
          new GoalFilter(
              1,
              catalog.catalogVersion(),
              new General(root.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
              List.of(
                  new Group(
                      "g",
                      Type.AND,
                      false,
                      null,
                      List.of(
                          new Entry(
                              "e",
                              stat.statId(),
                              stat.unit(),
                              new Range(number, number),
                              null,
                              false)))));
      var result = service.evaluate(item, goal);
      assertThat(result.status()).as(raw).isEqualTo(Status.MATCH);
      assertThat(result.groups().getFirst().entries().getFirst().value())
          .isEqualByComparingTo(number);
      checked++;
    }
    assertThat(checked).isGreaterThanOrEqualTo(30);
  }
}
