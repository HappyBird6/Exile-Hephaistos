package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** 시작 아이템의 한 옵션을 반복 교체하는 경로와 단회 경로를 원래 추첨 가중치로 독립 검증한다. */
class RecipeChaosRouteTest {
  /** 목표 a의 가중치는 1이며, 시작 옵션 b는 재추첨 가중치 1999를 갖는다. */
  private final BasicCurrencyFixture fixture = new BasicCurrencyFixture();

  private final com.poe2craft.shared.item.ItemCatalog catalog =
      fixture.catalog(
          List.of(
              fixture.implicit,
              fixture.a,
              fixture.definition("b", ModifierDefinition.AffixType.SUFFIX, 3, 4, 1999)));
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  private final ItemState start = fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("b", 3)));
  private final Predicate<ItemState> goal =
      item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));

  /** 목표 번호 1의 빈도에 따른 생성 정책을 실제 확률 계산과 독립적으로 지정한다. */
  private RouteCards.Card card(RouteCards.Frequency frequency) {
    return new RouteCards.Card(
        List.of(
            new RouteCards.Option(
                1, Map.of("a", List.of(new RouteCards.Source("B", null, frequency))))),
        0);
  }

  /** 한 번에 1/2000이므로 1000회 안에도 실패한다. 결과 분포를 확률 1로 처리할 수 없다. */
  @Test
  void initialChaosOffersBothMethodsWithActualOddsAndCappedConsumption() {
    var card = card(RouteCards.Frequency.M);
    var result =
        RecipeRoutePlanner.calculate(simulator, start, List.of(card), goal, 18000, true, null);
    var direct =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 1
                        && r.steps().getFirst().action().equals("CHAOS")
                        && r.steps().getFirst().omens().isEmpty())
            .toList();
    assertThat(direct).hasSize(2);
    double perTry = 1.0 / 2000;
    double cumulative = -Math.expm1(1000 * Math.log1p(-perTry));
    for (var route : direct) {
      var step = route.steps().getFirst();
      assertThat(step.chance().probability()).isCloseTo(perTry, offset(1e-14));
      if (step.maxUses() == null) {
        assertThat(step.expectedUses()).isNull();
        assertThat(route.probability()).isCloseTo(perTry, offset(1e-14));
      } else {
        assertThat(step.maxUses()).isEqualTo(1000);
        assertThat(step.expectedUses()).isCloseTo(cumulative / perTry, offset(1e-10));
        assertThat(route.probability()).isCloseTo(cumulative, offset(1e-14));
      }
      assertThat(goal.test(result.states().get(route.stateIndices().getFirst()))).isTrue();
      var request =
          new RecipeRoutePlanner.Step(
              step.action(),
              step.omens(),
              step.acquired(),
              step.lost(),
              step.socketIndex(),
              step.maxUses());
      var replay =
          RecipeRoutePlanner.evaluateFixed(
              simulator, start, card, goal, 18000, true, null, List.of(request));
      assertThat(replay.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps()).containsExactly(step);
                assertThat(r.probability()).isEqualTo(route.probability());
              });
    }
  }

  /** L은 시작 카오스 반복과 Perfect Exalt 예외로만 새로 얻으며 단회 카오스나 일반 Exalt의 적중은 쓰지 않는다. */
  @Test
  void freshLowTierDoesNotUseSingleChaosOrBasicExalt() {
    var result =
        RecipeRoutePlanner.calculate(
            simulator, start, List.of(card(RouteCards.Frequency.L)), goal, 18000, true, null);
    assertThat(result.routes())
        .anyMatch(
            r ->
                r.steps().getFirst().action().equals("CHAOS")
                    && Integer.valueOf(1000).equals(r.steps().getFirst().maxUses()));
    assertThat(result.routes())
        .noneMatch(
            r ->
                r.steps().getFirst().action().equals("EXALTED")
                    || (r.steps().getFirst().action().equals("CHAOS")
                        && r.steps().getFirst().maxUses() == null));
  }

  /** 우선인 M을 기다리는 동안 임시 H만 붙은 결과도 재시도에 들어간다. 이후 엑잘로 H를 얻는다. */
  @Test
  void higherFrequencyHitsContinueUntilTheTargetedMediumOptionAppears() {
    var other = fixture.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 2);
    var smallCatalog = fixture.catalog(List.of(fixture.implicit, fixture.a, fixture.b, other));
    var bench = new WorkbenchSimulator(smallCatalog, new CraftingEngine(smallCatalog));
    var goals =
        new RouteCards.Card(
            List.of(
                card(RouteCards.Frequency.M).options().getFirst(),
                new RouteCards.Option(
                    2,
                    Map.of(
                        "c", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.H))))),
            0);
    Predicate<ItemState> both =
        s ->
            s.explicits().stream().map(m -> m.modifierId()).toList().containsAll(List.of("a", "c"));
    var seek = new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(1), List.of(), null, 1000);
    var high = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(2), List.of(), null);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            bench, start, goals, both, 10000, true, null, List.of(seek, high));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.order()).containsExactly(1, 2);
              assertThat(r.steps().getFirst().chance().probability())
                  .isCloseTo(1.0 / 6, offset(1e-12));
              assertThat(r.steps().getFirst().expectedUses()).isCloseTo(6, offset(1e-12));
              assertThat(r.probability()).isCloseTo(2.0 / 5, offset(1e-12));
            });
    var wrongFirst =
        new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(2), List.of(), null, 1000);
    var later = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    bench, start, goals, both, 10000, true, null, List.of(wrongFirst, later))
                .routes())
        .isEmpty();
  }

  /** Perfect Exalt에서 만들어진 L 후보도 같은 드롭 목표와 보유 상태에서 각 등급의 전이로 다시 계산한다. */
  @Test
  void nativePerfectExaltComparesBothLowerGradesOnTheSameRoute() {
    var actual = new RecipeTransitionsTest();
    var rare = actual.item(ItemState.Rarity.RARE, "amulet:prefix:adept-s");
    String target = "amulet:prefix:virile";
    var goals =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        target,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L))))),
            0);
    Predicate<ItemState> done =
        s -> s.explicits().stream().anyMatch(m -> m.modifierId().equals(target));
    for (String grade : List.of("PERFECT_EXALTED", "EXALTED", "GREATER_EXALTED")) {
      var step = new RecipeRoutePlanner.Step(grade, List.of(), List.of(1), List.of(), null);
      var chosen =
          RecipeRoutePlanner.evaluateFixed(
              actual.workbench, rare, goals, done, 1000, true, null, List.of(step));
      var law =
          new RecipeTransitions(actual.workbench, true)
              .successors(rare, grade, java.util.Set.of(), java.util.Set.of(target), 1000, false);
      var odds = law.outcomes().getFirst().probability();
      assertThat(chosen.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps().getFirst().action()).isEqualTo(grade);
                assertThat(r.probability())
                    .isCloseTo(
                        odds.numerator().doubleValue() / odds.denominator().doubleValue(),
                        offset(1e-12));
              });
    }
  }

  /** 같은 번호에서 L과 H가 모두 허용돼도 원래 어떤 티어를 겨누던 제작인지 등급 변경 후에 유지한다. */
  @Test
  void preservesTheOriginalTierPolicyWhenBothExaltRoutesExist() {
    var actual = new RecipeTransitionsTest();
    var start = actual.item(ItemState.Rarity.RARE, "amulet:prefix:adept-s");
    String low = "amulet:prefix:virile";
    String high = actual.life;
    var goals =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        low,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L)),
                        high,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.H))))),
            0);
    for (String origin : List.of("PERFECT_EXALTED", "EXALTED")) {
      String target = origin.equals("EXALTED") ? high : low;
      var step =
          new RecipeRoutePlanner.Step(
              "GREATER_EXALTED", List.of(), List.of(1), List.of(), null, null, origin);
      assertThat(step.validGradeSelection()).isTrue();
      var result =
          RecipeRoutePlanner.evaluateFixed(
              actual.workbench,
              start,
              goals,
              s ->
                  s.explicits().stream()
                      .anyMatch(m -> m.modifierId().equals(low) || m.modifierId().equals(high)),
              10000,
              true,
              null,
              List.of(step));
      var law =
          new RecipeTransitions(actual.workbench, true)
              .successors(
                  start, step.action(), java.util.Set.of(), java.util.Set.of(target), 1000, false);
      var p = law.outcomes().getFirst().probability();
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.probability())
                    .isCloseTo(
                        p.numerator().doubleValue() / p.denominator().doubleValue(), offset(1e-12));
                assertThat(r.steps().getFirst().originalAction()).isNull();
                assertThat(result.states().get(r.stateIndices().getFirst()).explicits())
                    .anyMatch(m -> m.modifierId().equals(target));
              });
    }
  }

  /** 같은 승급에서 L이 바로 붙으면 카오스를 거치지 않고 하나의 실제 경로로 끝난다. */
  @Test
  void luckyRegalDuringChaosSetupIsReturnedOnceWithoutPayingForChaos() {
    var initial = fixture.item(ItemState.Rarity.MAGIC, List.of(fixture.mod("b", 3)));
    var result =
        RecipeRoutePlanner.calculate(
            simulator, initial, List.of(card(RouteCards.Frequency.L)), goal, 18000, true, null);
    var direct =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 1
                        && r.steps().getFirst().action().equals("REGAL")
                        && r.steps().getFirst().omens().isEmpty())
            .toList();
    assertThat(direct).hasSize(1);
    assertThat(direct.getFirst().steps().getFirst().maxUses()).isNull();
    assertThat(direct.getFirst().steps().getFirst().expectedUses()).isNull();
  }

  /** 유일한 기존 H를 버리고 L을 기다린다. 시작 H의 제거는 최종 적중과 같은 단계에 기록한다. */
  @Test
  void losingTheStartingHighOptionDoesNotStopChaosBeforeTheLowIsAcquired() {
    var goals =
        new RouteCards.Card(
            List.of(
                card(RouteCards.Frequency.L).options().getFirst(),
                new RouteCards.Option(
                    2,
                    Map.of(
                        "b", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.H))))),
            0);
    var chaos = new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(1), List.of(2), null, 1000);
    var exalt = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(2), List.of(), null);
    Predicate<ItemState> both =
        s ->
            s.explicits().stream().map(m -> m.modifierId()).toList().containsAll(List.of("a", "b"));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, goals, both, 18000, true, null, List.of(chaos, exalt));
    double cumulative = -Math.expm1(1000 * Math.log1p(-1.0 / 2000));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              var first = r.steps().getFirst();
              assertThat(first.chance().probability()).isCloseTo(1.0 / 2000, offset(1e-15));
              assertThat(first.expectedUses()).isCloseTo(2000 * cumulative, offset(1e-10));
              assertThat(r.probability()).isCloseTo(cumulative, offset(1e-14));
            });
  }
}
