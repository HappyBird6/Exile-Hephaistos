package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
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

  /** 44와 48 이상은 각 화폐의 경계 티어만 남기고, 티어 확률·기대량은 적격 티어 전체로 계산한다. */
  @Test
  void spiritChaosRequiresDivineFromTheAcquisitionBoundary() {
    var f = new RecipeTransitionsTest();
    String countess = "amulet:prefix:countess";
    String marchioness = "amulet:prefix:marchioness";
    String spirit = SolarAmulet.SPIRIT_STAT;
    var item = f.item(ItemState.Rarity.RARE, f.strength);
    var chaos =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(), List.of(1), List.of(), null, null, null, true, 1);
    var divine = new RecipeRoutePlanner.Step("DIVINE", List.of(), List.of(), List.of(), null);
    for (int threshold : List.of(44, 48)) {
      boolean multiple = threshold == 44;
      var base =
          List.of(
              new RouteCards.Source(
                  "B", null, multiple ? RouteCards.Frequency.M : RouteCards.Frequency.L));
      var tiers = multiple ? Map.of(countess, base, marchioness, base) : Map.of(countess, base);
      var card = new RouteCards.Card(List.of(new RouteCards.Option(1, tiers, true, false)), 0);
      Predicate<ItemState> goal =
          state ->
              state.explicits().stream()
                  .anyMatch(m -> m.values().getOrDefault(spirit, 0L) >= threshold);
      var onlyChaos =
          RecipeRoutePlanner.evaluateFixed(
              f.workbench,
              item,
              card,
              goal,
              10000,
              false,
              null,
              List.of(chaos),
              java.util.Set.of(spirit));
      assertThat(onlyChaos.complete()).isTrue();
      assertThat(onlyChaos.routes()).isEmpty();
      var corrected =
          RecipeRoutePlanner.evaluateFixed(
              f.workbench,
              item,
              card,
              goal,
              10000,
              false,
              null,
              List.of(chaos, divine),
              java.util.Set.of(spirit));
      long weight =
          tiers.keySet().stream().mapToLong(id -> f.catalog.find(id).orElseThrow().weight()).sum();
      long total =
          f
              .workbench
              .pool(
                  RecipeStateGroups.withAffixes(item, ItemState.Rarity.RARE, List.of()),
                  WorkbenchCurrency.CHAOS,
                  null)
              .stream()
              .mapToLong(ModifierDefinition::weight)
              .sum();
      assertThat(total).isEqualTo(168856);
      assertThat(corrected.routes())
          .singleElement()
          .satisfies(
              r -> {
                var first = r.steps().getFirst();
                assertThat(first.chance().probability()).isEqualTo(1.0);
                assertThat(first.expectedUses()).isEqualTo((double) total / weight);
                assertThat(corrected.states().get(r.stateIndices().getFirst()).explicits())
                    .containsExactly(
                        new ModifierInstance(
                            multiple ? marchioness : countess,
                            Map.of(spirit, (long) threshold - 1)));
                assertThat(r.steps().getLast().chance().probability()).isEqualTo(0.75);
                assertThat(r.steps().getLast().expectedUses()).isCloseTo(4.0 / 3, offset(1e-12));
                assertThat(goal.test(corrected.states().get(r.stateIndices().getLast()))).isTrue();
              });
    }
  }

  /** 목표 a·c와 임시 옵션 b의 가중치를 1·2·3으로 정해 실제 풀과 빈 풀을 독립적으로 계산할 수 있다. */
  private WorkbenchSimulator threeOptionWorkbench() {
    var c = fixture.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 2);
    var example = fixture.catalog(List.of(fixture.implicit, fixture.a, fixture.b, c));
    return new WorkbenchSimulator(example, new CraftingEngine(example));
  }

  /** 새 경로는 1/2000 적중까지 평균 2000회를 쓰고, 기존 1000회 저장 경로는 원래 확률로 재생한다. */
  @Test
  void plainInitialChaosRepeatsUntilHitAndKeepsLegacyCappedReplay() {
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
    assertThat(direct).hasSize(1);
    double perTry = 1.0 / 2000;
    double cumulative = -Math.expm1(1000 * Math.log1p(-perTry));
    var route = direct.getFirst();
    var step = route.steps().getFirst();
    assertThat(step.maxUses()).isNull();
    assertThat(step.repeatUntilHit()).isTrue();
    assertThat(step.chance().probability()).isEqualTo(1.0);
    assertThat(step.expectedUses()).isEqualTo(2000.0);
    assertThat(route.probability()).isEqualTo(1.0);
    assertThat(goal.test(result.states().get(route.stateIndices().getFirst()))).isTrue();

    var repeat =
        new RecipeRoutePlanner.Step(
            step.action(),
            step.omens(),
            step.acquired(),
            step.lost(),
            step.socketIndex(),
            step.maxUses(),
            null,
            step.repeatUntilHit());
    var replay =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, card, goal, 18000, true, null, List.of(repeat));
    assertThat(replay.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.steps()).containsExactly(step);
              assertThat(r.probability()).isEqualTo(route.probability());
            });
    var legacy =
        new RecipeRoutePlanner.Step(
            step.action(), step.omens(), step.acquired(), step.lost(), step.socketIndex(), 1000);
    var capped =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, card, goal, 18000, true, null, List.of(legacy));
    assertThat(capped.routes())
        .singleElement()
        .satisfies(
            r -> {
              var saved = r.steps().getFirst();
              assertThat(saved.repeatUntilHit()).isFalse();
              assertThat(saved.maxUses()).isEqualTo(1000);
              assertThat(saved.chance().probability()).isCloseTo(perTry, offset(1e-14));
              assertThat(saved.expectedUses()).isCloseTo(cumulative / perTry, offset(1e-10));
              assertThat(r.probability()).isCloseTo(cumulative, offset(1e-14));
            });
    var once =
        new RecipeRoutePlanner.Step(
            step.action(), step.omens(), step.acquired(), step.lost(), step.socketIndex());
    var removed =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, card, goal, 18000, true, null, List.of(once));
    assertThat(removed.routes()).isEmpty();
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
                    && r.steps().getFirst().repeatUntilHit());
    assertThat(result.routes())
        .noneMatch(
            r ->
                r.steps().getFirst().action().equals("EXALTED")
                    || (r.steps().getFirst().action().equals("CHAOS")
                        && !r.steps().getFirst().repeatUntilHit()));
  }

  /** 우선인 M을 기다리는 동안 임시 H만 붙은 결과도 재시도에 들어간다. 이후 엑잘로 H를 얻는다. */
  @Test
  void higherFrequencyHitsContinueUntilTheTargetedMediumOptionAppears() {
    var bench = threeOptionWorkbench();
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
    assertThat(direct.getFirst().lucky()).isTrue();
    var normalTarget =
        RecipeRoutePlanner.calculate(
            simulator, initial, List.of(card(RouteCards.Frequency.M)), goal, 18000, true, null);
    assertThat(
            normalTarget.routes().stream()
                .filter(
                    r -> r.steps().size() == 1 && r.steps().getFirst().action().equals("REGAL")))
        .isNotEmpty()
        .allMatch(r -> !r.lucky());
  }

  /** 분열 뒤 첫 빈칸이 다음 목표에 해당해도 카오스가 즉시 교체할 더미는 준비 확률이나 획득 순서를 제한하지 않는다. */
  @Test
  void aFractureOnlyExaltCanUseTheFutureHighTargetAsItsDisposableSlot() {
    var bench = threeOptionWorkbench();
    var fractured = new ModifierInstance("c", Map.of("c", 1L), true);
    var initial = fixture.item(ItemState.Rarity.RARE, List.of(fractured));
    var options =
        new RouteCards.Card(
            List.of(
                card(RouteCards.Frequency.L).options().getFirst(),
                new RouteCards.Option(
                    2,
                    Map.of("b", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.H)))),
                new RouteCards.Option(
                    3,
                    Map.of(
                        "c", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.H))))),
            0);
    Predicate<ItemState> done =
        item ->
            item.explicits().stream()
                .map(ModifierInstance::modifierId)
                .toList()
                .containsAll(List.of("a", "b", "c"));
    var dummy = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(), List.of(), null);
    var chaos =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(), List.of(1), List.of(), null, null, null, true);
    var high = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(2), List.of(), null);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            bench, initial, options, done, 5000, true, null, List.of(dummy, chaos, high));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.lucky()).isFalse();
              assertThat(route.order()).containsExactly(1, 2);
              assertThat(route.steps().getFirst().dummy()).isTrue();
              assertThat(route.steps().getFirst().chance().probability()).isEqualTo(1);
              assertThat(result.states().get(route.stateIndices().getFirst()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder("b", "c");
              assertThat(route.steps().get(1).chance().probability()).isEqualTo(1);
              assertThat(route.probability()).isEqualTo(1);
            });
    var lucky = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    var earlierHit =
        RecipeRoutePlanner.evaluateFixed(
            bench, initial, options, done, 5000, true, null, List.of(lucky, high));
    assertThat(earlierHit.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.lucky()).isTrue();
              assertThat(route.steps())
                  .extracting(RecipeRoutePlanner.Step::lucky)
                  .containsExactly(true, false);
              assertThat(route.probability()).isEqualTo(0.25);
              assertThat(route.steps().getFirst().dummy()).isFalse();
              assertThat(route.steps().getFirst().chance().probability()).isEqualTo(0.25);
            });
  }

  /** 같은 실제 엑잘 등급도 최초 의도가 한 칸 준비면 럭키, 완벽 엑잘 목표 추첨이면 직접 획득이다. */
  @Test
  void selectedGradeRetainsTheIntendedLuckyHitOnAnEmptySolar() {
    var actual = new RecipeTransitionsTest();
    var empty = actual.item(ItemState.Rarity.RARE);
    String target = "amulet:suffix:of-battle";
    var goals =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        target,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L))))),
            0);
    var grade = "GREATER_EXALTED";
    var law =
        new RecipeTransitions(actual.workbench, true)
            .successors(empty, grade, java.util.Set.of(), java.util.Set.of(target), 1000, false);
    var actualChance = law.outcomes().getFirst().probability();
    for (String origin : List.of("EXALTED", "PERFECT_EXALTED")) {
      var selected =
          new RecipeRoutePlanner.Step(grade, List.of(), List.of(1), List.of(), null, null, origin);
      var result =
          RecipeRoutePlanner.evaluateFixed(
              actual.workbench,
              empty,
              goals,
              state -> state.explicits().stream().anyMatch(mod -> mod.modifierId().equals(target)),
              1000,
              true,
              null,
              List.of(selected));
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              route -> {
                assertThat(route.lucky()).isEqualTo(origin.equals("EXALTED"));
                var step = route.steps().getFirst();
                assertThat(step.dummy()).isFalse();
                assertThat(step.chance().probability())
                    .isCloseTo(
                        actualChance.numerator().doubleValue()
                            / actualChance.denominator().doubleValue(),
                        offset(1e-14));
                assertThat(route.probability()).isEqualTo(step.chance().probability());
              });
    }
  }

  /** 분열로 현재 풀이 줄어도 카오스 수량은 빈 베이스의 전체 가중치로 추정하고 실제 적중 판정은 분열을 보존한다. */
  @Test
  void estimatesCountOnTheEmptyPoolWhileValidatingTheActualFracture() {
    var fracture = new ModifierInstance("c", Map.of("c", 1L), true);
    var withFracture = fixture.item(ItemState.Rarity.RARE, List.of(fracture, fixture.mod("b", 3)));
    var bench = threeOptionWorkbench();
    var goalCard =
        new RouteCards.Card(
            List.of(
                card(RouteCards.Frequency.L).options().getFirst(),
                new RouteCards.Option(
                    2,
                    Map.of(
                        "c", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))))),
            0);
    var chaos = new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(1), List.of(), null, 1000);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            bench,
            withFracture,
            goalCard,
            s -> goal.test(s) && s.explicits().contains(fracture),
            1000,
            true,
            null,
            List.of(chaos));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.steps().getFirst().chance().probability())
                  .isCloseTo(1.0 / 4, offset(1e-12));
              assertThat(r.steps().getFirst().expectedUses()).isCloseTo(6, offset(1e-12));
              assertThat(result.states().get(r.stateIndices().getLast()).explicits())
                  .contains(fracture);
            });
  }

  /** 목표 티어 하나는 수치 범위가 있어도 단일 경계 상태다. 결과 예산 한 칸으로 적중 확률을 검증한다. */
  @Test
  void verifiesTheWholeTargetTierUnderASingleOutcomeBudget() {
    var chaos = new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(1), List.of(), null, 1000);
    var partial =
        RecipeRoutePlanner.evaluateFixed(
            simulator, start, card(RouteCards.Frequency.L), goal, 1, false, null, List.of(chaos));
    double first = 1.0 / 2000;
    double estimate = -Math.expm1(1000 * Math.log1p(-first)) / first;
    assertThat(partial.routes())
        .singleElement()
        .satisfies(
            r -> {
              var step = r.steps().getFirst();
              assertThat(step.chance().probability()).isEqualTo(first);
              assertThat(step.expectedUses()).isCloseTo(estimate, offset(1e-10));
            });
  }

  /** 동률 L 둘을 각자 지정하여 반복한다. 먼저 나온 다른 L의 가중치는 성공 확률과 기대량에 합치지 않는다. */
  @Test
  void competingLowOptionsEachRepeatToCertaintyAtTheirOwnCost() {
    var bench = threeOptionWorkbench();
    var low = List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L));
    var goals =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of("a", low)),
                new RouteCards.Option(2, Map.of("c", low))),
            0);
    Predicate<ItemState> both =
        state ->
            state.explicits().stream()
                .map(ModifierInstance::modifierId)
                .toList()
                .containsAll(List.of("a", "c"));
    var generated =
        RecipeRoutePlanner.calculate(bench, start, List.of(goals), both, 18000, true, null);
    for (int number : List.of(1, 2)) {
      var route =
          generated.routes().stream()
              .filter(r -> r.steps().size() == 2)
              .filter(r -> Integer.valueOf(number).equals(r.steps().getFirst().repeatTarget()))
              .filter(r -> r.steps().getLast().action().equals("PERFECT_EXALTED"))
              .findFirst()
              .orElseThrow();
      var first = route.steps().getFirst();
      var wanted = number == 1 ? "a" : "c";
      assertThat(first.acquired()).containsExactly(number);
      assertThat(first.chance().probability()).isEqualTo(1.0);
      assertThat(first.expectedUses()).isEqualTo(number == 1 ? 6d : 3d);
      assertThat(generated.states().get(route.stateIndices().getFirst()).explicits())
          .extracting(ModifierInstance::modifierId)
          .containsExactly(wanted);
      var replay =
          RecipeRoutePlanner.evaluateFixed(
              bench, start, goals, both, 18000, true, null, route.steps());
      assertThat(replay.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps()).isEqualTo(route.steps());
                assertThat(r.probability()).isEqualTo(route.probability());
              });
    }
  }

  /** 하나의 실제 티어가 두 번호에 속해도 의도한 번호에 따라 반복의 분모와 적중 후 상태를 따로 보관한다. */
  @Test
  void simultaneouslyAcquiredNumbersKeepTheirDistinctRepeatIntentInGradeReplay() {
    var bench = threeOptionWorkbench();
    var low = List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L));
    var options =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of("a", low, "c", low)),
                new RouteCards.Option(2, Map.of("a", low))),
            0);
    var generated =
        RecipeRoutePlanner.calculate(bench, start, List.of(options), goal, 18000, true, null);
    var repeats =
        generated.routes().stream()
            .filter(r -> r.steps().size() == 1)
            .filter(r -> r.steps().getFirst().repeatUntilHit())
            .toList();
    assertThat(repeats)
        .extracting(r -> r.steps().getFirst().repeatTarget())
        .containsExactlyInAnyOrder(1, 2);
    for (var path : repeats) {
      var step = path.steps().getFirst();
      assertThat(step.acquired()).containsExactly(1, 2);
      assertThat(step.chance().probability()).isEqualTo(1.0);
      boolean firstNumber = step.repeatTarget() == 1;
      assertThat(step.expectedUses()).isEqualTo(firstNumber ? 2d : 6d);
      assertThat(path.probability()).isCloseTo(firstNumber ? 1.0 / 3 : 1.0, offset(1e-12));
      assertThat(step.reachChance().probability()).isEqualTo(path.probability());
      var replay =
          RecipeRoutePlanner.evaluateFixed(
              bench, start, options, goal, 18000, true, null, List.of(step));
      assertThat(replay.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps()).containsExactly(step);
                assertThat(r.probability()).isEqualTo(path.probability());
              });
    }
  }

  /** 같은 우선순위 목표 둘을 함께 탐색해도 각 경로에는 그 경로에서 획득한 번호의 기대량을 붙인다. */
  @Test
  void computesTheCostForEachAcquiredOptionInTheReturnedRoute() {
    var bench = threeOptionWorkbench();
    var goals =
        new RouteCards.Card(
            List.of(
                card(RouteCards.Frequency.M).options().getFirst(),
                new RouteCards.Option(
                    2,
                    Map.of(
                        "c", List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))))),
            0);
    for (int first : List.of(1, 2)) {
      var chaos =
          new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(first), List.of(), null, 1000);
      var exalt =
          new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(3 - first), List.of(), null);
      var result =
          RecipeRoutePlanner.evaluateFixed(
              bench,
              start,
              goals,
              s ->
                  s.explicits().stream()
                      .map(ModifierInstance::modifierId)
                      .toList()
                      .containsAll(List.of("a", "c")),
              1000,
              true,
              null,
              List.of(chaos, exalt));
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              r ->
                  assertThat(r.steps().getFirst().expectedUses())
                      .isCloseTo(first == 1 ? 6 : 3, offset(1e-12)));
    }
    var medium = List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M));
    var overlapping =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of("a", medium, "c", medium)),
                new RouteCards.Option(2, Map.of("a", medium))),
            0);
    var both =
        new RecipeRoutePlanner.Step("CHAOS", List.of(), List.of(1, 2), List.of(), null, 1000);
    var simultaneous =
        RecipeRoutePlanner.evaluateFixed(
            bench, start, overlapping, goal, 1000, true, null, List.of(both));
    assertThat(simultaneous.routes())
        .singleElement()
        .satisfies(
            r -> assertThat(r.steps().getFirst().expectedUses()).isCloseTo(6, offset(1e-12)));
  }

  /** 일반 태양 목걸이에서 진화·제왕·소멸로 만든 희귀 한 칸의 완벽 카오스 비용은 해당 등급의 전체 풀을 쓴다. */
  @Test
  void estimatesPerfectChaosAfterNormalSolarUpgradesAndAnnulment() {
    var actual = new RecipeTransitionsTest();
    var normal = SolarAmulet.initial(actual.catalog, 82, 15);
    String target = "amulet:suffix:of-battle";
    var goalCard =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        target,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L))))),
            0);
    var steps =
        List.of(
            new RecipeRoutePlanner.Step("TRANSMUTATION", List.of(), List.of(), List.of(), null),
            new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(), List.of(), null),
            new RecipeRoutePlanner.Step("ANNULMENT", List.of(), List.of(), List.of(), null),
            new RecipeRoutePlanner.Step(
                "PERFECT_CHAOS", List.of(), List.of(1), List.of(), null, 1000));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            actual.workbench,
            normal,
            goalCard,
            s -> s.explicits().stream().anyMatch(m -> m.modifierId().equals(target)),
            100000,
            true,
            null,
            steps);
    var pool =
        actual.workbench.pool(
            RecipeStateGroups.withAffixes(normal, ItemState.Rarity.RARE, List.of()),
            WorkbenchCurrency.PERFECT_CHAOS,
            null);
    double p =
        (double) actual.catalog.find(target).orElseThrow().weight()
            / pool.stream().mapToLong(ModifierDefinition::weight).sum();
    double estimate = -Math.expm1(1000 * Math.log1p(-p)) / p;
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              var beforeChaos = result.states().get(r.stateIndices().get(2));
              assertThat(beforeChaos.rarity()).isEqualTo(ItemState.Rarity.RARE);
              assertThat(beforeChaos.explicits()).hasSize(1);
              assertThat(beforeChaos.implicits()).isEqualTo(normal.implicits());
              assertThat(r.steps().getLast().expectedUses()).isCloseTo(estimate, offset(1e-10));
            });
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
              assertThat(first.reachChance().probability()).isCloseTo(cumulative, offset(1e-14));
              assertThat(first.expectedUses()).isCloseTo(2000 * cumulative, offset(1e-10));
              assertThat(r.probability()).isCloseTo(cumulative, offset(1e-14));
            });
  }
}
