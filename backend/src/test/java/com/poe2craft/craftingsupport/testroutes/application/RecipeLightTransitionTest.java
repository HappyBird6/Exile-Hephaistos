package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 제작지원 테스트에서 훼손 준비와 공개 대상의 티어·실제 수치 분포를 확인한다. */
class RecipeLightTransitionTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  /** 서로 충돌하지 않는 접두 둘과 접미 셋을 실제 카탈로그에서 골라 마지막 접두를 비운다. */
  ItemState fiveModifiers() {
    var stats =
        List.of(
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
                        .min(Comparator.comparingInt(ModifierDefinition::requiredItemLevel))
                        .orElseThrow()
                        .id())
            .toArray(String[]::new);
    return fixture.item(ItemState.Rarity.RARE, ids);
  }

  /** 지원 경로에서는 미공개·공개 슬롯 어느 쪽에도 빛의 징조로 소멸하는 후속 행동을 추가하지 않는다. */
  @Test
  void lightDoesNotResetAHiddenOrRevealedSlot() {
    var start = fiveModifiers();
    var targets = Set.of(fixture.life);
    var hidden =
        fixture
            .law
            .prepareDesecration(start, "Preserved_Collarbone", Set.of(), targets, 1)
            .outcomes()
            .getFirst()
            .state();
    var revealed =
        fixture
            .law
            .successors(hidden, "REVEAL", Set.of(), targets, 100)
            .outcomes()
            .getFirst()
            .state();
    for (var state : List.of(hidden, revealed)) {
      var law = fixture.law.successors(state, "ANNULMENT", Set.of(WorkbenchOmen.LIGHT), targets, 1);
      assertThat(law.complete()).isTrue();
      assertThat(law.removableModifierIds()).isEmpty();
      assertThat(law.outcomes()).isEmpty();
    }
  }

  /** 빈 접두에 놓인 훼손 슬롯은 다음 단계에서 선택한 목표로 공개된다. 실제 수치의 확률 질량 합이 1이다. */
  @Test
  void eligibleTargetHasUnitMassSpreadOverActualRolls() {
    var start = fiveModifiers();
    var targets = Set.of(fixture.life);
    var preparation =
        fixture.law.prepareDesecration(start, "Preserved_Collarbone", Set.of(), targets, 1);
    assertThat(preparation.complete()).isTrue();
    var hidden = preparation.outcomes().getFirst().state();
    assertThat(preparation.outcomes()).hasSize(1);
    assertThat(preparation.outcomes().getFirst().probability()).isEqualTo(Fraction.ONE);
    assertThat(hidden.explicits()).isEqualTo(start.explicits());
    assertThat(hidden.desecrated())
        .singleElement()
        .satisfies(
            slot -> {
              assertThat(slot.modifierId()).isNull();
              assertThat(slot.side()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
            });
    var reveal = fixture.law.successors(hidden, "REVEAL", Set.of(), targets, 100);
    assertThat(reveal.complete()).isTrue();
    var range = fixture.catalog.find(fixture.life).orElseThrow().stats().getFirst();
    long count = range.max() - range.min() + 1;
    assertThat(count).isGreaterThan(1);
    assertThat(reveal.outcomes()).hasSize((int) count);
    assertThat(
            reveal.outcomes().stream()
                .map(RecipeTransitions.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    var values = new TreeSet<Long>();
    for (var outcome : reveal.outcomes()) {
      var item = outcome.state();
      assertThat(item.explicits()).containsAll(start.explicits());
      assertThat(item.desecrated().getFirst().modifierId()).isEqualTo(fixture.life);
      var actual =
          item.explicits().stream()
              .filter(m -> m.modifierId().equals(fixture.life))
              .findFirst()
              .orElseThrow();
      values.add(actual.values().get(range.id()));
      assertThat(outcome.probability()).isEqualTo(Fraction.of(1, count));
    }
    assertThat(values).hasSize((int) count).contains(range.min(), range.max());
    assertThat(
            fixture
                .law
                .successors(hidden, "REVEAL", Set.of(), Set.of(fixture.strength), 100)
                .outcomes())
        .isEmpty();
    assertThat(
            fixture
                .law
                .prepareDesecration(hidden, "Preserved_Collarbone", Set.of(), targets, 1)
                .outcomes())
        .isEmpty();
    assertThat(
            fixture
                .law
                .prepareDesecration(
                    start, "Preserved_Collarbone", Set.of(), Set.of(fixture.strength), 1)
                .outcomes())
        .isEmpty();
  }
}
