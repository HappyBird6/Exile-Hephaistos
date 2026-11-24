package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 접두 한 칸만 비어 있는 희귀 목걸이에서 훼손 준비·공개·빛의 징조 소멸의 중간 상태를 확인한다. */
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

  /** 작업대의 소멸 결과와 같은 유일한 결과만 반환한다. 공개한 경우에만 제거된 수정자 ID를 기록한다. */
  @Test
  void removesOnlyTheSingleDesecratedSlotWhetherHiddenOrRevealed() {
    var start = fiveModifiers();
    var hidden =
        DesecrationCurrency.apply(
            start,
            "Preserved_Collarbone",
            ModifierDefinition.AffixType.PREFIX,
            null,
            Set.of(),
            fixture.catalog);
    String id = hidden.slots().getLast().id();
    var life = fixture.item(ItemState.Rarity.RARE, fixture.life).explicits().getFirst();
    var shown = hidden.selectDirect(id, life, fixture.catalog);
    var executor = new AbyssCurrencyExecutor(fixture.catalog, fixture.workbench);
    for (var state : List.of(hidden, shown)) {
      var expected =
          executor.apply(
              state, WorkbenchCurrency.ANNULMENT, Set.of(WorkbenchOmen.LIGHT.id()), new Random(1));
      var law =
          fixture.law.successors(
              state.storedItem(),
              "ANNULMENT",
              Set.of(WorkbenchOmen.LIGHT),
              Set.of(fixture.life),
              1);
      assertThat(expected.applied()).isTrue();
      assertThat(law.complete()).isTrue();
      var removed = state == hidden ? Set.<String>of() : Set.of(fixture.life);
      assertThat(law.removableModifierIds()).isEqualTo(removed);
      assertThat(law.outcomes())
          .singleElement()
          .satisfies(
              outcome -> {
                assertThat(outcome.probability()).isEqualTo(Fraction.ONE);
                assertThat(outcome.removedModifierIds()).isEqualTo(removed);
                assertThat(outcome.state()).isEqualTo(expected.state().storedItem());
                assertThat(outcome.state().desecrated()).isEmpty();
                assertThat(outcome.state().explicits()).isEqualTo(start.explicits());
              });
    }
  }

  /** 관측할 후보 분포를 가정하지 않고, 빗나간 실제 옵션 하나를 지워 같은 뼈를 다시 쓸 수 있다. */
  @Test
  void desecratesRevealsAndRetriesWithoutInventingARevealRate() {
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
    var opaque = AbyssCraftingState.from(hidden, fixture.catalog);
    var miss =
        opaque.eligibleRevealIds(hidden.desecrated().getFirst().id(), fixture.catalog).stream()
            .filter(id -> !id.equals(fixture.life))
            .findFirst()
            .orElseThrow();
    var reveal = fixture.law.successors(hidden, "REVEAL", Set.of(), Set.of(miss), 1);
    var missed = reveal.outcomes().getFirst();
    assertThat(missed.state().desecrated().getFirst().modifierId()).isEqualTo(miss);
    assertThat(missed.probability()).isNull();
    var clear =
        fixture
            .law
            .successors(missed.state(), "ANNULMENT", Set.of(WorkbenchOmen.LIGHT), targets, 1)
            .outcomes()
            .getFirst();
    assertThat(clear.state()).isEqualTo(start);
    var retry =
        fixture
            .law
            .prepareDesecration(clear.state(), "Preserved_Collarbone", Set.of(), targets, 1)
            .outcomes()
            .getFirst()
            .state();
    var hit = fixture.law.successors(retry, "REVEAL", Set.of(), targets, 1).outcomes().getFirst();
    assertThat(hit.state().desecrated().getFirst().modifierId()).isEqualTo(fixture.life);
    assertThat(hit.probability()).isNull();
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
