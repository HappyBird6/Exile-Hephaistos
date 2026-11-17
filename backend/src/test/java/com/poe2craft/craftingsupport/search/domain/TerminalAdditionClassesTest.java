package com.poe2craft.craftingsupport.search.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * 흡수 상태로 끝나는 유한 옵션 추가 구간에 대한 동치류를 증명한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code tiny}: tiny ({@code BasicCurrencyFixture}).
 *   <li>{@code policy}: 정책 ({@code AutomaticCurrencyPolicy}).
 *   <li>{@code kernel}: 전이커널 ({@code AutomaticCraftingKernel}).
 *   <li>{@code exalt}: 엑잘티드 ({@code AutomaticCraftingAction}).
 *   <li>{@code choices}: 선택목록 ({@code List<GoalDirectedPlanner.Choice>}).
 *   <li>{@code progress}: 진행상황 ({@code AutomaticMethodGraph.Progress}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code source(value)}: 출처 처리에서 {@code tiny.item}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code
 *       ItemState}.
 *   <li>{@code prove(item, classify)}: 증명 처리에서 {@code TerminalAdditionClasses.prove}을(를) 사용한다. 입력:
 *       {@code item}(아이템), {@code classify}(상태분류). 반환: {@code
 *       Optional<TerminalAdditionClasses.Key>}.
 *   <li>{@code sharesOnlyAfterComparingEveryOutcomeAndTransportsExactIncomingValues()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code distinguishesThresholdsAndRejectsIncompleteOrNonabsorbingLaws()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code rejectsRemovalIncludingLowestModifierLevelAndPreservesQualityIdentity()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code fullGraphSharesExecutionAndCheckpointRestoresConcreteOutcomeLaws()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code both()}: both 값을 조회·구성한다. 반환: {@code Function<ItemState,
 *       AutomaticMethodGraph.Progress>}.
 *   <li>{@code wideKernel()}: wide·전이커널 값을 조회·구성한다. 반환: {@code AutomaticCraftingKernel}.
 *   <li>{@code finish(graph)}: 진행 중인 계산 또는 누적 처리를 마무리한다. 입력: {@code graph}(그래프).
 *   <li>{@code restoresRunningRepresentativeAndAliasesWithoutDuplicatingExecution()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code restoresBudgetPauseThenEnlargesWithoutChangingAnyOutcome()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 * </ul>
 */
class TerminalAdditionClassesTest {
  final BasicCurrencyFixture tiny = new BasicCurrencyFixture();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          policy,
          tiny.catalog,
          new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
          "amulets");
  final AutomaticCraftingAction exalt =
      new AutomaticCraftingAction(
          WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
  final List<GoalDirectedPlanner.Choice> choices =
      List.of(new GoalDirectedPlanner.Choice(List.of(), List.of(exalt)));
  final AutomaticMethodGraph.Progress progress =
      new AutomaticMethodGraph.Progress(Status.NO_MATCH, List.of(Status.MATCH, Status.NO_MATCH));

  ItemState source(long value) {
    // A fixed source affix permits first-mutable addition; transport and conditional laws stay
    // exact.
    return tiny.item(
        ItemState.Rarity.RARE, List.of(new ModifierInstance("a", Map.of("a", value), true)));
  }

  Optional<TerminalAdditionClasses.Key> prove(
      ItemState item, Function<ItemState, Optional<Exit>> classify) {
    return TerminalAdditionClasses.prove(item, false, progress, choices, kernel, classify, 100);
  }

  @Test
  void sharesOnlyAfterComparingEveryOutcomeAndTransportsExactIncomingValues() {
    Function<ItemState, Optional<Exit>> classify = item -> Optional.of(Exit.GOAL);
    var first = source(1);
    var second = source(2);
    assertThat(prove(first, classify)).isPresent().isEqualTo(prove(second, classify));
    var expected = kernel.expand(second, exalt, 100);
    var transported =
        kernel.expand(first, exalt, 100).outcomes().stream()
            .map(
                o ->
                    new AutomaticCraftingKernel.Outcome(
                        TerminalAdditionClasses.transport(first, second, o.item()),
                        o.probability()))
            .toList();
    assertThat(transported).containsExactlyElementsOf(expected.outcomes());
    assertThat(prove(first, classify).orElseThrow().laws().getFirst().outcomes()).hasSize(2);
  }

  @Test
  void distinguishesThresholdsAndRejectsIncompleteOrNonabsorbingLaws() {
    Function<ItemState, Optional<Exit>> sumGoal =
        item ->
            item.explicits().stream()
                        .flatMap(m -> m.values().values().stream())
                        .mapToLong(Long::longValue)
                        .sum()
                    >= 5
                ? Optional.of(Exit.GOAL)
                : Optional.empty();
    assertThat(prove(source(1), sumGoal)).isNotEqualTo(prove(source(2), sumGoal));
    assertThat(prove(source(1), item -> Optional.of(Exit.CHECKPOINT))).isEmpty();
    assertThat(
            TerminalAdditionClasses.prove(
                source(1), false, progress, choices, kernel, item -> Optional.of(Exit.GOAL), 1))
        .isEmpty();
  }

  @Test
  void rejectsRemovalIncludingLowestModifierLevelAndPreservesQualityIdentity() {
    var chaos =
        new AutomaticCraftingAction(
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.WHITTLING),
            AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);
    assertThat(
            TerminalAdditionClasses.prove(
                source(1),
                false,
                progress,
                List.of(new GoalDirectedPlanner.Choice(List.of(), List.of(chaos))),
                kernel,
                item -> Optional.of(Exit.GOAL),
                100))
        .isEmpty();
    var falseQuality = prove(source(1), item -> Optional.of(Exit.GOAL));
    var trueQuality =
        TerminalAdditionClasses.prove(
            source(1), true, progress, choices, kernel, item -> Optional.of(Exit.GOAL), 100);
    assertThat(falseQuality).isPresent().isNotEqualTo(trueQuality);
  }

  @Test
  void fullGraphSharesExecutionAndCheckpointRestoresConcreteOutcomeLaws() {
    Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
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
            256,
            50000,
            100)) {
      for (int i = 0; i < 20000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      graph.publishProgress();
      assertThat(graph.finished()).isTrue();
      assertThat(graph.executionCount()).isEqualTo(graph.methods().size());
      assertThat(graph.nodes().size()).isGreaterThan(1);
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.source() != 0
                      && m.template().initial().isEmpty()
                      && m.template().cycle().equals(List.of(exalt)));
      try (var restored =
          AutomaticMethodGraph.restore(
              graph.checkpoint("same-goal"), "same-goal", policy, kernel, evaluate)) {
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
        assertThat(restored.executionCount()).isEqualTo(graph.executionCount());
        assertThat(restored.complete()).isEqualTo(graph.complete());
      }
      for (var method : graph.methods()) {
        if (method.template().initial().isEmpty()
            && method.template().cycle().equals(List.of(exalt))) {
          var source = graph.nodes().get(method.source()).item();
          var expected =
              kernel.expand(source, exalt, 100).outcomes().stream()
                  .filter(o -> evaluate.apply(o.item()).goal() == Status.MATCH)
                  .map(AutomaticCraftingKernel.Outcome::item)
                  .toList();
          var actual =
              method.points().get(1).exits().get(Exit.GOAL).keySet().stream()
                  .map(AutomaticMethodExecution.State::item)
                  .toList();
          assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        }
      }
    }
  }

  private Function<ItemState, AutomaticMethodGraph.Progress> both() {
    return item -> {
      boolean a = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
      boolean b = item.explicits().stream().anyMatch(m -> m.modifierId().equals("b"));
      return new AutomaticMethodGraph.Progress(
          a && b ? Status.MATCH : Status.NO_MATCH,
          List.of(a ? Status.MATCH : Status.NO_MATCH, b ? Status.MATCH : Status.NO_MATCH));
    };
  }

  private AutomaticCraftingKernel wideKernel() {
    var b = tiny.definition("b", ModifierDefinition.AffixType.SUFFIX, 3, 100, 3);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, b));
    return new AutomaticCraftingKernel(
        policy, catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)), "amulets");
  }

  private void finish(AutomaticMethodGraph graph) {
    for (int i = 0; i < 20000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
    graph.publishProgress();
  }

  @Test
  void restoresRunningRepresentativeAndAliasesWithoutDuplicatingExecution() {
    var k = wideKernel();
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            k,
            both(),
            CertifiedInterval.Precision.standard(),
            64,
            256,
            50000,
            1000)) {
      AutomaticMethodGraph.Checkpoint cursor = null;
      for (int i = 0; i < 20000 && !graph.finished(); i++) {
        graph.step();
        var c = graph.checkpoint("running");
        if (java.util.stream.IntStream.range(0, c.nodes().size())
            .anyMatch(
                id ->
                    c.nodes().get(id).members().size() > 1
                        && c.executions().stream()
                            .anyMatch(m -> m.source() == id && !m.finished()))) {
          cursor = c;
          break;
        }
      }
      assertThat(cursor)
          .as(
              "checkpoint must contain an unfinished method on one node with several conditional inputs")
          .isNotNull();
      try (var restored = AutomaticMethodGraph.restore(cursor, "running", policy, k, both())) {
        assertThat(restored.methods()).isEqualTo(graph.methods());
        finish(graph);
        finish(restored);
        assertThat(restored.finished()).isTrue();
        assertThat(restored.nodes()).isEqualTo(graph.nodes());
        assertThat(restored.methods()).isEqualTo(graph.methods());
        assertThat(restored.executionCount()).isEqualTo(graph.executionCount());
      }
    }
  }

  @Test
  void restoresBudgetPauseThenEnlargesWithoutChangingAnyOutcome() {
    var k = wideKernel();
    try (var graph =
        new AutomaticMethodGraph(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            policy,
            k,
            both(),
            CertifiedInterval.Precision.standard(),
            64,
            2,
            50000,
            1000)) {
      finish(graph);
      assertThat(graph.resourceLimited()).isTrue();
      try (var restored =
          AutomaticMethodGraph.restore(graph.checkpoint("paused"), "paused", policy, k, both())) {
        assertThat(restored.resourceLimited()).isTrue();
        assertThat(restored.methods()).isEqualTo(graph.methods());
        graph.increaseLimits(64, 256, 50000, 1000);
        restored.increaseLimits(64, 256, 50000, 1000);
        try (var enlarged =
            AutomaticMethodGraph.restore(
                restored.checkpoint("enlarged"), "enlarged", policy, k, both())) {
          finish(graph);
          finish(restored);
          finish(enlarged);
          assertThat(enlarged.finished()).isTrue();
          assertThat(restored.methods()).isEqualTo(graph.methods());
          assertThat(enlarged.methods()).isEqualTo(graph.methods());
          assertThat(enlarged.nodes()).isEqualTo(graph.nodes());
        }
      }
    }
  }
}
