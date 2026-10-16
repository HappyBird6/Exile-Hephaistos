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
            item -> new AutomaticMethodGraph.Progress(Status.NO_MATCH, List.of(Status.NO_MATCH)),
            CertifiedInterval.Precision.standard(),
            10,
            2,
            1000,
            10)) {
      for (int i = 0; i < 200 && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.resourceLimited()).isTrue();
      assertThat(graph.complete()).isFalse();
      assertThat(graph.finished()).isFalse();
      var first = graph.methods().stream().map(AutomaticMethodGraph.MethodView::id).toList();
      assertThat(first).hasSize(2);
      graph.increaseLimits(20, 50, 10000);
      for (int i = 0; i < 200 && !graph.resourceLimited(); i++) graph.step();
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
                          == AutomaticCurrencyPolicy.Purpose.ORDINARY);
      graph.strategy(100);
    }
  }
}
