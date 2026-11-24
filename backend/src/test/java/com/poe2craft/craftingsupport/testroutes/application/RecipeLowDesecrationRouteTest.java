package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 경로 계산기가 반환한 필름에서 목표 다섯 개를 보존하며 여섯 번째 희소 옵션을 공개하는 각 실제 상태를 검증한다. */
class RecipeLowDesecrationRouteTest {
  private static final String BONE = "Preserved_Collarbone";
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final ItemState five = new RecipeLightTransitionTest().fiveModifiers();

  /** 다섯 시작 옵션과 훼손으로 획득 가능한 여섯 번째 희소 티어의 목표를 구성한다. */
  private RouteCards.Card card() {
    var options = new ArrayList<RouteCards.Option>();
    for (var modifier : five.explicits()) {
      options.add(
          new RouteCards.Option(
              options.size() + 1,
              Map.of(
                  modifier.modifierId(),
                  List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M)))));
    }
    options.add(
        new RouteCards.Option(
            6,
            Map.of(
                fixture.life,
                List.of(
                    new RouteCards.Source("B", null, RouteCards.Frequency.L),
                    new RouteCards.Source("D", BONE, null)))));
    return new RouteCards.Card(options, 0);
  }

  /** 수치 제한 없이 여섯 옵션을 얻는 모든 지원 경로를 계산한다. */
  private RecipeRoutePlanner.Result calculate(ItemState start) {
    return RecipeRoutePlanner.calculate(
        fixture.workbench,
        start,
        List.of(card()),
        item ->
            item.explicits().containsAll(five.explicits())
                && item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
        20000,
        true,
        null);
  }

  /** 공개 적중은 보장되어도 실제 최댓값이 함께 필요하면 해당 원시 굴림만 두 단계 완주의 확률에 기여한다. */
  @Test
  void fixedRevealSeparatesTheGuaranteedTierFromTheRequestedMaximum() {
    var value = fixture.catalog.find(fixture.life).orElseThrow().stats().getFirst();
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            five,
            card(),
            item ->
                item.explicits().containsAll(five.explicits())
                    && item.explicits().stream()
                        .anyMatch(
                            m ->
                                m.modifierId().equals(fixture.life)
                                    && m.values().get(value.id()) == value.max()),
            5000,
            false,
            null,
            List.of(
                new RecipeRoutePlanner.Step(BONE, List.of(), List.of(), List.of(), null),
                new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(6), List.of(), null)));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1d);
              assertThat(route.probability()).isEqualTo(1d / (value.max() - value.min() + 1));
              var finalItem = result.states().get(route.stateIndices().getLast());
              assertThat(finalItem.explicits())
                  .anyMatch(
                      m ->
                          m.modifierId().equals(fixture.life)
                              && m.values().get(value.id()) == value.max());
            });
  }

  /** 뼈와 공개의 두 상태를 보존하며 여섯 번째 목표를 한 번에 얻는다. */
  @Test
  void guaranteesTheEligibleTargetWithoutLightOrASecondReveal() {
    var result = calculate(five);
    assertThat(result.routes().stream().flatMap(r -> r.steps().stream()))
        .noneMatch(
            s ->
                s.omens().contains(WorkbenchOmen.LIGHT.id())
                    || (s.action().equals("REVEAL") && s.acquired().isEmpty()));
    var route =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().stream()
                        .map(RecipeRoutePlanner.Step::action)
                        .toList()
                        .equals(List.of(BONE, "REVEAL")))
            .findFirst()
            .orElseThrow();
    assertThat(route.probability()).isEqualTo(1d);
    assertThat(route.order()).containsExactly(6);
    assertThat(route.stateIndices()).hasSize(2);
    var prepared = result.states().get(route.stateIndices().getFirst());
    assertThat(prepared.explicits()).isEqualTo(five.explicits());
    assertThat(prepared.desecrated())
        .singleElement()
        .satisfies(
            slot -> {
              assertThat(slot.side()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
              assertThat(slot.modifierId()).isNull();
            });
    var revealed = result.states().get(route.stateIndices().getLast());
    assertThat(revealed.explicits()).containsAll(five.explicits());
    assertThat(revealed.desecrated().getFirst().modifierId()).isEqualTo(fixture.life);
    var step = route.steps().getLast();
    assertThat(step.acquired()).containsExactly(6);
    assertThat(step.lost()).isEmpty();
    assertThat(step.chance().probability()).isEqualTo(1d);
    assertThat(step.maxUses()).isNull();
    assertThat(step.expectedUses()).isNull();
  }

  /** 이미 존재하는 비목표 옵션 다섯 개와 기존 미공개 슬롯은 각각 실제 슬롯 상태를 기준으로 허용한다. */
  @Test
  void existingNonTargetsAndOpenRevealSlotUseTheSameGuaranteedLowTarget() {
    var target = new RouteCards.Option(1, card().options().getLast().tiers());
    var card = new RouteCards.Card(List.of(target), 0);
    var one = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    var hidden =
        DesecrationCurrency.apply(
                one, BONE, ModifierDefinition.AffixType.PREFIX, null, Set.of(), fixture.catalog)
            .storedItem();
    assertThat(new RecipeMoves(fixture.workbench).choices(one, card, Map.of()))
        .noneMatch(c -> c.move().action().equals(BONE));
    for (var start : List.of(five, hidden)) {
      var actual =
          RecipeRoutePlanner.calculate(
              fixture.workbench,
              start,
              List.of(card),
              item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
              20000,
              true,
              null);
      var expected = start.desecrated().isEmpty() ? List.of(BONE, "REVEAL") : List.of("REVEAL");
      var route =
          actual.routes().stream()
              .filter(
                  r ->
                      r.steps().stream()
                          .map(RecipeRoutePlanner.Step::action)
                          .toList()
                          .equals(expected))
              .findFirst()
              .orElseThrow();
      assertThat(route.order()).containsExactly(1);
      assertThat(route.probability()).isEqualTo(1d);
      assertThat(route.steps().getLast().chance().probability()).isEqualTo(1d);
      assertThat(actual.states().get(route.stateIndices().getLast()).explicits())
          .containsAll(start.explicits());
    }
  }

  /** 여섯 번째 훼손 슬롯의 옵션이 이미 공개되었으면 이를 다시 공개하거나 빛의 징조로 교체하지 않는다. */
  @Test
  void previouslyRevealedWrongSlotCannotBeResetWithLight() {
    var opaque =
        DesecrationCurrency.apply(
            five, BONE, ModifierDefinition.AffixType.PREFIX, null, Set.of(), fixture.catalog);
    var slot = opaque.slots().getLast().id();
    var wrongId =
        opaque.eligibleRevealIds(slot, fixture.catalog).stream()
            .filter(id -> !id.equals(fixture.life))
            .findFirst()
            .orElseThrow();
    var wrong = fixture.item(ItemState.Rarity.RARE, wrongId).explicits().getFirst();
    var start = opaque.selectDirect(slot, wrong, fixture.catalog).storedItem();
    var result = calculate(start);
    assertThat(result.routes().stream().flatMap(r -> r.steps().stream()))
        .noneMatch(
            s -> s.action().equals("REVEAL") || s.omens().contains(WorkbenchOmen.LIGHT.id()));
  }
}
