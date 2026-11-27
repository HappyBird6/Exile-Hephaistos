package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Chance;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.ProbabilityBounds;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Route;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner.Step;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 한 결정점의 배타적 분열 추첨과 선택 가능한 후속 제작법을 구별하고, 원래 아이템의 무분열 대안까지 대조한다. */
class FractureStrategiesTest {
  /** 검증된 태양 목걸이의 공개 옵션 세 개와 미공개 한 칸이다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  private final List<String> ids =
      List.of("amulet:prefix:countess", fixture.life, fixture.strength);
  private final ItemState start = fixture.item(ItemState.Rarity.RARE, ids.toArray(String[]::new));
  private final ItemState decision =
      start.withDesecrated(List.of(new ItemState.DesecratedSlot("bone", AffixType.SUFFIX, null)));

  /** 원래 아이템에서 뼈의 성공 방향에 도달할 확률은 1/2이다. */
  private final Step bone = step("Preserved_Collarbone", .5, .5, null);

  private final Step reveal = step("REVEAL", 1, 1, null);

  /** 조건부 행동 확률과 시작점 기준 도달 확률을 별도로 설정한다. */
  private static Step step(String action, double chance, double reached, String hit) {
    return new Step(
        action,
        List.of(),
        List.of(),
        List.of(),
        null,
        new Chance(chance, null),
        null,
        null,
        null,
        null,
        false,
        false,
        null,
        new Chance(reached, null),
        false,
        hit);
  }

  /** 단계별 상태 번호 중 결정 직전 상태가 res.states의 0번을 가리키게 한다. */
  private Route route(int card, Double chance, ProbabilityBounds bounds, Step... steps) {
    return new Route(
        card,
        List.of(),
        List.of(steps),
        chance,
        Collections.nCopies(steps.length, 0),
        bounds,
        false);
  }

  /** 분열 결과는 동일한 3개 옵션에서 1/3 확률로 나온다. 개별 완주율은 이미 이 확률을 포함한다. */
  private Route branch(String id, double mass) {
    return route(0, mass, null, bone, step("FRACTURING", 1d / 3, .5 / 3, id), reveal);
  }

  /** 분열 결과당 하나의 제작법만 세며, 조건부 후속 확률은 분열 결과까지의 도달 질량으로 나눈다. */
  @Test
  void choosesOneContinuationPerHitAndUsesOriginalReach() {
    var plain = route(0, .21, null, bone, reveal);
    var otherCard = route(1, 1d, null, reveal);
    var first = branch(ids.get(0), .15);
    var alternate = branch(ids.get(0), .11);
    var second = branch(ids.get(1), .12);
    var third = branch(ids.get(2), .08);
    var input = List.of(plain, alternate, second, otherCard, third, first, first);
    var result = FractureStrategies.recommend(start, input, List.of(decision), true);
    assertThat(result).contains(plain, otherCard).hasSize(5);
    var recommended = result.stream().filter(r -> r.fractureStrategy() != null).toList();
    assertThat(recommended).extracting(Route::probability).containsExactlyInAnyOrder(.15, .12, .08);
    assertThat(recommended.stream().map(r -> r.fractureStrategy().decisionId()).distinct())
        .hasSize(1);
    for (var row : recommended) {
      var group = row.fractureStrategy();
      assertThat(group.stepIndex()).isOne();
      assertThat(group.successChance().probability()).isCloseTo(.35, offset(1e-14));
      assertThat(group.baselineChance().probability()).isEqualTo(.21);
      assertThat(group.conditionalChance().probability())
          .isCloseTo(row.probability() * 6, offset(1e-14));
    }
  }

  /** 일부 결과·비교 기준 또는 예산이 없으면 누락된 확률을 성공이나 영으로 취급하지 않는다. */
  @Test
  void suppressesMissingBranchesMissingComparisonAndUnknownBaseline() {
    var a = branch(ids.get(0), .15);
    var b = branch(ids.get(1), .12);
    var c = branch(ids.get(2), .08);
    var plain = route(0, .21, null, bone, reveal);
    assertThat(
            FractureStrategies.recommend(start, List.of(plain, a, a, b), List.of(decision), true))
        .containsExactly(plain);
    assertThat(FractureStrategies.recommend(start, List.of(a, b, c), List.of(decision), true))
        .isEmpty();
    assertThat(
            FractureStrategies.recommend(start, List.of(plain, a, b, c), List.of(decision), false))
        .containsExactly(plain);
    var unknown = route(0, null, null, bone, reveal);
    assertThat(
            FractureStrategies.recommend(start, List.of(unknown, a, b, c), List.of(decision), true))
        .containsExactly(unknown);
  }

