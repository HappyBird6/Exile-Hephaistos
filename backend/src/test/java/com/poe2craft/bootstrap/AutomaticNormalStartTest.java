package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 자동·일반·시작값 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code firstChaosFromNormalSolarCanFinishThreeAffixes()}: 일반 태양 목걸이에서 첫 카오스 반복과 세 목표의 동시
 *       충족을 확인한다.
 *   <li>{@code normalSolarFindsRareLifeWithoutAlchemy()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class AutomaticNormalStartTest {
  @Test
  void normalSolarFindsRareLifeWithoutAlchemy() {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var root = fixture.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal = fixture.goal(root, Map.of(GoalDirectedPlannerScenariosTest.LIFE, 1L));
    try (var graph = fixture.graph(root, goal)) {
      fixture.exhaust(graph);
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.item().rarity() == ItemState.Rarity.RARE
                      && n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.source() == 0
                      && m.template().initial().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.TRANSMUTATION));
      assertThat(graph.methods())
          .noneMatch(
              m ->
                  m.template().initial().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.ALCHEMY)
                      || m.template().cycle().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.ALCHEMY));
      System.out.println(
          "NORMAL_START|finished="
              + graph.finished()
              + "|resources="
              + graph.resources()
              + "|goalNodes="
              + graph.nodes().stream()
                  .filter(n -> n.progress().goal() == GoalFilter.Status.MATCH)
                  .count());
    }
  }

  @Test
  void firstChaosFromNormalSolarCanFinishThreeAffixes() {
    var fixture = new GoalDirectedPlannerScenariosTest();
    var root = fixture.item(ItemState.Rarity.NORMAL, 82, List.of());
    var goal =
        fixture.goal(
            root,
            Map.of(
                StrictExaltedScenarioTest.DEFENCE,
                20L,
                StrictExaltedScenarioTest.SPIRIT,
                30L,
                StrictExaltedScenarioTest.PROJECTILE,
                3L));
    try (var graph = fixture.graph(root, goal)) {
      graph.increaseLimits(128, 256, 40000, 10000);
      fixture.exhaust(graph);
      assertThat(graph.methods())
          .anyMatch(
              m ->
                  m.template().boundedChaos()
                      && m.template().initial().stream()
                          .anyMatch(a -> a.currency() == WorkbenchCurrency.REGAL)
                      && graph.nodes().get(m.source()).item().explicitSlotCount() == 0);
      assertThat(graph.nodes())
          .anyMatch(
              n ->
                  n.progress().goal() == GoalFilter.Status.MATCH
                      && n.progress().entries().stream()
                          .allMatch(e -> e == GoalFilter.Status.MATCH));
    }
  }
}
