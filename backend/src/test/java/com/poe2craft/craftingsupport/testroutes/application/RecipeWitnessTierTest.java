package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 근접 스킬 레벨의 티어 합산 확률과 그 경로에서 실제로 얻은 대표 티어를 태양의 목걸이로 검증한다. */
class RecipeWitnessTierTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final String one = "amulet:suffix:of-combat";
  private final String two = "amulet:suffix:of-dueling";
  private final String three = "amulet:suffix:of-battle";

  /** 동일한 경로의 대표 아이템은 허용 티어 중 처음 등장하는 것을 사용하며 확률에는 가능한 모든 티어가 기여한다. */
  @Test
  void keepsTheEarliestEligibleTierAndTheProbabilityOfEveryEligibleTier() {
    var start = fixture.item(ItemState.Rarity.RARE);
    assertWitness(start, List.of(three, two, one), WorkbenchCurrency.EXALTED, one);
    assertWitness(start, List.of(three, two), WorkbenchCurrency.EXALTED, two);
    assertWitness(start, List.of(one), WorkbenchCurrency.EXALTED, one);
    assertWitness(start, List.of(three, two, one), WorkbenchCurrency.GREATER_EXALTED, two);
    assertWitness(start, List.of(three, two, one), WorkbenchCurrency.PERFECT_EXALTED, three);
  }

  /** 태양의 목걸이에서 진화로 얻은 +1을 그대로 지키고 완벽 에센스로 방어력을 얻는 실제 필름을 따른다. */
  @Test
  void theSolarAmuletFilmKeepsTheSameSuccessfulMeleeTierThroughTheEssence() {
    var raw = List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L));
    var defence = new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(1, Map.of(fixture.defences, List.of(defence))),
                new RouteCards.Option(2, Map.of(one, raw, two, raw, three, raw))),
            0);
    var start = com.poe2craft.shared.item.SolarAmulet.initial(fixture.catalog, 82, 15);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            item ->
                item.rarity() == ItemState.Rarity.RARE
                    && item.explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(fixture.defences))
                    && item.explicits().stream()
                        .anyMatch(m -> m.values().containsKey("melee_skill_gem_level_+")),
            100000,
            true,
            null,
            List.of(
                new RecipeRoutePlanner.Step(
                    "TRANSMUTATION", List.of(), List.of(2), List.of(), null),
                new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(), List.of(), null),
                new RecipeRoutePlanner.Step(
                    defence.id(),
                    List.of("Omen_of_Sinistral_Crystallisation"),
                    List.of(1),
                    List.of(),
                    null)));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.order()).containsExactly(2, 1);
              assertThat(route.stateIndices())
                  .hasSize(3)
                  .allSatisfy(
                      index ->
                          assertThat(result.states().get(index).explicits())
                              .anyMatch(m -> m.modifierId().equals(one)));
            });
  }

  /** 작업대의 등급별 전체 추첨 가중치로 경로 확률을 따로 계산하고, 저장할 마지막 아이템을 확인한다. */
  private void assertWitness(
      ItemState start, List<String> eligible, WorkbenchCurrency currency, String expectedTier) {
    var raw = new RouteCards.Source("B", null, RouteCards.Frequency.M);
    var tiers = new LinkedHashMap<String, List<RouteCards.Source>>();
    eligible.forEach(id -> tiers.put(id, List.of(raw)));
    var card = new RouteCards.Card(List.of(new RouteCards.Option(1, tiers)), 0);
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            s -> s.explicits().stream().anyMatch(m -> eligible.contains(m.modifierId())),
            10000,
            true,
            null,
            List.of(
                new RecipeRoutePlanner.Step(
                    currency.name(), List.of(), List.of(1), List.of(), null)));
    var pool = fixture.workbench.pool(start, currency, null);
    long total = pool.stream().mapToLong(ModifierDefinition::weight).sum();
    long hit =
        pool.stream()
            .filter(d -> eligible.contains(d.id()))
            .mapToLong(ModifierDefinition::weight)
            .sum();
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.probability()).isEqualTo((double) hit / total);
              var actual = result.states().get(r.stateIndices().getFirst());
              assertThat(actual.explicits())
                  .singleElement()
                  .satisfies(m -> assertThat(m.modifierId()).isEqualTo(expectedTier));
            });
  }
}
