package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.AutomaticCraftingKernel;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.craftingsupport.search.domain.GoalDirectedPlanner;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * 자동·준비·규칙목록 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code tiny}: tiny ({@code BasicCurrencyFixture}).
 *   <li>{@code policy}: 정책 ({@code AutomaticCurrencyPolicy}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code kernel(catalog)}: 전이커널의 입력에 따른 분기·결과를 처리한다. 입력: {@code catalog}(카탈로그). 반환: {@code
 *       AutomaticCraftingKernel}.
 *   <li>{@code goal(modifier, value, rare)}: 목표 처리에서 {@code item.explicits}, {@code
 *       m.modifierId}을(를) 사용한다. 입력: {@code modifier}(옵션), {@code value}(값), {@code rare}(희귀). 반환:
 *       {@code Function<ItemState, AutomaticMethodGraph.Progress>}.
 *   <li>{@code choices(item, catalog, evaluate)}: 선택목록 처리에서 {@code
 *       AutomaticCraftingAction.candidates}을(를) 사용한다. 입력: {@code item}(아이템), {@code catalog}(카탈로그),
 *       {@code evaluate}(평가). 반환: {@code List<GoalDirectedPlanner.Choice>}.
 *   <li>{@code chaosPreparationUsesExactlyOnePlainExaltOrRegalWithoutAssumingEqualProbabilities()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code goalRegalUsesTheHelpfulSuffixOmen()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code certainCompletedRegalGoalDoesNotPayForAnOmenWithDifferentUnconstrainedJunk()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code identicalRegalLawKeepsOnlyPlainRegal()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code normalAmuletHasAnExactTransmutationThenRegalPathToRare()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 * </ul>
 */
class AutomaticPreparationRulesTest {
  final BasicCurrencyFixture tiny = new BasicCurrencyFixture();
  final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();

  AutomaticCraftingKernel kernel(ItemCatalog catalog) {
    return new AutomaticCraftingKernel(
        policy, catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)), "amulets");
  }

  Function<ItemState, AutomaticMethodGraph.Progress> goal(
      String modifier, Long value, boolean rare) {
    return item -> {
      boolean hit =
          item.explicits().stream()
              .anyMatch(
                  m ->
                      m.modifierId().equals(modifier)
                          && (value == null || m.values().containsValue(value)));
      return new AutomaticMethodGraph.Progress(
          hit && (!rare || item.rarity() == ItemState.Rarity.RARE) ? Status.MATCH : Status.NO_MATCH,
          List.of(hit ? Status.MATCH : Status.NO_MATCH));
    };
  }

  List<GoalDirectedPlanner.Choice> choices(
      ItemState item,
      ItemCatalog catalog,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate) {
    return new GoalDirectedPlanner(
            kernel(catalog), evaluate, AutomaticCraftingAction.candidates(policy))
        .choices(item);
  }

  @Test
  void chaosPreparationUsesExactlyOnePlainExaltOrRegalWithoutAssumingEqualProbabilities() {
    var evaluate = goal("a", 2L, true);
    for (var root :
        List.of(
            tiny.item(ItemState.Rarity.RARE, List.of()),
            tiny.item(ItemState.Rarity.MAGIC, List.of()))) {
      var prepared =
          choices(root, tiny.catalog, evaluate).stream()
              .filter(
                  c ->
                      !c.initial().isEmpty()
                          && c.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS)
              .toList();
      assertThat(prepared).hasSize(1);
      assertThat(prepared.getFirst().initial().getFirst().currency())
          .isEqualTo(
              root.rarity() == ItemState.Rarity.RARE
                  ? WorkbenchCurrency.EXALTED
                  : WorkbenchCurrency.REGAL);
      assertThat(prepared.getFirst().initial().getFirst().omens()).isEmpty();
    }
  }

  @Test
  void goalRegalUsesTheHelpfulSuffixOmen() {
    var c = tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 2);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, c));
    var root = tiny.item(ItemState.Rarity.MAGIC, List.of(tiny.mod("a", 1)));
    var regal =
        choices(root, catalog, goal("b", null, true)).stream()
            .filter(
                craft ->
                    craft.initial().isEmpty()
                        && craft.cycle().getFirst().currency() == WorkbenchCurrency.REGAL)
            .toList();
    assertThat(regal).hasSize(1);
    assertThat(regal.getFirst().cycle().getFirst().omens())
        .containsExactly(WorkbenchOmen.DEXTRAL_CORONATION);
    assertThat(kernel(catalog).expand(root, regal.getFirst().cycle().getFirst(), 5000).outcomes())
        .allMatch(o -> goal("b", null, true).apply(o.item()).goal() == Status.MATCH);
  }

  @Test
  void certainCompletedRegalGoalDoesNotPayForAnOmenWithDifferentUnconstrainedJunk() {
    var c = tiny.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 2);
    var catalog = tiny.catalog(List.of(tiny.implicit, tiny.a, tiny.b, c));
    var root = tiny.item(ItemState.Rarity.MAGIC, List.of(tiny.mod("a", 1)));
    var regal =
        choices(root, catalog, goal("a", null, true)).stream()
            .filter(
                craft ->
                    craft.initial().isEmpty()
                        && craft.cycle().getFirst().currency() == WorkbenchCurrency.REGAL)
            .toList();
    assertThat(regal).hasSize(1);
    assertThat(regal.getFirst().cycle().getFirst().omens()).isEmpty();
  }

  @Test
  void identicalRegalLawKeepsOnlyPlainRegal() {
    var root = tiny.item(ItemState.Rarity.MAGIC, List.of(tiny.mod("a", 1)));
    var regal =
        choices(root, tiny.catalog, goal("b", null, true)).stream()
            .filter(
                c ->
                    c.initial().isEmpty()
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.REGAL)
            .toList();
    assertThat(regal).hasSize(1);
    assertThat(regal.getFirst().cycle().getFirst().omens()).isEmpty();
  }

  @Test
  void normalAmuletHasAnExactTransmutationThenRegalPathToRare() {
    var root = tiny.item(ItemState.Rarity.NORMAL, List.of());
    var evaluate = goal("a", null, true);
    var k = kernel(tiny.catalog);
    var transmute =
        new AutomaticCraftingAction(
            WorkbenchCurrency.TRANSMUTATION,
            Set.of(),
            AutomaticCurrencyPolicy.Purpose.RARITY_UPGRADE);
    var law = k.expand(root, transmute, 5000);
    assertThat(law.status()).isEqualTo(AutomaticCraftingKernel.Status.COMPLETE);
    assertThat(law.outcomes())
        .allMatch(
            o -> o.item().rarity() == ItemState.Rarity.MAGIC && o.item().explicits().size() == 1);
    assertThat(AutomaticCraftingAction.candidates(policy))
        .noneMatch(a -> a.currency() == WorkbenchCurrency.ALCHEMY);
    try (var graph =
        new AutomaticMethodGraph(
            root,
            policy,
            k,
            evaluate,
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000)) {
      for (int i = 0; i < 30000 && !graph.finished() && !graph.resourceLimited(); i++) graph.step();
      assertThat(graph.finished()).isTrue();
      var method =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.template().initial().equals(List.of(transmute))
                          && m.template().cycle().size() == 1
                          && m.template().cycle().getFirst().currency() == WorkbenchCurrency.REGAL
                          && m.template().cycle().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var point = method.points().get(1);
      var hit =
          point.exits().get(Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(hit.contains(Fraction.ONE)).isTrue();
      assertThat(point.initialUses().get(transmute).contains(Fraction.ONE)).isTrue();
      assertThat(point.cycleUses().values()).allMatch(mass -> mass.contains(Fraction.ONE));
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.item().rarity() == ItemState.Rarity.RARE
                      && n.progress().goal() == Status.MATCH);
    }
  }
}
