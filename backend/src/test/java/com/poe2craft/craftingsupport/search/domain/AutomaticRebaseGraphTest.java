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

/**
 * 자동·기준변경·그래프 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code completedConditionalResultsEndBeforeSuccessorDiscoveryAndRoundTrip()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class AutomaticRebaseGraphTest {
  @Test
  void completedConditionalResultsEndBeforeSuccessorDiscoveryAndRoundTrip() {
    var f = new BasicCurrencyFixture();
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel =
        new AutomaticCraftingKernel(
            policy,
            f.catalog,
            new WorkbenchSimulator(f.catalog, new CraftingEngine(f.catalog)),
            "amulets");
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
        item -> {
          boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
          boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
          return new AutomaticMethodGraph.Progress(
              a && b ? Status.MATCH : Status.NO_MATCH,
              List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
        };
    var rare = new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, 1_000_000, 1_000_000);
    try (var graph =
        new AutomaticMethodGraph(
            f.item(ItemState.Rarity.RARE, List.of()),
            policy,
            kernel,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      graph.configureRareOutcomes(rare);
      for (int i = 0; i < 5000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.methods())
          .anySatisfy(
              m ->
                  assertThat(m.points().get(100).exits().get(ExactMethodRunner.Exit.REBASE))
                      .isNotEmpty());
      assertThat(graph.nodes()).allMatch(n -> n.id() == 0 || n.progress().goal() == Status.MATCH);
      graph.refineStrategy(graph.strategy(100), 16);
      assertThat(graph.policyCandidates(3))
          .anySatisfy(
              c -> {
                assertThat(c.rebase().lowerUnits().signum()).isPositive();
                assertThat(c.uses()).isNotEmpty();
              });
      var saved = graph.checkpoint("both");
      try (var restored = AutomaticMethodGraph.restore(saved, "both", policy, kernel, evaluate)) {
        assertThat(restored.rareOutcomePolicy()).isEqualTo(rare);
        assertThat(restored.methods()).isEqualTo(graph.methods());
        assertThat(restored.strategy(100).result(0).value())
            .isEqualTo(graph.strategy(100).result(0).value());
      }
      for (var m : graph.methods())
        for (var point : m.points().values()) {
          var mass = CertifiedInterval.zero(64);
          for (var outcome : AutomaticMethodGraph.encode(point).outcomes())
            mass = mass.add(outcome.probability());
          assertThat(mass.contains(Fraction.ONE)).isTrue();
        }
    }
  }
}
