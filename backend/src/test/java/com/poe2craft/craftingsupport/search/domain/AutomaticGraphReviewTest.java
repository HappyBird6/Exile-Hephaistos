package com.poe2craft.craftingsupport.search.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AutomaticGraphReviewTest {
  final BasicCurrencyFixture f = new BasicCurrencyFixture();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  final CertifiedInterval.Precision precision = CertifiedInterval.Precision.standard();
  final AutomaticCraftingAction exalt =
      new AutomaticCraftingAction(
          WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);

  AutomaticCraftingKernel kernel(ItemCatalog catalog) {
    return new AutomaticCraftingKernel(
        policy, catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)), "amulets");
  }

  AutomaticMethodExecution execution(
      String id,
      ItemState root,
      AutomaticCraftingAction action,
      AutomaticCraftingKernel kernel,
      long budget,
      Function<ItemState, Optional<ExactMethodRunner.Exit>> classify) {
    return new AutomaticMethodExecution(
        root,
        new AutomaticMethodExecution.Template(id, List.of(), List.of(action)),
        kernel,
        classify,
        precision,
        budget);
  }

  AutomaticMethodGraph restore(
      AutomaticCraftingKernel kernel,
      List<ItemState> nodes,
      List<AutomaticMethodGraph.MethodCursor> methods,
      List<String> ready,
      long budget,
      RareOutcomePolicy rare,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate) {
    try (var empty =
        new AutomaticMethodGraph(
            nodes.getFirst(), policy, kernel, evaluate, precision, 64, 128, 20000, budget)) {
      var c = empty.checkpoint("review");
      var saved =
          new AutomaticMethodGraph.Checkpoint(
              c.version(),
              c.objective(),
              c.identity(),
              c.precision(),
              c.states(),
              c.methods(),
              c.retained(),
              c.primitive(),
              0,
              0,
              false,
              false,
              c.candidates(),
              nodes.stream()
                  .map(
                      n ->
                          new AutomaticMethodGraph.NodeCursor(
                              n, false, nodes.indexOf(n), 0, 0, true, List.of(), Set.of()))
                  .toList(),
              methods,
              ready,
              rare);
      return AutomaticMethodGraph.restore(saved, "review", policy, kernel, evaluate);
    }
  }

  @Test
  void partialAndCompleteUnpublishedBoundsEncloseFinalRarePolicyEvenWithKnownSuccessor() {
    var catalog =
        f.catalog(
            List.of(
                f.implicit,
                f.definition("a", ModifierDefinition.AffixType.PREFIX, 1, 1, 1),
                f.definition("b", ModifierDefinition.AffixType.SUFFIX, 1, 1, 127)));
    var kernel = kernel(catalog);
    var root = f.item(ItemState.Rarity.RARE, List.of());
    var successor =
        f.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("a", Map.of("a", 1L))));
    var goal =
        f.item(
            ItemState.Rarity.RARE,
            List.of(
                new ModifierInstance("a", Map.of("a", 1L)),
                new ModifierInstance("b", Map.of("b", 1L))));
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item ->
            new AutomaticMethodGraph.Progress(
                item.explicits().size() == 2 ? Status.MATCH : Status.NO_MATCH,
                List.of(
                    item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"))
                        ? Status.MATCH
                        : Status.NO_MATCH));
    try (var m =
            execution(
                "M",
                root,
                exalt,
                kernel,
                5000,
                item ->
                    item.explicits().isEmpty()
                        ? Optional.empty()
                        : Optional.of(
                            item.explicits().getFirst().modifierId().equals("a")
                                ? ExactMethodRunner.Exit.CHECKPOINT
                                : ExactMethodRunner.Exit.LOSS));
        var follow =
            execution(
                "S",
                successor,
                exalt,
                kernel,
                5000,
                item ->
                    item.explicits().size() == 2
                        ? Optional.of(ExactMethodRunner.Exit.GOAL)
                        : Optional.empty())) {
      follow.beginCycle();
      while (!follow.atBoundary()) follow.step(64);
      var s =
          new AutomaticMethodGraph.MethodCursor(
              "S",
              1,
              follow.checkpoint("review"),
              List.of(
                  new AutomaticMethodGraph.Point(
                      100,
                      List.of(
                          new AutomaticMethodGraph.Mass(
                              ExactMethodRunner.Exit.GOAL,
                              new AutomaticMethodExecution.State(goal, false),
                              CertifiedInterval.one(64))),
                      List.of(
                          new AutomaticMethodGraph.Use(exalt, CertifiedInterval.one(64), false)),
                      CertifiedInterval.one(64))),
              true);
      m.beginCycle();
      m.step(1);
      assertThat(m.atBoundary()).isFalse();
      assertThat(m.provisional(100).exits().get(ExactMethodRunner.Exit.CHECKPOINT)).isNotEmpty();
      var partial = m.checkpoint("review");
      while (!m.atBoundary()) m.step(64);
      var complete = m.checkpoint("review");
      for (int threshold : List.of(10000, 1000)) {
        var configured = new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, threshold, 50000);
        var expected = threshold == 10000 ? Fraction.ZERO : Fraction.of(1, 128);
        for (var state : List.of(partial, complete)) {
          try (var graph =
              restore(
                  kernel,
                  List.of(root, successor, goal),
                  List.of(
                      new AutomaticMethodGraph.MethodCursor("M", 0, state, List.of(), false), s),
                  List.of("M"),
                  5000,
                  configured,
                  evaluate)) {
            var strategy = graph.strategy(100);
            graph.refineStrategy(strategy, 16);
            assertThat(strategy.result(1).value()).isEqualTo(CertifiedInterval.one(64));
            assertThat(strategy.result(0).value().contains(expected)).isTrue();
            var candidate = graph.policyCandidates(3).getFirst();
            assertThat(candidate.success().contains(expected)).isTrue();
            if (state == partial) {
              assertThat(candidate.unresolved().lowerUnits().signum()).isPositive();
              assertThat(candidate.rebase().lowerUnits().signum()).isZero();
            } else {
              assertThat(candidate.success()).isEqualTo(CertifiedInterval.enclose(expected, 64));
              assertThat(candidate.rebase())
                  .isEqualTo(
                      CertifiedInterval.enclose(
                          threshold == 10000 ? Fraction.of(1, 128) : Fraction.ZERO, 64));
              graph.step(); // publish the previously complete law
              assertThat(graph.policyCandidates(3).getFirst().success())
                  .isEqualTo(candidate.success());
            }
          }
        }
      }
    }
  }

  @Test
  void twoLimitedCursorsDoNotBlockSmallMethodAndResumeWithoutDoubleCharging() {
    var catalog =
        f.catalog(
            List.of(
                f.implicit,
                f.definition("wide", ModifierDefinition.AffixType.PREFIX, 1, 256, 1),
                f.definition("small", ModifierDefinition.AffixType.SUFFIX, 1, 1, 1)));
    var kernel = kernel(catalog);
    var root = f.item(ItemState.Rarity.RARE, List.of());
    var small =
        new AutomaticCraftingAction(
            WorkbenchCurrency.EXALTED,
            Set.of(WorkbenchOmen.DEXTRAL_EXALTATION),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item ->
            new AutomaticMethodGraph.Progress(
                item.explicits().isEmpty() ? Status.NO_MATCH : Status.MATCH, List.of());
    var rows = new ArrayList<AutomaticMethodGraph.MethodCursor>();
    for (var id : List.of("A", "B", "C"))
      try (var e =
          execution(
              id, root, id.equals("C") ? small : exalt, kernel, 1, item -> Optional.empty())) {
        rows.add(
            new AutomaticMethodGraph.MethodCursor(id, 0, e.checkpoint("review"), List.of(), false));
      }
    try (var graph =
        restore(
            kernel,
            List.of(root),
            rows,
            List.of("A", "B", "C"),
            1,
            RareOutcomePolicy.disabled(),
            evaluate)) {
      for (int i = 0; i < 40 && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.resourceLimited()).isTrue();
      var c = graph.methods().stream().filter(m -> m.id().equals("C")).findFirst().orElseThrow();
      assertThat(c.finished()).isTrue();
      assertThat(c.points().get(100).cycleUses().get(small)).isEqualTo(CertifiedInterval.one(64));
      assertThat(graph.resources().blockers()).contains("PRIMITIVE_OUTCOMES");
      assertThat(graph.canResume()).isTrue();
      graph.increaseLimits(64, 128, 20000, 1000);
      for (int i = 0; i < 500 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.finished()).isTrue();
      for (var method : graph.methods()) {
        assertThat(method.points().get(100).cycleUses().values())
            .containsExactly(CertifiedInterval.one(64));
        assertThat(method.points().get(100).mass()).isEqualTo(CertifiedInterval.one(64));
      }
    }
  }
}
