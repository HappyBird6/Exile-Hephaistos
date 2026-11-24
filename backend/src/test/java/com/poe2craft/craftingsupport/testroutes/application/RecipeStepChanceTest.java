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
    assertThat(followed.routes()).hasSize(1);
    var route = followed.routes().getFirst();
    assertThat(route.steps())
        .extracting(RecipeRoutePlanner.Step::action)
        .containsExactly("PERFECT_REGAL", "EXALTED");
    assertThat(route.steps().getFirst().chance()).isNull();
    assertThat(route.order()).containsExactly(1);
    assertThat(followed.states().get(route.stateIndices().getFirst()).explicits())
        .noneMatch(m -> m.modifierId().equals(fixture.life));
    assertThat(followed.states().get(route.stateIndices().getLast()).explicits())
        .anyMatch(m -> m.modifierId().equals(fixture.life));
    var limited =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, goal, 1, true, null, List.of(topMiss, exalt));
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

  /** 훼손 공개의 적중은 단계에 존재하나 분포가 없어 확률을 알 수 없다. 순수 준비의 필드 생략과 구별한다. */
  @Test
  void unverifiedRevealIsAnUnknownChance() {
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
    var route = result.routes().getFirst();
    assertThat(route.steps().getFirst().chance()).isNotNull();
    assertThat(route.steps().getFirst().chance().probability()).isNull();
    assertThat(route.probability()).isNull();
  }

  /** 생명력의 두 허용 티어가 같은 단계 번호를 받으며 부분 겹치는 85~99의 모든 수치가 적중한다. */
  @Test
  void addsWholeTierProbabilitiesButOnlyCreditsSuccessfulFinalRolls() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var higher = "amulet:prefix:virile";
    var tiers = Map.of(fixture.life, List.of(base), higher, List.of(base));
    var card = new RouteCards.Card(List.of(new RouteCards.Option(1, tiers)), 0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state ->
                state.explicits().stream()
                    .anyMatch(m -> m.values().getOrDefault("base_maximum_life", 0L) >= 90),
            30000);
    var route =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 1
                        && r.steps().getFirst().action().equals("EXALTED")
                        && r.steps().getFirst().omens().isEmpty())
            .findFirst()
            .orElseThrow();
    long total =
        fixture.workbench.pool(start, WorkbenchCurrency.EXALTED, null).stream()
            .mapToLong(ModifierDefinition::weight)
            .sum();
    double lowerWeight = fixture.catalog.find(fixture.life).orElseThrow().weight();
    double higherWeight = fixture.catalog.find(higher).orElseThrow().weight();
    assertThat(route.steps().getFirst().chance().probability())
        .isCloseTo((lowerWeight + higherWeight) / total, offset(1e-14));
    assertThat(route.probability())
        .isCloseTo((lowerWeight * (10d / 15) + higherWeight) / total, offset(1e-14));
  }

  /** 일반 승급 에센스는 정해진 티어에 적중하고, 미달 상태에서만 마지막 Divine을 사용한다. */
  @Test
  void countsDivineFromUnfinishedStatesOnly() {
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
    assertThat(route.steps().getLast().expectedUses()).isEqualTo(1.5);
    assertThat(route.steps().getLast().maxUses()).isEqualTo(1000);
    assertThat(route.probability())
        .isCloseTo((5d / 15) * (1 - Math.pow(5d / 15, 1000)), offset(1e-14));
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
    assertThat(route.steps().getFirst().chance()).isNull();
    assertThat(route.steps().getLast().chance().probability()).isCloseTo(chance, offset(1e-14));
    assertThat(route.probability()).isCloseTo(chance, offset(1e-14));
  }
}
