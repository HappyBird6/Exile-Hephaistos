package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 원래 작업대의 동률 제거 및 등급별 추첨 분모로 절사 한 번의 세 결과와 표시 확률을 검증한다. */
class RecipeWhittlingChanceTest {
  private final BasicCurrencyFixture fixture = new BasicCurrencyFixture();
  private final RouteCards.Card card =
      new RouteCards.Card(
          List.of(
              new RouteCards.Option(
                  1,
                  Map.of(
                      "target",
                      List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))))),
          0);

  /** 같은 요구 레벨의 두 접두와 목표 접미, 미적중 시 복구 여부가 다른 높은 레벨의 접두·접미를 만든다. */
  private WorkbenchSimulator workbench() {
    var catalog =
        fixture.catalog(
            List.of(
                fixture.implicit,
                mod("b", true, 1, 2),
                mod("c", true, 1, 2),
                mod("target", false, 70, 2),
                mod("higher-prefix", true, 70, 1),
                mod("higher-suffix", false, 70, 2)));
    return new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  }

  /** 다른 계열과 배타 충돌이 없는 고정 수치의 옵션이다. */
  private ModifierDefinition mod(String id, boolean prefix, int level, int weight) {
    var def =
        fixture.definition(
            id,
            prefix ? ModifierDefinition.AffixType.PREFIX : ModifierDefinition.AffixType.SUFFIX,
            1,
            1,
            weight);
    return new ModifierDefinition(
        id,
        id,
        def.layer(),
        def.affixType(),
        def.familyIds(),
        level,
        weight,
        1,
        id,
        def.stats(),
        Set.of(),
        def.sourceUrl());
  }

  /** b·c 중 하나가 동률로 사라진다. 높은 접두는 남은 낮은 접두를 넘어서 제거하지 못하고, 높은 접미는 우측 말소로 회복 가능하다. */
  @Test
  void tiesAndOtherOmenPairsUseTheWholeOneActionDenominatorInFixedReplay() {
    var simulator = workbench();
    var start =
        fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("b", 1), fixture.mod("c", 1)));
    for (String action : List.of("CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS")) {
      var chosen =
          new RecipeRoutePlanner.Step(
              action, List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
      var repl =
          RecipeRoutePlanner.evaluateFixed(
              simulator,
              start,
              card,
              RecipeWhittlingChanceTest::matches,
              1000,
              true,
              null,
              List.of(chosen));
      var result = repl.routes().getFirst();
      var step = result.steps().getFirst();
      var odds = step.omenOutcomes();
      // b와 c는 각 계열의 유일한 티어여서 상급·완벽에서도 최상위 티어 예외로 추첨된다.
      int total = 7;
      assertThat(odds.success()).isEqualTo(step.chance());
      assertThat(odds.success().probability()).isCloseTo(2d / total, offset(1e-14));
      assertThat(odds.recoverable().probability()).isCloseTo((total - 3d) / total, offset(1e-14));
      assertThat(odds.failure().probability()).isCloseTo(1d / total, offset(1e-14));
      assertThat(result.probability()).isEqualTo(odds.success().probability());
      assertThat(step.maxUses()).isNull();
    }
  }

  /** 가득 찬 접두 세 개 중 기존 목표를 지우는 1/3은 새 목표나 원래 ID가 다시 붙어도 모두 실패다. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(RouteCards.Frequency.class)
  void losingAProtectedAffixInTheInitialRemovalTieIsAnAbsorbingFailure(
      RouteCards.Frequency frequency) {
    var catalog =
        fixture.catalog(
            List.of(
                fixture.implicit,
                mod("keep", true, 1, 2),
                mod("b", true, 1, 2),
                mod("c", true, 1, 2),
                mod("target", true, 70, 2),
                mod("higher-prefix", true, 70, 1),
                mod("higher-suffix", false, 70, 2)));
    var bench = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var start =
        fixture.item(
            ItemState.Rarity.RARE,
            List.of(fixture.mod("keep", 1), fixture.mod("b", 1), fixture.mod("c", 1)));
    var goals =
        new RouteCards.Card(
            List.of(
                card.options().getFirst(),
                new RouteCards.Option(
                    2, Map.of("keep", List.of(new RouteCards.Source("B", null, frequency))))),
            0);
    var hit =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            bench,
            start,
            goals,
            s -> matches(s) && s.explicits().stream().anyMatch(m -> m.modifierId().equals("keep")),
            1000,
            true,
            null,
            List.of(hit));
    var target = fixed.routes().getFirst();
    var probabilities = target.steps().getFirst().omenOutcomes();
    assertThat(probabilities.success().probability()).isCloseTo(4d / 21, offset(1e-14));
    assertThat(probabilities.recoverable().probability()).isCloseTo(8d / 21, offset(1e-14));
    assertThat(probabilities.failure().probability()).isCloseTo(9d / 21, offset(1e-14));
    assertThat(target.probability()).isEqualTo(probabilities.success().probability());
    // 다른 화폐의 M/H 복구 규칙과 달리 절사에서는 목표를 잃는 경로 자체가 존재하지 않는다.
    var losing = new RecipeRoutePlanner.Step("CHAOS", hit.omens(), List.of(1), List.of(2), null);
    var regain = new RecipeRoutePlanner.Step("CHAOS", hit.omens(), List.of(2), List.of(), null);
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    bench,
                    start,
                    goals,
                    s ->
                        matches(s)
                            && s.explicits().stream().anyMatch(m -> m.modifierId().equals("keep")),
                    2000,
                    true,
                    null,
                    List.of(losing, regain))
                .routes())
        .isEmpty();
  }

  /** 제왕에서 나온 서로 다른 더미의 조건부 비중 2:1:2로 절사 한 번의 세 결과를 각각 가중한다. */
  @Test
  void thePriorRegalDummiesAreWeightedByTheirActualRollProbabilities() {
    var start = fixture.item(ItemState.Rarity.MAGIC, List.of(fixture.mod("b", 1)));
    var prepare = new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(), List.of(), null);
    var hit =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            workbench(),
            start,
            card,
            RecipeWhittlingChanceTest::matches,
            2000,
            true,
            null,
            List.of(prepare, hit));
    var route = fixed.routes().getFirst();
    var outcomes = route.steps().getLast().omenOutcomes();
    assertThat(outcomes.success().probability()).isCloseTo(39d / 140, offset(1e-14));
    assertThat(outcomes.recoverable().probability()).isCloseTo(93d / 140, offset(1e-14));
    assertThat(outcomes.failure().probability()).isCloseTo(8d / 140, offset(1e-14));
    assertThat(route.probability()).isEqualTo(outcomes.success().probability());
  }

  /** 예산이 작은 경우에 이미 측정한 적중률과 실측한 세 결과 모두 범위 안에 들어간다. */
  @Test
  void partialClassificationKeepsTheUnseenMissMass() {
    var simulator = workbench();
    var start =
        fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("b", 1), fixture.mod("c", 1)));
    var chosen =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
    var repl =
        RecipeRoutePlanner.evaluateFixed(
            simulator,
            start,
            card,
            RecipeWhittlingChanceTest::matches,
            2,
            true,
            null,
            List.of(chosen));
    var odds = repl.routes().getFirst().steps().getFirst().omenOutcomes();
    assertThat(odds.success().probability()).isCloseTo(2d / 7, offset(1e-14));
    assertThat(odds.recoverable().probability()).isNull();
    assertThat(4d / 7)
        .isBetween(
            odds.recoverable().probabilityBounds().lower(),
            odds.recoverable().probabilityBounds().upper());
    assertThat(1d / 7)
        .isBetween(
            odds.failure().probabilityBounds().lower(), odds.failure().probabilityBounds().upper());
    assertThat(odds.recoverable().probabilityBounds().upper()).isLessThan(1);
  }

  /** 같은 베이스에서 반복하는 대상은 무징조 카오스다. 절사 1회에서 복구 가능한 미적중은 다음 단계의 성공으로 들어가지 않는다. */
  @Test
  void onlyUnmarkedChaosRepeatsOnTheSingleMutableAffix() {
    var simulator = workbench();
    var start = fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("b", 1)));
    var result =
        RecipeRoutePlanner.calculate(
            simulator, start, List.of(card), RecipeWhittlingChanceTest::matches, 10000, true, null);
    var routes =
        result.routes().stream()
            .filter(r -> r.steps().size() == 1 && r.steps().getFirst().action().equals("CHAOS"))
            .toList();
    assertThat(routes)
        .anyMatch(
            r -> r.steps().getFirst().omens().isEmpty() && r.steps().getFirst().maxUses() != null);
    for (var route : routes) {
      var step = route.steps().getFirst();
      if (step.omens().isEmpty()) {
        assertThat(step.omenOutcomes()).isNull();
        continue;
      }
      assertThat(step.maxUses()).isNull();
      assertThat(step.omenOutcomes().success().probability()).isCloseTo(2d / 9, offset(1e-14));
      assertThat(step.omenOutcomes().recoverable().probability()).isCloseTo(7d / 9, offset(1e-14));
      assertThat(step.omenOutcomes().failure().probability()).isZero();
      assertThat(route.probability()).isEqualTo(step.chance().probability());
      var repeated =
          new RecipeRoutePlanner.Step(
              step.action(), step.omens(), step.acquired(), step.lost(), null, 1000);
      assertThat(repeated.validGradeSelection()).isFalse();
      assertThat(
              RecipeRoutePlanner.evaluateFixed(
                      simulator,
                      start,
                      card,
                      RecipeWhittlingChanceTest::matches,
                      1000,
                      true,
                      null,
                      List.of(repeated))
                  .routes())
          .isEmpty();
    }
    assertThat(routes).anyMatch(r -> r.steps().getFirst().omenOutcomes() != null);
  }

  /** 획득된 실제 옵션 ID로 완료 상태를 검증한다. */
  private static boolean matches(ItemState item) {
    return item.explicits().stream().anyMatch(m -> m.modifierId().equals("target"));
  }
}
