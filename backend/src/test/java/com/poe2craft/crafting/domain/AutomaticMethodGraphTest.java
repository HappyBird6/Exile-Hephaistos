package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomaticMethodGraphTest {
  @Test
  void approvedInventoryDiscoversDiamondAndParallelMethodsWithIndependentTinyCatalog() {
    var tiny = new BasicCurrencyTransitionsTest();
    var filler = tiny.definition("filler", ModifierDefinition.AffixType.PREFIX, 1, 1, 2);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, filler));
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            catalog,
            new WorkbenchSimulator(
                catalog,
                new CraftingEngine(catalog),
                Set.of(),
                Map.of(),
                Map.of(
                    WorkbenchCurrency.PERFECT_ESSENCE_MIND,
                    List.of("a"),
                    WorkbenchCurrency.PERFECT_ESSENCE_BODY,
                    List.of("b"))),
            "amulets");
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
          boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
          return new AutomaticMethodGraph.Progress(
              a && b ? Status.MATCH : Status.NO_MATCH,
              List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
        };
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            2000,
            50000,
            100)) {
      for (int i = 0; i < 30000 && !graph.resourceLimited(); i++) {
        graph.step();
        if (i % 100 == 0) graph.publishProgress();
        if (graph.nodes().stream()
            .anyMatch(n -> n.depth() == 2 && n.progress().goal() == Status.MATCH)) break;
      }
      graph.publishProgress();
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.depth() == 1
                      && n.progress().entries().equals(List.of(Status.MATCH, Status.NO_MATCH)));
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.depth() == 1
                      && n.progress().entries().equals(List.of(Status.NO_MATCH, Status.MATCH)));
      assertThat(graph.nodes())
          .anyMatch(n -> n.depth() == 2 && n.progress().goal() == Status.MATCH);
      // A single probabilistic acquisition may branch into both milestone orders;
      // duplicating it as unrelated prep pairs is no longer a planning requirement.
      assertThat(graph.methods().stream().filter(m -> m.source() == 0).toList())
          .anySatisfy(
              m ->
                  assertThat(m.points().get(100).exits().get(ExactMethodRunner.Exit.CHECKPOINT))
                      .hasSizeGreaterThanOrEqualTo(2));
      assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 4);
      assertThat(graph.complete()).isFalse();
    }
  }

  @Test
  void lateShorterMergeUpdatesAlreadyDiscoveredDescendantDepths() {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    var root = tiny.item(ItemState.Rarity.RARE, List.of());
    var a = tiny.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("a", Map.of("a", 1L))));
    var b = tiny.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("b", Map.of("b", 3L))));
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> new AutomaticMethodGraph.Progress(Status.NO_MATCH, List.of(Status.NO_MATCH));
    var action =
        new AutomaticCraftingAction(
            WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.ORDINARY);
    var template =
        new AutomaticMethodExecution.Template(
            "structural-merge-fixture", List.of(), List.of(action));
    var nodes = new ArrayList<AutomaticMethodGraph.NodeCursor>();
    var methods = new ArrayList<AutomaticMethodGraph.MethodCursor>();
    var items = List.of(root, a, b);
    for (int i = 0; i < 3; i++) {
      nodes.add(
          new AutomaticMethodGraph.NodeCursor(
              items.get(i), false, i == 0 ? 0 : i + 4, 76, 0, true, List.of(), Set.of()));
      if (i < 2)
        try (var execution =
            new AutomaticMethodExecution(
                items.get(i),
                template,
                kernel,
                ignored -> Optional.empty(),
                CertifiedInterval.Precision.standard(),
                10)) {
          var one = CertifiedInterval.one(64);
          var point =
              new AutomaticMethodGraph.Point(
                  100,
                  List.of(
                      new AutomaticMethodGraph.Mass(
                          ExactMethodRunner.Exit.CHECKPOINT,
                          new AutomaticMethodExecution.State(items.get(i + 1), false),
                          one)),
                  List.of(),
                  one);
          methods.add(
              new AutomaticMethodGraph.MethodCursor(
                  "edge-" + i,
                  i,
                  execution.checkpoint("goal"),
                  List.of(
                      point,
                      new AutomaticMethodGraph.Point(
                          300,
                          items.stream()
                              .map(
                                  item ->
                                      new AutomaticMethodGraph.Mass(
                                          ExactMethodRunner.Exit.DEAD,
                                          new AutomaticMethodExecution.State(item, false),
                                          CertifiedInterval.enclose(
                                              com.poe2craft.crafting.domain.goalfilter
                                                  .ExactNumericDistribution.Fraction.of(1, 3),
                                              64)))
                              .toList(),
                          List.of(),
                          one)),
                  true));
        }
    }
    var saved =
        new AutomaticMethodGraph.Checkpoint(
            AutomaticMethodGraph.VERSION,
            "goal",
            kernel.identity(),
            CertifiedInterval.Precision.standard(),
            10,
            10,
            1000,
            10,
            0,
            0,
            false,
            false,
            AutomaticCraftingAction.candidates(policy),
            nodes,
            methods,
            List.of());
    try (var graph = AutomaticMethodGraph.restore(saved, "goal", policy, kernel, evaluate)) {
      graph.increaseLimits(10, 10, 1000);
      assertThat(graph.nodes())
          .extracting(AutomaticMethodGraph.NodeView::depth)
          .containsExactly(0, 1, 2);
      assertThatCode(() -> graph.strategy(300)).doesNotThrowAnyException();
    }
  }

  @Test
  void graphCursorRoundTripsWithoutLosingQueuedDiscovery() throws Exception {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> new AutomaticMethodGraph.Progress(Status.NO_MATCH, List.of(Status.NO_MATCH));
    try (var original =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            10,
            4,
            1000,
            10)) {
      for (int i = 0; i < 20; i++) original.step();
      var json = new com.fasterxml.jackson.databind.ObjectMapper();
      var saved =
          json.readValue(
              json.writeValueAsBytes(original.checkpoint("goal")),
              AutomaticMethodGraph.Checkpoint.class);
      try (var restored = AutomaticMethodGraph.restore(saved, "goal", policy, kernel, evaluate)) {
        for (var graph : List.of(original, restored)) {
          graph.increaseLimits(20, 50, 10000);
          for (int i = 0; i < 100; i++) graph.step();
        }
        assertThat(restored.checkpoint("goal")).isEqualTo(original.checkpoint("goal"));
      }
      assertThatThrownBy(
              () -> AutomaticMethodGraph.restore(saved, "other-goal", policy, kernel, evaluate))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void resourcePauseRetainsUnfinishedDiscoveryAndResumesTheSameCandidate() {
    var tiny = new BasicCurrencyTransitionsTest();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            tiny.catalog,
            new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
            "amulets");
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            item -> {
              boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
              boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
              return new AutomaticMethodGraph.Progress(
                  a && b ? Status.MATCH : Status.NO_MATCH,
                  List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
            },
            CertifiedInterval.Precision.standard(),
            10,
            2,
            1000,
            10)) {
      for (int i = 0; i < 20000 && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.resourceLimited()).isTrue();
      assertThat(graph.complete()).isFalse();
      assertThat(graph.finished()).isFalse();
      var first = graph.methods().stream().map(AutomaticMethodGraph.MethodView::id).toList();
      assertThat(first).hasSize(2);
      graph.increaseLimits(20, 50, 10000);
      for (int i = 0; i < 20000 && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.methods())
          .extracting(AutomaticMethodGraph.MethodView::id)
          .containsAll(first)
          .doesNotHaveDuplicates();
      assertThat(graph.methods().size()).isGreaterThan(first.size());
      assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 4);
      assertThat(graph.methods())
          .allMatch(
              m ->
                  m.template().initial().size() > 0
                      || m.template().cycle().size() > 1
                      || m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.ORDINARY
                      || m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().isEmpty()
                      && m.template().cycle().size() == 1
                      && m.template().cycle().getFirst().purpose()
                          == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL);
      graph.strategy(100);
    }
  }
}
