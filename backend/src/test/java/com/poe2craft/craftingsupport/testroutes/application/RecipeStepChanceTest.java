package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 단계 적중은 실제 이전 상태에 조건을 걸고, 제작 목표 수치와 티어 적중을 별도로 검증한다. */
class RecipeStepChanceTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final RouteCards.Source base = new RouteCards.Source("B", null, RouteCards.Frequency.M);

  /** 미공개 훼손 접두가 한 칸을 차지한다. 마지막 칸의 목표는 획득하지만, 더미는 이후 교체 재료로 이어지지 않는다. */
  @Test
  void lastSlotExaltMissCannotPrepareAnEssence() {
    var baseItem =
        fixture.item(
            ItemState.Rarity.RARE,
            fixture.life,
            fixture.strength,
            "amulet:suffix:of-the-seal",
            "amulet:suffix:of-the-cloud");
    var mods = new java.util.ArrayList<>(baseItem.explicits());
    var life = mods.getFirst();
    mods.set(
        0, new com.poe2craft.shared.item.ModifierInstance(life.modifierId(), life.values(), true));
    var item =
        new ItemState(
                baseItem.snapshotId(),
                baseItem.baseItemId(),
                baseItem.itemLevel(),
                baseItem.rarity(),
                baseItem.implicits(),
                mods,
                baseItem.conditions())
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "hidden-prefix", ModifierDefinition.AffixType.PREFIX, null)));
    var slots = new com.poe2craft.shared.item.ItemStateValidator(fixture.catalog).slots(item);
    assertThat(slots.remainingPrefixes() + slots.remainingSuffixes()).isEqualTo(1);
    var craft = new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(base))),
                new RouteCards.Option(2, Map.of(fixture.defences, List.of(craft)))),
            0);
    var dummy =
        new RecipeRoutePlanner.Step(
            "EXALTED", List.of("Omen_of_Sinistral_Exaltation"), List.of(), List.of(), null);
    var replace =
        new RecipeRoutePlanner.Step(
            craft.id(), List.of("Omen_of_Sinistral_Crystallisation"), List.of(2), List.of(), null);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            item,
            card,
            s -> s.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.defences)),
            10000,
            true,
            null,
            List.of(dummy, replace));
    assertThat(result.routes()).isEmpty();
    var mana = "amulet:prefix:azure";
    var ordinary =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(base))),
                new RouteCards.Option(2, Map.of(mana, List.of(base)))),
            0);
    var hit = new RecipeRoutePlanner.Step("EXALTED", dummy.omens(), List.of(2), List.of(), null);
    var successful =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            item,
            ordinary,
            s -> s.explicits().stream().anyMatch(m -> m.modifierId().equals(mana)),
            10000,
            true,
            null,
            List.of(hit));
    assertThat(successful.routes()).hasSize(1);
    assertThat(successful.routes().getFirst().probability()).isBetween(0.0, 1.0);
  }

  /** 엑잘로 노린 목표는 바로 다음 에센스로 희생하지 않는다. 같은 제거 풀의 다른 옵션이 빠지는 확률은 그대로다. */
  @Test
  void fixedEssenceKeepsTheNewExaltedHitWithItsOriginalRemovalProbability() {
    var item = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:adept-s");
    var essence = new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(base))),
                new RouteCards.Option(2, Map.of(fixture.defences, List.of(essence)))),
            0);
    var keep =
        new RecipeRoutePlanner.Step(
            essence.id(),
            List.of("Omen_of_Sinistral_Crystallisation"),
            List.of(2),
            List.of(),
            null);
    var sacrifice =
        new RecipeRoutePlanner.Step(keep.action(), keep.omens(), List.of(2), List.of(1), null);
    var recover = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    java.util.function.Predicate<ItemState> done =
        state ->
            state.explicits().stream()
                .map(m -> m.modifierId())
                .toList()
                .containsAll(List.of(fixture.life, fixture.defences));
    for (String action : List.of("EXALTED", "GREATER_EXALTED")) {
      var draw =
          new RecipeRoutePlanner.Step(
              action,
              List.of("Omen_of_Sinistral_Exaltation"),
              List.of(1),
              List.of(),
              null,
              null,
              action.equals("EXALTED") ? null : "EXALTED",
              false);
      var retained =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench, item, card, done, 1000, true, null, List.of(draw, keep));
      var route = retained.routes().getFirst();
      assertThat(route.steps().getLast().chance().probability()).isEqualTo(0.5);
      var firstReach = route.steps().getFirst().reachChance().probability();
      assertThat(firstReach).isEqualTo(route.steps().getFirst().chance().probability());
      assertThat(route.steps().getLast().reachChance().probability()).isEqualTo(firstReach * 0.5);
      assertThat(route.probability())
          .isEqualTo(route.steps().getLast().reachChance().probability());
      assertThat(retained.states().get(route.stateIndices().getLast()).explicits())
          .extracting(m -> m.modifierId())
          .containsExactlyInAnyOrder(fixture.life, fixture.defences);
      // 같은 에센스 적중이 중간 단계인 경우에도 후속 신성 결과를 앞 단계에 곱하지 않는다.
      var adjusted =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              item,
              card,
              s ->
                  done.test(s)
                      && s.explicits().stream()
                          .anyMatch(
                              m ->
                                  m.modifierId().equals(fixture.life)
                                      && m.values().get("base_maximum_life") >= 90),
              30000,
              false,
              null,
              List.of(
                  draw,
                  keep,
                  new RecipeRoutePlanner.Step("DIVINE", List.of(), List.of(), List.of(), null)));
      var withDivine = adjusted.routes().getFirst();
      assertThat(withDivine.steps().get(1).reachChance())
          .isEqualTo(route.steps().getLast().reachChance());
      assertThat(withDivine.steps().getLast().reachChance().probability())
          .isEqualTo(withDivine.probability());
      assertThat(withDivine.steps().getLast().chance().probability())
          .isCloseTo(10d / 15, offset(1e-14));
      var lost =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              item,
              card,
              done,
              1000,
              true,
              null,
              List.of(draw, sacrifice, recover));
      assertThat(lost.routes()).isEmpty();
    }
    var alreadyHeld = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:adept-s", fixture.life);
    var recovered =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            alreadyHeld,
            card,
            done,
            1000,
            true,
            null,
            List.of(sacrifice, recover));
    var route = recovered.routes().getFirst();
    assertThat(route.order()).containsExactly(2, 1);
    assertThat(route.steps().getFirst().chance().probability()).isEqualTo(0.5);
  }

  /** 한 티어의 15가지 값은 획득에서 한 경계 상태다. 작은 예산에서도 전체 제왕 후보 풀의 원래 가중치를 유지한다. */
  @Test
  void aSingleBoundaryStateKeepsTheWholeTierMassWithASmallBudget() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(base)))), 0);
    var selected = new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(1), List.of(), null);
    // 제왕이 여는 희귀 접미까지 분모에 포함한다. 마법 시작 상태의 1/1 슬롯 제한은 이미 풀린다.
    var upgraded = RecipeStateGroups.withAffixes(start, ItemState.Rarity.RARE, start.explicits());
    var pool = fixture.workbench.pool(upgraded, WorkbenchCurrency.REGAL, null);
    long total = pool.stream().mapToLong(ModifierDefinition::weight).sum();
    double expected = (double) fixture.catalog.find(fixture.life).orElseThrow().weight() / total;
    assertThat(expected).isBetween(0.0, 1.0);
    for (int budget : List.of(5, 100)) {
      var result =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              start,
              card,
              item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
              budget,
              false,
              null,
              List.of(selected));
      assertThat(result.complete()).isTrue();
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              route -> {
                assertThat(route.probability()).isCloseTo(expected, offset(1e-15));
                assertThat(route.steps().getFirst().chance().probability())
                    .isEqualTo(route.probability());
                assertThat(route.probabilityBounds()).isNull();
                assertThat(selectedLife(result, route, 0)).isEqualTo(85);
              });
    }
  }

  /** 무징조 단회 카오스는 선택 등급이 바뀌어도 기존 세션의 고정 재생 후보가 아니다. */
  @Test
  void unmarkedSingleChaosCannotBeReplayedAsAnAcquisition() {
    var item = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(base)))), 0);
    for (var name : List.of("CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS")) {
      var step = new RecipeRoutePlanner.Step(name, List.of(), List.of(1), List.of(), null);
      var replay =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              item,
              card,
              state ->
                  state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
              10000,
              true,
              null,
              List.of(step));
      assertThat(replay.routes()).isEmpty();
      assertThat(replay.complete()).isTrue();
    }
  }

  /** 기본 경로의 이후 단계는 유지하고, 요청한 한 등급의 작업대 풀로만 확률과 예시를 재검증한다. */
  @Test
  void replaysOnlyTheRequestedGradeAndObservationSequence() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(base)))), 0);
    java.util.function.Predicate<ItemState> goal =
        state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life));
    var highHit =
        new RecipeRoutePlanner.Step("GREATER_REGAL", List.of(), List.of(1), List.of(), null);
    var direct =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 10000, true, null, List.of(highHit));
    assertThat(direct.routes()).hasSize(1);
    assertThat(direct.complete()).isTrue();
    var selected = direct.routes().getFirst();
    assertThat(selected.steps()).hasSize(1);
    assertThat(selected.steps().getFirst().action()).isEqualTo("GREATER_REGAL");
    var higherLaw =
        new RecipeTransitions(fixture.workbench, true)
            .successors(start, "GREATER_REGAL", Set.of(), Set.of(fixture.life), 1000, false);
    var expected = higherLaw.outcomes().getFirst().probability();
    assertThat(selected.probability())
        .isCloseTo(
            expected.numerator().doubleValue() / expected.denominator().doubleValue(),
            offset(1e-14));
    assertThat(direct.states().get(selected.stateIndices().getFirst()).explicits())
        .anyMatch(m -> m.modifierId().equals(fixture.life));
    var topHit =
        new RecipeRoutePlanner.Step("PERFECT_REGAL", List.of(), List.of(1), List.of(), null);
    var cannotHit =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 10000, true, null, List.of(topHit));
    assertThat(cannotHit.routes()).isEmpty();
    assertThat(cannotHit.complete()).isTrue();
    var topMiss =
        new RecipeRoutePlanner.Step("PERFECT_REGAL", List.of(), List.of(), List.of(), null);
    var exalt = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    var followed =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 10000, true, null, List.of(topMiss, exalt));
    assertThat(followed.routes()).isEmpty();
    var changedDummy =
        new RecipeRoutePlanner.Step(
            "GREATER_REGAL", List.of(), List.of(), List.of(), null, null, "REGAL");
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    fixture.workbench,
                    start,
                    card,
                    goal,
                    10000,
                    true,
                    null,
                    List.of(changedDummy, exalt))
                .routes())
        .isEmpty();
    var ordinaryMiss = new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(), List.of(), null);
    var prepared =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 10000, true, null, List.of(ordinaryMiss, exalt));
    assertThat(prepared.routes()).hasSize(1);
    var route = prepared.routes().getFirst();
    assertThat(route.steps())
        .extracting(RecipeRoutePlanner.Step::action)
        .containsExactly("REGAL", "EXALTED");
    assertThat(route.steps().getFirst().dummy()).isTrue();
    assertThat(route.steps().getFirst().chance().probability()).isEqualTo(1);
    assertThat(route.order()).containsExactly(1);
    assertThat(prepared.states().get(route.stateIndices().getFirst()).explicits())
        .noneMatch(m -> m.modifierId().equals(fixture.life));
    assertThat(prepared.states().get(route.stateIndices().getLast()).explicits())
        .anyMatch(m -> m.modifierId().equals(fixture.life));
    var limited =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 1, true, null, List.of(ordinaryMiss, exalt));
    assertThat(limited.routes()).isEmpty();
    assertThat(limited.complete()).isFalse();
  }

  /** 고정 재생도 분열 이외의 L 보호 징조가 가능한 상태에서 무작위 제거를 받아들이지 않는다. */
  @Test
  void fixedReplayUsesTheSameLowProtectionDecision() {
    var start = fixture.item(ItemState.Rarity.RARE, fixture.life, fixture.strength);
    var low = new RouteCards.Source("B", null, RouteCards.Frequency.L);
    var craft = new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(low))),
                new RouteCards.Option(2, Map.of(fixture.defences, List.of(craft)))),
            0);
    java.util.function.Predicate<ItemState> goal =
        state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.defences));
    var unsafe = new RecipeRoutePlanner.Step(craft.id(), List.of(), List.of(2), List.of(), null);
    var safe =
        new RecipeRoutePlanner.Step(
            craft.id(), List.of("Omen_of_Dextral_Crystallisation"), List.of(2), List.of(), null);
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    fixture.workbench, start, card, goal, 500, true, null, List.of(unsafe))
                .routes())
        .isEmpty();
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 500, true, null, List.of(safe));
    assertThat(result.routes().getFirst().probability()).isEqualTo(1);
  }

  /** 뼈가 두 방향 중 맞는 방향을 여는 경우에만 원하는 공개 옵션이 붙는다. */
  @Test
  void guaranteedDesecrationKeepsTheBonesSideProbability() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var source = new RouteCards.Source("D", "Preserved_Collarbone", null);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(source)))), 0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            1000,
            true,
            null);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              var bone = route.steps().getFirst();
              assertThat(bone.omens()).containsExactly("Omen_of_Sinistral_Necromancy");
              assertThat(bone.chance().probability()).isOne();
              assertThat(route.probability()).isOne();
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1);
            });
  }

  /** 어느 옵션을 수동 선택할지는 독립된 제작 의도다. 같은 슬롯에서 둘 중 하나를 고르는 두 경로를 반반으로 나누지 않는다. */
  @Test
  void choosingADifferentDesecrationGoalDoesNotDiluteTheSelectedOne() {
    var start =
        fixture
            .item(ItemState.Rarity.RARE)
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "open", ModifierDefinition.AffixType.PREFIX, null)));
    String mana = "amulet:prefix:azure";
    var source = new RouteCards.Source("D", "Preserved_Collarbone", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.life, List.of(source, base))),
                new RouteCards.Option(2, Map.of(mana, List.of(source, base)))),
            0);
    java.util.function.Predicate<ItemState> done =
        s ->
            s.explicits().stream()
                .map(m -> m.modifierId())
                .toList()
                .containsAll(List.of(fixture.life, mana));
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(card), done, 12000, true, null);
    for (int selected : List.of(1, 2)) {
      var first =
          new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(3 - selected), List.of(), null);
      var second =
          new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(selected), List.of(), null);
      var route =
          found.routes().stream()
              .filter(
                  r ->
                      r.steps().size() == 2
                          && r.steps().getFirst().action().equals("EXALTED")
                          && r.steps().getFirst().acquired().equals(first.acquired())
                          && r.steps().getLast().action().equals("REVEAL")
                          && r.steps().getFirst().omens().isEmpty())
              .findFirst()
              .orElseThrow();
      var fixed =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench, start, card, done, 12000, true, null, List.of(first, second));
      assertThat(route.steps().getLast().chance().probability()).isEqualTo(1);
      assertThat(fixed.routes())
          .singleElement()
          .satisfies(
              r -> {
                assertThat(r.steps()).isEqualTo(route.steps());
                assertThat(r.probability()).isEqualTo(route.probability());
              });
    }
  }

  /** 목표 99에 대해 공개는 Rotund의 85로 시작하고, 실제 85~99 중 99를 굴릴 확률은 Divine에서만 쓴다. */
  @Test
  void guaranteedTierStartsAtItsBoundaryAndDivineRollsTheActualValues() {
    var start =
        fixture
            .item(ItemState.Rarity.RARE)
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "open", ModifierDefinition.AffixType.PREFIX, null)));
    var source = new RouteCards.Source("D", "Preserved_Collarbone", null);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(source)))), 0);
    java.util.function.Predicate<ItemState> done =
        s ->
            s.explicits().stream()
                .anyMatch(
                    m -> m.modifierId().equals(fixture.life) && m.values().containsValue(99L));
    var search =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(card), done, 10000, false, null);
    assertThat(search.routes()).noneMatch(r -> r.steps().size() == 1);
    var divine =
        search.routes().stream()
            .filter(r -> r.steps().size() == 2 && r.steps().getLast().action().equals("DIVINE"))
            .findFirst()
            .orElseThrow();
    assertThat(divine.steps().getFirst().chance().probability()).isEqualTo(1);
    assertThat(selectedLife(search, divine, 0)).isEqualTo(85);
    assertThat(selectedLife(search, divine, 1)).isEqualTo(99);
    assertThat(divine.steps().getLast().chance().probability()).isCloseTo(1.0 / 15, offset(1e-12));
    double completed = 1 - Math.pow(14.0 / 15, 1000);
    assertThat(divine.probability()).isCloseTo(completed, offset(1e-12));
    assertThat(divine.steps().getLast().expectedUses()).isCloseTo(15 * completed, offset(1e-12));
  }

  /** 공개에서 허용된 같은 계열의 두 티어는 Rotund 85로 모으고, 뼈가 여는 방향 확률은 보존한다. */
  @Test
  void revealCombinesEligibleTiersAtTheBoundaryAndPreservesBoneDirectionRisk() {
    var high = "amulet:prefix:virile";
    var d = new RouteCards.Source("D", "Preserved_Collarbone", null);
    var tiers = Map.of(fixture.life, List.of(d), high, List.of(d));
    var card = new RouteCards.Card(List.of(new RouteCards.Option(1, tiers)), 0);
    var bare = fixture.item(ItemState.Rarity.RARE);
    var hidden =
        bare.withDesecrated(
            List.of(
                new ItemState.DesecratedSlot("open", ModifierDefinition.AffixType.PREFIX, null)));
    java.util.function.Predicate<ItemState> any =
        s -> s.explicits().stream().anyMatch(m -> tiers.containsKey(m.modifierId()));
    for (var start : List.of(bare, hidden)) {
      var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(1), List.of(), null);
      var steps =
          start == bare
              ? List.of(
                  new RecipeRoutePlanner.Step(
                      "Preserved_Collarbone", List.of(), List.of(), List.of(), null),
                  reveal)
              : List.of(reveal);
      var fixed =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench, start, card, any, 20000, false, null, steps);
      assertThat(fixed.routes())
          .singleElement()
          .satisfies(
              r -> {
                double expected = start == bare ? 0.5 : 1.0;
                assertThat(r.steps().getFirst().chance().probability()).isEqualTo(expected);
                assertThat(r.probability()).isEqualTo(expected);
                assertThat(selectedLife(fixed, r, r.steps().size() - 1)).isEqualTo(85);
              });
    }
    var needs99 =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            hidden,
            card,
            s ->
                s.explicits().stream()
                    .anyMatch(m -> m.values().getOrDefault("base_maximum_life", 0L) >= 99L),
            20000,
            false,
            null,
            List.of(new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(1), List.of(), null)));
    assertThat(needs99.complete()).isTrue();
    assertThat(needs99.routes()).isEmpty();
  }

  /** 두 허용 티어의 가중치를 전부 합쳐 Rotund 85에 적립하고 Divine에서 90 이상일 실제 확률을 적용한다. */
  @Test
  void sumsBothTierWeightsAtTheLowerBoundaryBeforeDivine() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var higher = "amulet:prefix:virile";
    var tiers = Map.of(fixture.life, List.of(base), higher, List.of(base));
    var card = new RouteCards.Card(List.of(new RouteCards.Option(1, tiers)), 0);
    var exalted = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(1), List.of(), null);
    var divine = new RecipeRoutePlanner.Step("DIVINE", List.of(), List.of(), List.of(), null);
    java.util.function.Predicate<ItemState> needs90 =
        state ->
            state.explicits().stream()
                .anyMatch(m -> m.values().getOrDefault("base_maximum_life", 0L) >= 90);
    var direct =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, needs90, 30000, false, null, List.of(exalted));
    assertThat(direct.routes()).isEmpty();
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, needs90, 30000, false, null, List.of(exalted, divine));
    long total =
        fixture.workbench.pool(start, WorkbenchCurrency.EXALTED, null).stream()
            .mapToLong(ModifierDefinition::weight)
            .sum();
    long combined =
        fixture.catalog.find(fixture.life).orElseThrow().weight()
            + fixture.catalog.find(higher).orElseThrow().weight();
    double tierHit = (double) combined / total;
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().getFirst().chance().probability())
                  .isCloseTo(tierHit, offset(1e-14));
              assertThat(selectedLife(result, route, 0)).isEqualTo(85);
              assertThat(selectedLife(result, route, 1)).isBetween(90L, 99L);
              assertThat(route.steps().getLast().chance().probability())
                  .isCloseTo(10d / 15, offset(1e-14));
              assertThat(route.probability())
                  .isCloseTo(tierHit * (1 - Math.pow(5d / 15, 1000)), offset(1e-14));
            });
  }

  /** 일반 승급 에센스의 대표 수치 85는 항상 미달이므로 모든 질량이 마지막 Divine에 진입한다. */
  @Test
  void everyGuaranteedEssenceHitEntersDivineFromTheBoundary() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var essence = new RouteCards.Source("E", "GREATER_ESSENCE_BODY", null);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(essence)))), 0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state ->
                state.explicits().stream()
                    .anyMatch(m -> m.values().getOrDefault("base_maximum_life", 0L) >= 90),
            8000);
    var route =
        result.routes().stream()
            .filter(r -> r.steps().size() == 2 && r.steps().getLast().action().equals("DIVINE"))
            .findFirst()
            .orElseThrow();
    assertThat(route.steps().getFirst().chance().probability()).isEqualTo(1);
    assertThat(route.steps().getLast().chance().probability()).isCloseTo(10d / 15, offset(1e-14));
    assertThat(selectedLife(result, route, 0)).isEqualTo(85);
    assertThat(selectedLife(result, route, 1)).isBetween(90L, 99L);
    assertThat(route.steps().getLast().expectedUses()).isEqualTo(1.5);
    assertThat(route.steps().getLast().maxUses()).isEqualTo(1000);
    assertThat(route.probability()).isCloseTo(1 - Math.pow(5d / 15, 1000), offset(1e-14));
  }

  /** 반환한 경로의 지정 프레임에서 실제 Rotund 생명력 수치를 읽는다. 다른 허용 티어가 대신 들어가면 실패한다. */
  private long selectedLife(
      RecipeRoutePlanner.Result result, RecipeRoutePlanner.Route route, int step) {
    return result.states().get(route.stateIndices().get(step)).explicits().stream()
        .filter(m -> m.modifierId().equals(fixture.life))
        .findFirst()
        .orElseThrow()
        .values()
        .get("base_maximum_life");
  }

  /** 승급 중 목표를 빗나간 질량을 감안해 더미 각각에서의 다음 적중 확률을 가중평균한다. */
  @Test
  void uncountedUpgradeMissRetainsTheActualMixOfDummies() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(base)))), 0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            100000,
            true,
            null);
    var route =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 2
                        && r.steps().stream().allMatch(s -> s.omens().isEmpty())
                        && r.steps().getFirst().action().equals("REGAL")
                        && r.steps().getLast().action().equals("EXALTED"))
            .findFirst()
            .orElseThrow();
    var law = new RecipeTransitions(fixture.workbench, true);
    Fraction misses = Fraction.ZERO;
    Fraction hitAfterMiss = Fraction.ZERO;
    var upgrade = law.successors(start, "REGAL", Set.of(), Set.of(fixture.life), 10000, true);
    assertThat(upgrade.complete()).isTrue();
    for (var outcome : upgrade.outcomes()) {
      var state = outcome.state();
      if (state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life))) continue;
      misses = misses.add(outcome.probability());
      var pool = fixture.workbench.pool(state, WorkbenchCurrency.EXALTED, null);
      long all = pool.stream().mapToLong(ModifierDefinition::weight).sum();
      long wanted =
          pool.stream()
              .filter(d -> d.id().equals(fixture.life))
              .mapToLong(ModifierDefinition::weight)
              .sum();
      if (wanted > 0)
        hitAfterMiss = hitAfterMiss.add(outcome.probability().multiply(Fraction.of(wanted, all)));
    }
    var expected = hitAfterMiss.divide(misses);
    double chance = expected.numerator().doubleValue() / expected.denominator().doubleValue();
    assertThat(misses).isNotEqualTo(Fraction.ONE);
    assertThat(route.steps().getFirst().dummy()).isTrue();
    assertThat(route.steps().getFirst().chance().probability()).isEqualTo(1);
    assertThat(route.steps().getFirst().reachChance().probability()).isOne();
    assertThat(route.steps().getLast().chance().probability()).isCloseTo(chance, offset(1e-14));
    assertThat(route.probability()).isCloseTo(chance, offset(1e-14));
    var limited =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            state -> state.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            220,
            true,
            null,
            route.steps());
    var incomplete = limited.routes().getFirst();
    assertThat(incomplete.probability()).isNull();
    assertThat(incomplete.steps().getLast().reachChance().probability()).isNull();
    assertThat(incomplete.steps().getLast().reachChance().probabilityBounds())
        .isEqualTo(incomplete.probabilityBounds());
    assertThat(incomplete.probabilityBounds().lower()).isGreaterThan(0);
    assertThat(incomplete.probabilityBounds().upper()).isLessThan(1);
    assertThat(chance)
        .isBetween(incomplete.probabilityBounds().lower(), incomplete.probabilityBounds().upper());
    assertThat(chance)
        .isBetween(
            incomplete.steps().getLast().chance().probabilityBounds().lower(),
            incomplete.steps().getLast().chance().probabilityBounds().upper());
  }
}
