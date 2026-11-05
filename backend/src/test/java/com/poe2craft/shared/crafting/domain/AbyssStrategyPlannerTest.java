package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.AbyssStrategyPlanner;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph.Progress;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import org.junit.jupiter.api.Test;

class AbyssStrategyPlannerTest {
  final HysteriaEssenceTest fixture = new HysteriaEssenceTest();

  Progress progress(ItemState item) {
    var ids = item.explicits().stream().map(ModifierInstance::modifierId).toList();
    var entries =
        List.of("amulet:prefix:hale", "amulet:prefix:adept-s", "amulet:suffix:of-the-brute")
            .stream()
            .map(id -> ids.contains(id) ? Status.MATCH : Status.NO_MATCH)
            .toList();
    return new Progress(
        entries.contains(Status.NO_MATCH) ? Status.NO_MATCH : Status.MATCH, entries);
  }

  AutomaticCraftingAction action(WorkbenchCurrency currency) {
    return new AutomaticCraftingAction(
        currency, Set.of(), AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION);
  }

  @Test
  void preparedRoutesPreserveExistingModifiersAndCompleteTheUsefulSide() {
    var source = fixture.rare(82, "", "amulet:prefix:hale", "amulet:prefix:adept-s");
    var planner = new AbyssStrategyPlanner(fixture.catalog, fixture.simulator, this::progress);
    var routes =
        planner.recommend(
            source,
            List.of(action(WorkbenchCurrency.EXALTED), action(WorkbenchCurrency.ESSENCE_HYSTERIA)));
    assertThat(routes).anyMatch(r -> r.kind() == AbyssStrategyPlanner.Kind.COMPLETE_SIDE);
    assertThat(routes).anyMatch(r -> r.kind() == AbyssStrategyPlanner.Kind.SHARE_REMOVAL_RISK);
    for (var route : routes) {
      assertThat(route.prepared().knownItem()).isEqualTo(source);
      assertThat(route.prepared().slots()).hasSize(3);
      assertThat(route.prepared().offer()).isNull();
      var protection = route.removalProtection();
      if (protection != null)
        assertThat((double) protection.safeAfter() / protection.totalAfter())
            .isGreaterThan((double) protection.safeBefore() / protection.totalBefore());
    }
  }

  @Test
  void noRouteIsInventedWithoutPermittedNextActionsOrForCompletedGoals() {
    var source = fixture.rare(82, "", "amulet:prefix:hale", "amulet:prefix:adept-s");
    assertThat(
            new AbyssStrategyPlanner(fixture.catalog, fixture.simulator, this::progress)
                .recommend(source, List.of()))
        .isEmpty();
    assertThat(
            new AbyssStrategyPlanner(
                    fixture.catalog,
                    fixture.simulator,
                    i -> new Progress(Status.MATCH, List.of(Status.MATCH)))
                .recommend(source, List.of(action(WorkbenchCurrency.EXALTED))))
        .isEmpty();
  }

  @Test
  void graphRoutesKeepFamilyAndOmenPolicyAndExposeAllThreeStrategies() {
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel = new AutomaticCraftingKernel(policy, fixture.catalog, fixture.simulator, "amulets");
    var source = fixture.rare(82, "", "amulet:prefix:hale", "amulet:prefix:adept-s");
    var routes =
        kernel.abyssStrategies(source, this::progress, AutomaticCraftingAction.candidates(policy));
    assertThat(routes)
        .extracting(AbyssStrategyPlanner.Strategy::kind)
        .contains(
            AbyssStrategyPlanner.Kind.COMPLETE_SIDE,
            AbyssStrategyPlanner.Kind.BLOCK_HIGH_WEIGHT,
            AbyssStrategyPlanner.Kind.SHARE_REMOVAL_RISK);
    assertThat(routes)
        .noneMatch(
            r ->
                r.nextCurrency() == WorkbenchCurrency.ESSENCE_HYSTERIA
                    || r.nextCurrency() == WorkbenchCurrency.RUNIC_ALLOY
                    || r.nextCurrency() == WorkbenchCurrency.CHAOS);
  }
}
