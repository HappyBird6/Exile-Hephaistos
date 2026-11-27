package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 미공개 접미를 예약한 태양 목걸이에서 실제로 분열된 옵션을 관측해 같은 베이스로 이어가는지 확인한다. */
class RecipeFracturePathTest {
  /** 세 공개 옵션을 가진 실제 목걸이와 적용 가능한 작업대다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  /** 알려진 각 옵션을 고정하는 독립 재생은 그 결과의 1/3만 가지고 마지막 공개를 수행한다. */
  @Test
  void replaysEachObservedFractureAndRejectsAnAbsentOption() {
    String cold = "amulet:suffix:of-the-seal";
    var before =
        fixture.item(ItemState.Rarity.RARE, fixture.life, fixture.strength, fixture.defences);
    var options = new ArrayList<RouteCards.Option>();
    for (var mod : before.explicits()) {
      var frequency =
          mod.modifierId().equals(fixture.life) ? RouteCards.Frequency.L : RouteCards.Frequency.M;
      options.add(
          new RouteCards.Option(
              options.size() + 1,
              Map.of(mod.modifierId(), List.of(new RouteCards.Source("B", null, frequency)))));
    }
    options.add(
        new RouteCards.Option(
            4, Map.of(cold, List.of(new RouteCards.Source("D", "Preserved_Collarbone", null)))));
    var card = new RouteCards.Card(options, 0);
    var bone =
        new RecipeRoutePlanner.Step(
            "Preserved_Collarbone",
            List.of("Omen_of_Dextral_Necromancy"),
            List.of(),
            List.of(),
            null);
    var draw = new RecipeRoutePlanner.Step("FRACTURING", List.of(), List.of(), List.of(), null);
    var reveal = new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(4), List.of(), null);
    assertThat(draw.validGradeSelection()).isFalse();
    assertThat(reveal.withFracturedModifier(fixture.life).validGradeSelection()).isFalse();

    for (String id : List.of(fixture.life, fixture.strength, fixture.defences, cold)) {
      var observed = draw.withFracturedModifier(id);
      assertThat(observed.validGradeSelection()).isTrue();
      var fixed =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              before,
              card,
              item -> item.explicits().stream().anyMatch(m -> cold.equals(m.modifierId())),
              3000,
              true,
              null,
              List.of(bone, observed, reveal),
              Set.of());
      if (id.equals(cold)) {
        assertThat(fixed.routes()).isEmpty();
        continue;
      }
      assertThat(fixed.routes())
          .singleElement()
          .satisfies(
              route -> {
                assertThat(route.probability()).isEqualTo(1.0 / 3);
                var step = route.steps().get(1);
                assertThat(step.fracturedModifierId()).isEqualTo(id);
                assertThat(step.chance().probability()).isEqualTo(1.0 / 3);
                assertThat(step.reachChance().probability()).isEqualTo(1.0 / 3);
                assertThat(route.fractureStrategy()).isNull();
                var after = fixed.states().get(route.stateIndices().getLast());
                assertThat(after.explicits())
                    .filteredOn(ModifierInstance::fractured)
                    .extracting(ModifierInstance::modifierId)
                    .containsExactly(id);
              });
    }
    // 공개 한 번으로 이미 완주하는 경우 분열로 비용만 늘어나므로 추천하지 않는다.
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            before,
            List.of(card),
            item -> item.explicits().stream().anyMatch(m -> cold.equals(m.modifierId())),
            3000,
            true,
            null);
    assertThat(result.states())
        .noneMatch(s -> s.explicits().stream().anyMatch(ModifierInstance::fractured));
    assertThat(result.routes())
        .isNotEmpty()
        .allMatch(route -> route.steps().stream().noneMatch(s -> s.fracturedModifierId() != null));
  }

  /** 같은 에센스 단계에서 서로 다른 더미가 제거되면 각자의 분열 분모를 임의의 예시 한 개로 정하지 않는다. */
  @Test
  void defersMixedFractureDecisionsAndKeepsTheVerifiedOrdinaryContinuation() {
    String spirit = "amulet:prefix:countess";
    String cold = "amulet:suffix:of-the-seal";
    var before =
        fixture.item(
            ItemState.Rarity.RARE,
            fixture.life,
            spirit,
            fixture.strength,
            "amulet:suffix:of-the-overseer");
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        fixture.life,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L)))),
                new RouteCards.Option(
                    2,
                    Map.of(
                        spirit, List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M)))),
                new RouteCards.Option(
                    3,
                    Map.of(
                        fixture.defences,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null)))),
                new RouteCards.Option(
                    4,
                    Map.of(
                        cold, List.of(new RouteCards.Source("D", "Preserved_Collarbone", null))))),
            0);
    var wanted = Set.of(fixture.life, spirit, fixture.defences, cold);
    java.util.function.Predicate<ItemState> matches =
        item ->
            item.explicits().stream()
                .map(ModifierInstance::modifierId)
                .toList()
                .containsAll(wanted);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, before, List.of(card), matches, 100_000, true, null);
    assertThat(result.trace().budgetSkippedStates).isZero();
    assertThat(result.complete()).isFalse();
    assertThat(result.routes())
        .anySatisfy(
            route -> {
              assertThat(route.probability()).isOne();
              assertThat(route.steps())
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("PERFECT_ESSENCE_ENHANCEMENT", "Preserved_Collarbone", "REVEAL");
              assertThat(route.fractureStrategy()).isNull();
            });

    // 한쪽 더미가 남은 절반의 경우에만 그 옵션으로 분열한다. 나머지 절반을 분모에서 빼지 않는다.
    var method =
        List.of(
            new RecipeRoutePlanner.Step(
                "PERFECT_ESSENCE_ENHANCEMENT",
                List.of("Omen_of_Dextral_Crystallisation"),
                List.of(3),
                List.of(),
                null),
            new RecipeRoutePlanner.Step("FRACTURING", List.of(), List.of(), List.of(), null)
                .withFracturedModifier(fixture.strength),
            new RecipeRoutePlanner.Step(
                "Preserved_Collarbone", List.of(), List.of(), List.of(), null),
            new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(4), List.of(), null));
    var fixed =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, before, card, matches, 3000, true, null, method, Set.of());
    assertThat(fixed.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().get(1).chance().probability()).isEqualTo(1.0 / 8);
              assertThat(route.probability()).isEqualTo(1.0 / 8);
            });
  }
}
