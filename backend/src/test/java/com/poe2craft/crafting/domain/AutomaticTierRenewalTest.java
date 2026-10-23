package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticTierRenewalTest {
  @Test
  void oneHundredFirstHitLawMatchesIndependentChangingStateMarkovOracle() {
    var tiny = new BasicCurrencyTransitionsTest();
    var definitions = new ArrayList<ModifierDefinition>();
    definitions.add(tiny.implicit);
    for (String stat : List.of("a", "b"))
      for (int tier = 1; tier <= (stat.equals("a") ? 4 : 3); tier++)
        definitions.add(
            new ModifierDefinition(
                stat + tier,
                stat + tier,
                ModifierDefinition.Layer.EXPLICIT,
                stat.equals("a")
                    ? ModifierDefinition.AffixType.PREFIX
                    : ModifierDefinition.AffixType.SUFFIX,
                Set.of(stat),
                1,
                tier,
                tier,
                stat,
                List.of(new ModifierDefinition.StatRange(stat, tier, tier)),
                Set.of(),
                "https://example.test/" + stat + tier));
    var catalog = tiny.catalog(definitions);
    var root =
        tiny.item(
            ItemState.Rarity.RARE,
            List.of(
                new ModifierInstance("a1", Map.of("a", 1L)),
                new ModifierInstance("b1", Map.of("b", 1L))));
    var known = new HashMap<String, GoalCatalog.Stat>();
    for (String stat : List.of("a", "b"))
      known.put(
          stat,
          new GoalCatalog.Stat(
              stat,
              stat,
              "flat",
              GoalCatalog.Kind.EXPLICIT,
              new GoalCatalog.Support(
                  GoalFilter.Capability.SUPPORTED, GoalFilter.Capability.SUPPORTED, null),
              true,
              null,
              List.of(stat),
              List.of(),
              List.of()));
    var entries =
        List.of("a", "b").stream()
            .map(
                stat ->
                    new GoalFilter.Entry(
                        stat,
                        stat,
                        "flat",
                        new GoalFilter.Range(BigDecimal.valueOf(stat.equals("a") ? 4 : 3), null),
                        null,
                        false))
            .toList();
    var goal =
        new GoalFilter(
            1,
            "test",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(new GoalFilter.Group("g", GoalFilter.Type.AND, false, null, entries)));
    var policy = AutomaticCurrencyPolicyLoader.load();
    var scalar =
        new AutomaticCraftingKernel(
            policy,
            catalog,
            new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
            "amulets");
    var grouped =
        scalar.withRollPartition(
            RollPartition.compile(catalog, known, goal, root), goal.toString());
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    java.util.function.Predicate<ItemState> hit =
        item ->
            item.explicits().stream().anyMatch(m -> m.values().getOrDefault("a", 0L) >= 4)
                && item.explicits().stream().anyMatch(m -> m.values().getOrDefault("b", 0L) >= 3);
    var active = new LinkedHashMap<ItemState, Fraction>();
    active.put(root, Fraction.ONE);
    var firstHit = Fraction.ZERO;
    for (int attempt = 0; attempt < 100; attempt++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var source : active.entrySet()) {
        var law = scalar.expand(source.getKey(), action, 100);
        assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
        for (var outcome : law.outcomes()) {
          var mass = source.getValue().multiply(outcome.probability());
          if (hit.test(outcome.item())) firstHit = firstHit.add(mass);
          else next.merge(outcome.item(), mass, Fraction::add);
        }
      }
      active = next;
    }
    assertThat(firstHit).isNotEqualTo(Fraction.ZERO);
    try (var execution =
        new AutomaticMethodExecution(
            root,
            new AutomaticMethodExecution.Template("changing-chaos", List.of(), List.of(action)),
            grouped,
            item -> hit.test(item) ? Optional.of(Exit.GOAL) : Optional.empty(),
            CertifiedInterval.Precision.standard(),
            100)) {
      for (int attempt = 0; attempt < 100; attempt++) {
        if (execution.atBoundary()) execution.beginCycle();
        for (int step = 0; !execution.atBoundary() && step < 10000; step++) execution.step(10000);
        assertThat(execution.atBoundary()).isTrue();
      }
      var observation = execution.observation();
      var observed =
          observation.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(observed.contains(firstHit)).isTrue();
      assertThat(observation.mass().contains(Fraction.ONE)).isTrue();
      assertThat(execution.checkpoint("test").rows())
          .anyMatch(row -> row.state() != null && !row.state().tiers().isEmpty());
      System.out.println("TIER_CHAOS_100|exact=" + firstHit + "|bounds=" + observed);
    }
  }
}
