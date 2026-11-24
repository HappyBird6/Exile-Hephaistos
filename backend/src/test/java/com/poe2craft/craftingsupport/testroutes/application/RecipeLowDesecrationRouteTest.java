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

  /** 다섯 시작 옵션과 훼손으로 획득 가능한 여섯 번째 희소 티어로 같은 목표를 검증한다. */
  private RecipeRoutePlanner.Result calculate(ItemState start) {
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
    return RecipeRoutePlanner.calculate(
        fixture.workbench,
        start,
        List.of(new RouteCards.Card(options, 0)),
        item ->
            item.explicits().containsAll(five.explicits())
                && item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
        20000,
        true,
        null);
  }

  /** 정확한 화폐·징조 순서와 매 단계 직후의 실제 상태를 검사한다. */
  private void verifyFilm(RecipeRoutePlanner.Result result, String... actions) {
    var route =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().stream()
                            .map(RecipeRoutePlanner.Step::action)
                            .toList()
                            .equals(List.of(actions))
                        && r.steps().stream()
                            .filter(s -> s.action().equals("ANNULMENT"))
                            .allMatch(s -> s.omens().equals(List.of(WorkbenchOmen.LIGHT.id()))))
            .findFirst()
            .orElseThrow();
    assertThat(route.order()).containsExactly(6);
    assertThat(route.probability()).isNull();
    assertThat(route.stateIndices()).hasSameSizeAs(route.steps());
    for (int i = 0; i < route.steps().size(); i++) {
      var step = route.steps().get(i);
      var item = result.states().get(route.stateIndices().get(i));
      assertThat(item.explicits()).containsAll(five.explicits());
      assertThat(step.lost()).isEmpty();
      if (i == route.steps().size() - 1) {
        assertThat(step.acquired()).containsExactly(6);
        assertThat(item.desecrated())
            .singleElement()
            .satisfies(slot -> assertThat(slot.modifierId()).isEqualTo(fixture.life));
        assertThat(step.chance()).isNotNull();
        assertThat(step.chance().probability()).isNull();
        assertThat(step.maxUses()).isEqualTo(DivineRepeatEstimate.MAX_USES);
        assertThat(step.expectedUses()).isNull();
      } else {
        assertThat(step.acquired()).isEmpty();
        switch (step.action()) {
          case "ANNULMENT" -> assertThat(item).isEqualTo(five);
          case BONE -> {
            assertThat(item.explicits()).isEqualTo(five.explicits());
            assertThat(item.desecrated())
                .singleElement()
                .satisfies(
                    slot -> {
                      assertThat(slot.side()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
                      assertThat(slot.modifierId()).isNull();
                    });
          }
          case "REVEAL" -> {
            assertThat(item.desecrated())
                .singleElement()
                .satisfies(
                    slot -> {
                      assertThat(slot.modifierId()).isNotNull().isNotEqualTo(fixture.life);
                      assertThat(item.explicits())
                          .anyMatch(m -> m.modifierId().equals(slot.modifierId()));
                    });
          }
          default -> throw new AssertionError(step.action());
        }
      }
    }
  }

  /** 다섯 옵션에서 곧장 적중하는 경우와 첫 공개가 빗나간 뒤 다시 적중하는 경우의 중간 필름을 구별한다. */
  @Test
  void recordsFirstSuccessAndOneActualMissBeforeRetrying() {
    var result = calculate(five);
    verifyFilm(result, BONE, "REVEAL");
    verifyFilm(result, BONE, "REVEAL", "ANNULMENT", BONE, "REVEAL");
  }

  /** 이미 잘못 공개된 여섯 번째 훼손 옵션에서 시작하면 그 옵션만 지운 뒤 같은 자리에서 목표를 공개한다. */
  @Test
  void restartsFromTheSameFiveAfterAReportedWrongReveal() {
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
    verifyFilm(calculate(start), "ANNULMENT", BONE, "REVEAL");
  }
}