  /** 같은 시작점에서 준비를 우회하는 무분열 경로, 준비 방향의 대표 수치가 다른 무분열 경로도 비교한다. */
  @Test
  void considersBypassAndDoesNotMatchOnDisplayAnnotations() {
    var a = branch(ids.get(0), .1);
    var b = branch(ids.get(1), .1);
    var c = branch(ids.get(2), .1);
    var plain = route(0, .11, null, step("Preserved_Collarbone", .45, .45, null), reveal);
    var winning =
        FractureStrategies.recommend(start, List.of(plain, a, b, c), List.of(decision), true);
    assertThat(winning).hasSize(4);
    var bypass = route(0, .35, null, step("EXALTED", .35, .35, null));
    assertThat(
            FractureStrategies.recommend(
                start, List.of(plain, bypass, a, b, c), List.of(decision), true))
        .containsExactly(plain, bypass);
    var tied = route(0, .3, null, bone, reveal);
    assertThat(FractureStrategies.recommend(start, List.of(tied, a, b, c), List.of(decision), true))
        .containsExactly(tied);
  }

  /** 서로 다른 실제 아이템이나 다른 비율로 도달했던 뼈의 결과는 하나의 분열 추첨으로 합치지 않는다. */
  @Test
  void doesNotCombineDistinctIncomingStatesOrMasses() {
    var plain = route(0, .2, null, bone, reveal);
    var a = branch(ids.get(0), .1);
    var b = branch(ids.get(1), .1);
    var c = branch(ids.get(2), .1);
    var otherState =
        decision.withDesecrated(
            List.of(new ItemState.DesecratedSlot("bone", AffixType.PREFIX, null)));
    var elsewhere =
        new Route(0, c.order(), c.steps(), c.probability(), List.of(1, 1, 1), null, false);
    assertThat(
            FractureStrategies.recommend(
                start, List.of(plain, a, b, elsewhere), List.of(decision, otherState), true))
        .containsExactly(plain);
    var otherMass = route(0, .1, null, bone, step("FRACTURING", 1d / 3, .11, ids.get(2)), reveal);
    assertThat(
            FractureStrategies.recommend(
                start, List.of(plain, a, b, otherMass), List.of(decision), true))
        .containsExactly(plain);
  }

  /** 분열 하한이 무분열 상한을 넘을 때만 권고하고 구간을 정확한 확률처럼 표시하지 않는다. */
  @Test
  void boundsMustProveStrictImprovement() {
    var weak =
        route(
            0,
            null,
            new ProbabilityBounds(.10, .14),
            bone,
            step("FRACTURING", 1d / 3, .5 / 3, ids.getFirst()),
            reveal);
    var b = branch(ids.get(1), .12);
    var c = branch(ids.get(2), .08);
    var plain = route(0, null, new ProbabilityBounds(.2, .29), bone, reveal);
    var result =
        FractureStrategies.recommend(start, List.of(plain, weak, b, c), List.of(decision), true);
    var rec = result.get(1).fractureStrategy();
    assertThat(rec.successChance().probability()).isNull();
    assertThat(rec.successChance().probabilityBounds().lower()).isCloseTo(.30, offset(1e-14));
    assertThat(rec.successChance().probabilityBounds().upper()).isCloseTo(.34, offset(1e-14));
    assertThat(rec.conditionalChance().probabilityBounds().lower()).isCloseTo(.6, offset(1e-14));
    var overlaps = route(0, null, new ProbabilityBounds(.21, .31), bone, reveal);
    assertThat(
            FractureStrategies.recommend(
                start, List.of(overlaps, weak, b, c), List.of(decision), true))
        .containsExactly(overlaps);
  }
}
