package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 절사 카오스의 허용 조합과 제거 대상, 분열·훼손 및 화폐 등급별 고정 경로 재생을 검증한다. */
class RecipeWhittlingMovesTest {
  /** 무징조 카오스 후보는 반복을 시작할 수 있을 때만 제공하고, 더 많은 속성이 있는 아이템에서는 징조 교체를 유지한다. */
  @Test
  void plainChaosNeedsOneMutableOptionButOmenChaosRemainsAnAlternative() {
    var fixture = new RecipeTransitionsTest();
    var option =
        new RouteCards.Option(
            1,
            Map.of(
                fixture.strength,
                List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))));
    var card = new RouteCards.Card(List.of(option), 0);
    var moves = new RecipeMoves(fixture.workbench);
    var one = fixture.item(ItemState.Rarity.RARE, fixture.life);
    var two = fixture.item(ItemState.Rarity.RARE, fixture.life, "amulet:prefix:azure");
    var whittling = WorkbenchOmen.WHITTLING;
    var left = Set.of(whittling, WorkbenchOmen.SINISTRAL_ERASURE);
    var right = Set.of(whittling, WorkbenchOmen.DEXTRAL_ERASURE);
    assertThat(moves.choices(one, card, Map.of()))
        .extracting(RecipeMoves.Choice::move)
        .filteredOn(m -> m.action().equals("CHAOS"))
        .extracting(RecipeMoves.Move::omens)
        .containsExactlyInAnyOrder(Set.of(), Set.of(whittling), left, right);
    assertThat(moves.choices(two, card, Map.of()))
        .extracting(RecipeMoves.Choice::move)
        .filteredOn(m -> m.action().equals("CHAOS"))
        .extracting(RecipeMoves.Move::omens)
        .containsExactlyInAnyOrder(Set.of(whittling), left, right);
  }

  /** 절사와 말소 조합은 화폐 등급이 달라져도 선택한 쪽의 가장 낮은 등장 레벨만 제거한다. */
  @Test
  void allThreeChaosGradesUseTheSelectedWhittlingRemovalPool() {
    var fixture = new RecipeTransitionsTest();
    String prefix = "amulet:prefix:azure";
    String higherPrefix = "amulet:prefix:professor-s";
    String life = "amulet:prefix:athlete-s";
    var item = fixture.item(ItemState.Rarity.RARE, prefix, higherPrefix, fixture.strength);
    var whittling = WorkbenchOmen.WHITTLING;
    var groups =
        Map.of(
            Set.of(whittling), fixture.strength,
            Set.of(whittling, WorkbenchOmen.SINISTRAL_ERASURE), prefix,
            Set.of(whittling, WorkbenchOmen.DEXTRAL_ERASURE), fixture.strength);
    for (var currency : List.of("CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS")) {
      for (var combination : groups.entrySet()) {
        var law =
            fixture.law.successors(item, currency, combination.getKey(), Set.of(life), 100, false);
        assertThat(law.complete()).isTrue();
        assertThat(law.removableModifierIds()).containsExactly(combination.getValue());
        assertThat(law.outcomes())
            .isNotEmpty()
            .allSatisfy(
                hit -> {
                  assertThat(hit.removedModifierIds()).containsExactly(combination.getValue());
                  assertThat(hit.state().explicits())
                      .extracting(m -> m.modifierId())
                      .contains(life, higherPrefix)
                      .doesNotContain(combination.getValue());
                });
      }
    }
  }

  /** 분열 접두는 절사·좌측 말소로 제거할 수 없으며 미공개 훼손의 최저 레벨은 어느 조합으로도 확정하지 않는다. */
  @Test
  void whittlingProtectsFracturesAndLeavesHiddenRemovalLevelsUnknown() {
    var fixture = new RecipeTransitionsTest();
    var start = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:azure", fixture.strength);
    var prefix = start.explicits().getFirst();
    var fractured =
        RecipeStateGroups.withAffixes(
            start,
            start.rarity(),
            List.of(
                new ModifierInstance(prefix.modifierId(), prefix.values(), true),
                start.explicits().getLast()));
    var whittling = WorkbenchOmen.WHITTLING;
    var left = Set.of(whittling, WorkbenchOmen.SINISTRAL_ERASURE);
    var right = Set.of(whittling, WorkbenchOmen.DEXTRAL_ERASURE);
    var targets = Set.of(fixture.life);
    assertThat(fixture.law.successors(fractured, "CHAOS", left, targets, 100, false).outcomes())
        .isEmpty();
    var rightLaw = fixture.law.successors(fractured, "CHAOS", right, targets, 100, false);
    assertThat(rightLaw.removableModifierIds()).containsExactly(fixture.strength);
    assertThat(rightLaw.outcomes())
        .isNotEmpty()
        .allSatisfy(
            o -> assertThat(o.state().explicits()).contains(fractured.explicits().getFirst()));
    var hidden =
        start.withDesecrated(
            List.of(
                new ItemState.DesecratedSlot("open", ModifierDefinition.AffixType.PREFIX, null)));
    for (var omens : List.of(Set.of(whittling), left, right)) {
      var law = fixture.law.successors(hidden, "CHAOS", omens, targets, 100, false);
      assertThat(law.outcomes()).isEmpty();
      assertThat(law.complete()).isFalse();
    }
  }

  /** 말소만 지정한 고정 경로는 세 등급 모두 추천 정책을 통과하지 못한다. 절사와 함께 쓰는 입력은 그대로 재생한다. */
  @Test
  void fixedOmenChaosReplaysOnlyWhittlingCombinations() {
    var fixture = new RecipeTransitionsTest();
    var item = fixture.item(ItemState.Rarity.RARE, "amulet:prefix:azure", fixture.strength);
    String life = "amulet:prefix:athlete-s";
    var source = new RouteCards.Source("B", null, RouteCards.Frequency.M);
    var card =
        new RouteCards.Card(List.of(new RouteCards.Option(1, Map.of(life, List.of(source)))), 0);
    for (var currency : List.of("CHAOS", "GREATER_CHAOS", "PERFECT_CHAOS")) {
      for (var side : List.of(WorkbenchOmen.SINISTRAL_ERASURE, WorkbenchOmen.DEXTRAL_ERASURE)) {
        for (boolean whittling : List.of(false, true)) {
          var omens =
              whittling ? List.of(side.id(), WorkbenchOmen.WHITTLING.id()) : List.of(side.id());
          var step = new RecipeRoutePlanner.Step(currency, omens, List.of(1), List.of(), null);
          var result =
              RecipeRoutePlanner.evaluateFixed(
                  fixture.workbench,
                  item,
                  card,
                  end -> end.explicits().stream().anyMatch(m -> m.modifierId().equals(life)),
                  1000,
                  true,
                  null,
                  List.of(step));
          if (whittling)
            assertThat(result.routes())
                .singleElement()
                .satisfies(
                    route -> {
                      assertThat(route.steps().getFirst().omens())
                          .containsExactlyInAnyOrderElementsOf(omens);
                      assertThat(route.steps().getFirst().action()).isEqualTo(currency);
                    });
          else assertThat(result.routes()).isEmpty();
        }
      }
    }
  }
}
