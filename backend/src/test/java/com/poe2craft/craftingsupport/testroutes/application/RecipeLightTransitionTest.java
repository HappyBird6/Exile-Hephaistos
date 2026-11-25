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

  /** 빈 장비 양방향 및 6옵션 제거를 먼저 추첨하고, 목표와 맞지 않는 방향·제거의 질량도 분모에 남긴다. */
  @Test
  void preparingEarlyOrReplacingAtCapacityKeepsTheWorkbenchDenominator() {
    var bone = "Preserved_Collarbone";
    var empty = fixture.item(ItemState.Rarity.RARE);
    var prefixOnly = fixture.law.prepareDesecration(empty, bone, Set.of(), Set.of(fixture.life), 1);
    assertThat(prefixOnly.complete()).isTrue();
    assertThat(prefixOnly.outcomes())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.probability()).isEqualTo(Fraction.of(1, 2));
              assertThat(o.removedModifierIds()).isEmpty();
              assertThat(o.state().explicits()).isEmpty();
              assertThat(o.state().desecrated())
                  .singleElement()
                  .satisfies(
                      s -> {
                        assertThat(s.modifierId()).isNull();
                        assertThat(s.side()).isEqualTo(ModifierDefinition.AffixType.PREFIX);
                      });
            });
    var partial =
        fixture.law.prepareDesecration(
            empty, bone, Set.of(), Set.of(fixture.life, fixture.strength), 1);
    assertThat(partial.complete()).isFalse();
    assertThat(partial.outcomes())
        .singleElement()
        .satisfies(o -> assertThat(o.probability()).isEqualTo(Fraction.of(1, 2)));

    var five = fiveModifiers();
    var mods = new ArrayList<>(five.explicits());
    mods.add(fixture.item(ItemState.Rarity.RARE, fixture.life).explicits().getFirst());
    var full = fixture.workbench.finishBasicCurrency(five, mods);
    var replace =
        fixture.law.prepareDesecration(full, bone, Set.of(), Set.of("amulet:prefix:virile"), 1);
    assertThat(replace.complete()).isTrue();
    assertThat(replace.removableModifierIds()).hasSize(6);
    assertThat(replace.outcomes())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.probability()).isEqualTo(Fraction.of(1, 6));
              assertThat(o.removedModifierIds()).containsExactly(fixture.life);
              assertThat(o.state().explicits()).isEqualTo(five.explicits());
              assertThat(o.state().desecrated().getFirst().modifierId()).isNull();
            });
  }

  /** 기존 생명력이 막은 계열을 에센스로 제거한 뒤 같은 미공개 슬롯에서 다른 생명력 티어를 얻는다. */
  @Test
  void deferredSelectionCanOutliveAMutableFamilyBlocker() {
    String material = "Preserved_Collarbone";
    String blocker = "amulet:prefix:hale";
    var start = fixture.item(ItemState.Rarity.RARE, blocker);
    var targets = Set.of(fixture.life);
    var immediate = fixture.law.prepareDesecration(start, material, Set.of(), targets, 8);
    assertThat(immediate.complete()).isTrue();
    assertThat(immediate.outcomes()).isEmpty();
    var delayed = fixture.law.prepareDesecration(start, material, Set.of(), targets, 8, true);
    assertThat(delayed.complete()).isTrue();
    assertThat(delayed.outcomes()).hasSize(1);
    var bone = delayed.outcomes().getFirst();
    assertThat(bone.probability()).isEqualTo(Fraction.of(1, 2));
    var hidden = bone.state();
    assertThat(hidden.explicits()).isEqualTo(start.explicits());
    assertThat(fixture.law.successors(hidden, "REVEAL", Set.of(), targets, 32).outcomes())
        .isEmpty();
    var essence =
        fixture.law.successors(
            hidden,
            "PERFECT_ESSENCE_ENHANCEMENT",
            Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION),
            Set.of(fixture.defences),
            64,
            false);
    assertThat(essence.outcomes())
        .anySatisfy(
            o -> {
              assertThat(o.removedModifierIds()).containsExactly(blocker);
              assertThat(o.state().desecrated()).isEqualTo(hidden.desecrated());
              assertThat(o.state().explicits())
                  .anyMatch(m -> m.crafted() && m.modifierId().equals(fixture.defences));
              var reveal = fixture.law.successors(o.state(), "REVEAL", Set.of(), targets, 32);
              assertThat(reveal.complete()).isTrue();
              assertThat(reveal.outcomes())
                  .isNotEmpty()
                  .allMatch(
                      hit -> hit.state().desecrated().getFirst().modifierId().equals(fixture.life));
              assertThat(
                      reveal.outcomes().stream()
                          .map(RecipeTransitions.Outcome::probability)
                          .reduce(Fraction.ZERO, Fraction::add))
                  .isEqualTo(Fraction.ONE);
            });
    var original = start.explicits().getFirst();
    var fixed =
        fixture.workbench.finishBasicCurrency(
            start, List.of(new ModifierInstance(blocker, original.values(), true)));
    var immutableConflict =
        fixture.law.prepareDesecration(fixed, material, Set.of(), targets, 8, true);
    assertThat(immutableConflict.complete()).isTrue();
    assertThat(immutableConflict.outcomes()).isEmpty();
    var ancient =
        fixture.law.prepareDesecration(
            fixture.item(ItemState.Rarity.RARE),
            "Ancient_Collarbone",
            Set.of(),
            Set.of(blocker),
            8,
            true);
    assertThat(ancient.complete()).isTrue();
    assertThat(ancient.outcomes()).isEmpty();
  }

  /** 고정 경로에서도 뼈를 먼저 쓰고 계열 차단을 없앤 뒤 마지막 공개로 마무리한다. */
  @Test
  void replaysEarlyBoneWithAConflictThatOnlyTheLaterEssenceClears() {
    String bone = "Preserved_Collarbone";
    String enhancement = "PERFECT_ESSENCE_ENHANCEMENT";
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        fixture.defences, List.of(new RouteCards.Source("C", enhancement, null)))),
                new RouteCards.Option(
                    2, Map.of(fixture.life, List.of(new RouteCards.Source("D", bone, null))))),
            0);
    var start = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:hale");
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            i ->
                i.explicits().stream()
                        .anyMatch(m -> m.modifierId().equals(fixture.defences) && m.crafted())
                    && i.desecrated().stream().anyMatch(s -> fixture.life.equals(s.modifierId())),
            5000,
            true,
            null,
            List.of(
                new RecipeRoutePlanner.Step(bone, List.of(), List.of(), List.of(), null),
                new RecipeRoutePlanner.Step(
                    enhancement,
                    List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()),
                    List.of(1),
                    List.of(),
                    null),
                new RecipeRoutePlanner.Step("REVEAL", List.of(), List.of(2), List.of(), null)));
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.order()).containsExactly(1, 2);
              var hidden = result.states().get(route.stateIndices().getFirst());
              var cleared = result.states().get(route.stateIndices().get(1));
              var end = result.states().get(route.stateIndices().getLast());
              assertThat(hidden.explicits()).isEqualTo(start.explicits());
              assertThat(hidden.desecrated().getFirst().modifierId()).isNull();
              assertThat(cleared.desecrated()).isEqualTo(hidden.desecrated());
              assertThat(cleared.explicits())
                  .noneMatch(m -> m.modifierId().equals("amulet:prefix:hale"));
              assertThat(end.desecrated().getFirst().modifierId()).isEqualTo(fixture.life);
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1d);
            });
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
