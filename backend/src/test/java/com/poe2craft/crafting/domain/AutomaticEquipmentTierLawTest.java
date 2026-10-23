package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticEquipmentTierLawTest {
  @Test
  void ringTiersWithDifferentCreationEligibilityShareOneAttributeAndOneFutureState() {
    var tiny = new BasicCurrencyTransitionsTest();
    var low =
        new ModifierDefinition(
            "a-low",
            "low",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("a"),
            1,
            1,
            2,
            "a",
            List.of(new ModifierDefinition.StatRange("a", 1, 1)),
            Set.of(),
            "https://example.test/low");
    var high =
        new ModifierDefinition(
            "a-high",
            "high",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("a"),
            60,
            3,
            1,
            "a",
            List.of(new ModifierDefinition.StatRange("a", 4, 4)),
            Set.of(),
            "https://example.test/high");
    var definitions = List.of(tiny.implicit, low, high, tiny.b);
    var solar = tiny.catalog(definitions);
    var catalog =
        new ItemCatalog(
            solar.metadata(),
            new ItemCatalog.BaseItem(
                RingEssenceTargets.BASE_ID,
                "Synthetic ring",
                "https://example.test/ring",
                "implicit",
                1,
                1,
                3,
                3),
            definitions);
    var root =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(new ModifierInstance("implicit", Map.of("spirit", 5L))),
            List.of(),
            Set.of());
    var known = new HashMap<String, GoalCatalog.Stat>();
    for (var stat : List.of("a", "b"))
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
    var goal =
        new GoalFilter(
            1,
            "test",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group(
                    "g",
                    GoalFilter.Type.AND,
                    false,
                    null,
                    List.of("a", "b").stream()
                        .map(
                            stat ->
                                new GoalFilter.Entry(
                                    stat,
                                    stat,
                                    "flat",
                                    new GoalFilter.Range(BigDecimal.ONE, null),
                                    null,
                                    false))
                        .toList())));
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
                policy,
                catalog,
                new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
                "rings")
            .withRollPartition(RollPartition.compile(catalog, known, goal, root), goal.toString());
    var ordinary =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    var law = kernel.expandDeferred(root, Map.of(), ordinary, 5000);
    var a =
        law.outcomes().stream()
            .filter(o -> o.item().explicits().getFirst().values().containsKey("a"))
            .findFirst()
            .orElseThrow();
    assertThat(a.tiers()).hasSize(1);
    var cell = a.tiers().values().iterator().next();
    assertThat(cell.variants()).hasSize(2);
    assertThat(cell.variants().stream().map(TierCell.Variant::probability))
        .containsExactlyInAnyOrder(Fraction.of(1, 4), Fraction.of(3, 4));
    var greater =
        new AutomaticCraftingAction(
            WorkbenchCurrency.GREATER_EXALTED,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    // Greater currency is deliberately excluded from automatic exploration; its
    // existing finite pool still demonstrates a different creation eligibility.
    var greaterPool =
        new AdditionRules(catalog).pool(StateBucket.from(root), greater.currency(), null);
    assertThat(greaterPool)
        .extracting(ModifierDefinition::id)
        .doesNotContain("a-low")
        .contains("a-high");
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean av = item.explicits().stream().anyMatch(m -> m.values().containsKey("a")),
              bv = item.explicits().stream().anyMatch(m -> m.values().containsKey("b"));
          return new AutomaticMethodGraph.Progress(
              av && bv ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
              List.of(
                  av ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
                  bv ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH));
        };
    try (var graph =
        new AutomaticMethodGraph(
            root,
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      for (int i = 0; i < 10000 && !graph.finished() && !graph.resourceLimited(); i++) {
        graph.step();
        if (i % 100 == 0) graph.publishProgress();
      }
      graph.publishProgress();
      assertThat(graph.complete()).isTrue();
      var inputs =
          graph.nodes().stream()
              .filter(
                  n ->
                      n.item().explicits().size() == 1
                          && n.item().explicits().getFirst().values().containsKey("a"))
              .toList();
      assertThat(inputs).hasSize(1);
      assertThat(inputs.getFirst().members()).hasSize(1);
      assertThat(inputs.getFirst().tiers().values().iterator().next().variants()).hasSize(2);
      assertThat(inputs.getFirst().attributes().values().iterator().next())
          .extracting(AutomaticMethodGraph.AttributeOption::modifierId)
          .containsExactlyInAnyOrder("a-low", "a-high");
      System.out.println(
          "JOINT_RING|nodes="
              + graph.nodes().size()
              + "|aStates="
              + inputs.size()
              + "|aConditionalInputs="
              + inputs.getFirst().members().size());
    }
  }
}
