package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 실제 목걸이 카탈로그에서 보호·손실 확률, 회복과 최초 목표 달성 계약을 검사한다. */
class RecipeRoutePlannerTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final String life = fixture.life;
  private final String def = fixture.defences;
  private final String strength = fixture.strength;

  /** 같은 카드에 생명력 일반 추첨과 방어 교체 에센스를 배정한다. */
  private List<RouteCards.Card> cards(RouteCards.Frequency frequency) {
    return List.of(
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1, Map.of(life, List.of(new RouteCards.Source("B", null, frequency)))),
                new RouteCards.Option(
                    4,
                    Map.of(
                        def,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null))))),
            0));
  }

  /** 두 종류 모두 같은 아이템에 존재할 때만 성공이다. */
  private boolean matches(ItemState item) {
    var ids = item.explicits().stream().map(ModifierInstance::modifierId).toList();
    return ids.contains(life) && ids.contains(def);
  }

  /** 접미 제거로 보존할 수 있으면 무징조·접두 제거를 권하지 않는다. */
  @Test
  void lowOptionUsesAvailableProtection() {
    var start = fixture.item(ItemState.Rarity.RARE, life, strength);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, cards(RouteCards.Frequency.L), this::matches, 5000);
    var single = result.routes().stream().filter(r -> r.steps().size() == 1).toList();
    assertThat(single)
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.order()).containsExactly(4);
              assertThat(r.steps().getFirst().omens())
                  .containsExactly("Omen_of_Dextral_Crystallisation");
              assertThat(r.probability()).isEqualTo(1.0);
            });
  }

  /** 제거할 접미가 없을 때는 접미 더미부터 확보하여 보호하고, 준비에는 획득 번호를 붙이지 않는다. */
  @Test
  void preparesDisposableSuffixBeforeEssence() {
    var start = fixture.item(ItemState.Rarity.RARE, life, "amulet:prefix:adept-s");
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            cards(RouteCards.Frequency.L),
            this::matches,
            100000,
            true,
            null);
    assertThat(result.routes())
        .extracting(r -> r.steps().getFirst().action())
        .containsExactly("EXALTED");
    assertThat(result.routes())
        .allSatisfy(
            r -> {
              assertThat(r.steps()).hasSize(2);
              assertThat(r.order()).containsExactly(4);
              assertThat(r.steps().getFirst().acquired()).isEmpty();
              assertThat(r.steps().getLast().omens())
                  .containsExactly("Omen_of_Dextral_Crystallisation");
              assertThat(r.probability()).isEqualTo(1.0);
            });
  }

  /** 가득 찬 접두에서 제거가 불가피하면 L이 빠지는 질량은 실패이며 재획득 경로가 없다. */
  @Test
  void forcedLowLossStaysInDenominator() {
    var start =
        fixture.item(
            ItemState.Rarity.RARE, life, "amulet:prefix:adept-s", "amulet:prefix:azure", strength);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, cards(RouteCards.Frequency.L), this::matches, 5000);
    assertThat(result.routes())
        .isNotEmpty()
        .allSatisfy(
            r -> {
              assertThat(r.order()).containsExactly(4);
              assertThat(r.probability()).isEqualTo(2.0 / 3.0);
              assertThat(r.steps().getFirst().lost()).isEmpty();
            });
  }

  /** 시작 보유 M을 잃었다가 복구하면 4→1이며 보존 경로의 획득 순서는 4만 기록한다. */
  @Test
  void recoverMediumOptionInActualOrder() {
    var start = fixture.item(ItemState.Rarity.RARE, life, strength);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, cards(RouteCards.Frequency.M), this::matches, 12000);
    assertThat(result.routes()).anyMatch(r -> r.order().equals(List.of(4)));
    assertThat(result.routes())
        .anyMatch(
            r ->
                r.order().equals(List.of(4, 1))
                    && r.steps().getFirst().lost().equals(List.of(1))
                    && r.probability() != null
                    && r.probability() > 0);
  }

  /** 출처가 둘 다 B인 목걸이에서 우연히 먼저 붙은 번호를 기준으로 서로 다른 실제 경로를 만든다. */
  @Test
  void bothBaseAcquisitionOrdersHaveTheirOriginalProbability() {
    var start = fixture.item(ItemState.Rarity.RARE);
    var raw = new RouteCards.Source("B", null, RouteCards.Frequency.M);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(life, List.of(raw))),
                new RouteCards.Option(2, Map.of(strength, List.of(raw)))),
            0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state ->
                state.explicits().stream()
                    .map(ModifierInstance::modifierId)
                    .collect(java.util.stream.Collectors.toSet())
                    .containsAll(Set.of(life, strength)),
            100000,
            true,
            null);
    var basic =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 2
                        && r.steps().stream()
                            .allMatch(s -> s.action().equals("EXALTED") && s.omens().isEmpty()))
            .toList();
    assertThat(basic.stream().map(RecipeRoutePlanner.Route::order))
        .containsExactlyInAnyOrder(List.of(1, 2), List.of(2, 1));
    var total =
        fixture
            .workbench
            .pool(start, com.poe2craft.shared.crafting.domain.WorkbenchCurrency.EXALTED, null)
            .stream()
            .mapToLong(d -> d.weight())
            .sum();
    for (var route : basic) {
      String first = route.order().getFirst() == 1 ? life : strength;
      String second = first.equals(life) ? strength : life;
      var next = fixture.item(ItemState.Rarity.RARE, first);
      var after =
          fixture
              .workbench
              .pool(next, com.poe2craft.shared.crafting.domain.WorkbenchCurrency.EXALTED, null)
              .stream()
              .mapToLong(d -> d.weight())
              .sum();
      var expected =
          (double) fixture.catalog.find(first).orElseThrow().weight()
              / total
              * fixture.catalog.find(second).orElseThrow().weight()
              / after;
      assertThat(route.probability())
          .isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-15));
    }
  }

  /** 접두·접미 여섯 목표 중 보유한 네 개를 재획득하지 않고 남은 두 목표를 어느 쪽부터든 완성한다. */
  @Test
  void finishesSixOptionsFromFourAndFromEmpty() {
    var stats =
        List.of(
            "base_maximum_life",
            "base_maximum_mana",
            "base_maximum_energy_shield",
            "base_chaos_damage_resistance_%",
            "base_cold_damage_resistance_%",
            "base_fire_damage_resistance_%");
    var ids =
        stats.stream()
            .map(
                stat ->
                    fixture.catalog.modifiers().values().stream()
                        .filter(
                            d ->
                                d.weight() > 0
                                    && d.stats().size() == 1
                                    && d.stats().getFirst().id().equals(stat))
                        .sorted(Comparator.comparingInt(d -> d.requiredItemLevel()))
                        .findFirst()
                        .orElseThrow()
                        .id())
            .toList();
    var options = new ArrayList<RouteCards.Option>();
    for (int i = 0; i < ids.size(); i++)
      options.add(
          new RouteCards.Option(
              i + 1,
              Map.of(
                  ids.get(i), List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M)))));
    for (int count : List.of(4, 0)) {
      var start = fixture.item(ItemState.Rarity.RARE, ids.subList(0, count).toArray(String[]::new));
      var result =
          RecipeRoutePlanner.calculate(
              fixture.workbench,
              start,
              List.of(new RouteCards.Card(options, 0)),
              state ->
                  state.explicits().stream()
                      .map(ModifierInstance::modifierId)
                      .toList()
                      .containsAll(ids),
              5000,
              true,
              null);
      assertThat(result.routes())
          .as("held %s", count)
          .anyMatch(
              r -> r.order().size() == 6 - count && r.probability() != null && r.probability() > 0);
    }
  }

  /** 같은 단계에서 굴림이 갈라져도 표시 예시는 마지막 성공 상태로 실제로 이어진 부모 분기를 따른다. */
  @Test
  void witnessesReplayFromTheSameStartAndOnlyReferencedStatesAreReturned() {
    var start = fixture.item(ItemState.Rarity.MAGIC, strength);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        life, List.of(new RouteCards.Source("E", "GREATER_ESSENCE_BODY", null)))),
                new RouteCards.Option(
                    2,
                    Map.of(
                        def,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null))))),
            0);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(card),
            state ->
                state.explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(life) && m.values().containsValue(99L))
                    && state.explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(def) && m.values().containsValue(25L)),
            50000);
    var materialRoute =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 2
                        && r.steps().getFirst().action().equals("GREATER_ESSENCE_BODY")
                        && r.steps()
                            .getLast()
                            .omens()
                            .equals(List.of("Omen_of_Dextral_Crystallisation")))
            .findFirst()
            .orElseThrow();
    assertThat(materialRoute.probability()).isEqualTo(1.0 / 165);
    assertThat(materialRoute.stateIndices()).hasSameSizeAs(materialRoute.steps());
    ItemState previous = start;
    for (int i = 0; i < materialRoute.steps().size(); i++) {
      var step = materialRoute.steps().get(i);
      var next = result.states().get(materialRoute.stateIndices().get(i));
      var omens =
          step.omens().stream()
              .map(com.poe2craft.shared.crafting.domain.WorkbenchOmen::fromId)
              .collect(java.util.stream.Collectors.toSet());
      assertThat(
              fixture
                  .law
                  .successors(previous, step.action(), omens, Set.of(life, def), 1000, false)
                  .outcomes())
          .extracting(RecipeTransitions.Outcome::state)
          .contains(next);
      assertThat(next.explicits())
          .anyMatch(m -> m.modifierId().equals(life) && m.values().containsValue(99L));
      previous = next;
    }
    var used =
        result.routes().stream()
            .flatMap(r -> r.stateIndices().stream())
            .collect(java.util.stream.Collectors.toSet());
    assertThat(result.routes())
        .allSatisfy(r -> assertThat(r.stateIndices()).hasSameSizeAs(r.steps()));
    assertThat(result.states()).doesNotHaveDuplicates();
    assertThat(used)
        .containsExactlyInAnyOrderElementsOf(
            java.util.stream.IntStream.range(0, result.states().size()).boxed().toList());
    assertThat(result.routes().stream().mapToInt(r -> r.stateIndices().size()).sum())
        .isGreaterThan(result.states().size());
  }

  /** 이미 맞는 목표는 카드 중복과 관계없이 한 번만 반환한다. */
  @Test
  void returnsOneCompletedStart() {
    var start = fixture.item(ItemState.Rarity.RARE, life, def);
    var card = cards(RouteCards.Frequency.M).getFirst();
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(card, card), this::matches, 1);
    assertThat(result.routes())
        .containsExactly(new RecipeRoutePlanner.Route(0, List.of(), List.of(), 1.0, List.of()));
    assertThat(result.states()).isEmpty();
    assertThat(result.complete()).isTrue();
  }

  /** 옵션이 필요 없어도 목표 희귀도는 실제 제왕의 오브를 사용한 다음에 달성한다. */
  @Test
  void completesRarityOnlyGoalWithRealUpgrade() {
    var start = fixture.item(ItemState.Rarity.MAGIC, strength);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(new RouteCards.Card(List.of(), 0)),
            state -> state.rarity() == ItemState.Rarity.RARE,
            30000);
    assertThat(result.routes())
        .anySatisfy(
            r -> {
              assertThat(r.order()).isEmpty();
              assertThat(r.steps()).hasSize(1);
              assertThat(r.steps().getFirst().action()).isEqualTo("REGAL");
              assertThat(r.probability()).isEqualTo(1.0);
            });
  }
}
