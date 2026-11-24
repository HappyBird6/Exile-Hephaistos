package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 승급의 실제 목표 적중과 준비만 끝난 결과를 같은 시작 상태에서 검증한다. */
class RecipeUpgradeBranchesTest {
  /** 기본 제왕의 목표 적중과 빗나간 뒤의 기본 엑잘 적중이 별도 경로이고 상위 등급은 생성하지 않는다. */
  @Test
  void branchesOnTheActualRegalOutcomeAndUsesTheGradePool() {
    var fixture = new RecipeTransitionsTest();
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var option =
        new RouteCards.Option(
            1,
            Map.of(
                fixture.life, List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))));
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            start,
            List.of(new RouteCards.Card(List.of(option), 0)),
            item -> item.explicits().stream().anyMatch(m -> m.modifierId().equals(fixture.life)),
            100000,
            true,
            null);
    var direct =
        result.routes().stream()
            .filter(r -> r.steps().size() == 1)
            .map(r -> r.steps().getFirst())
            .filter(s -> s.omens().isEmpty() && s.action().endsWith("REGAL"))
            .toList();
    assertThat(direct).extracting(RecipeRoutePlanner.Step::action).containsExactly("REGAL");
    assertThat(result.routes().stream().flatMap(r -> r.steps().stream()))
        .allMatch(s -> !s.action().startsWith("GREATER_") && !s.action().startsWith("PERFECT_"));
    assertThat(direct).allSatisfy(s -> assertThat(s.acquired()).containsExactly(1));
    var throughMiss =
        result.routes().stream()
            .filter(
                r ->
                    r.steps().size() == 2
                        && r.steps().getFirst().action().endsWith("REGAL")
                        && r.steps().getLast().action().equals("EXALTED")
                        && r.steps().stream().allMatch(s -> s.omens().isEmpty()))
            .toList();
    assertThat(throughMiss).extracting(r -> r.steps().getFirst().action()).containsExactly("REGAL");
    for (var route : throughMiss) {
      var upgraded = result.states().get(route.stateIndices().getFirst());
      assertThat(upgraded.rarity()).isEqualTo(ItemState.Rarity.RARE);
      assertThat(upgraded.explicits()).hasSize(2);
      assertThat(upgraded.explicits()).noneMatch(m -> m.modifierId().equals(fixture.life));
      assertThat(route.steps().getFirst().acquired()).isEmpty();
      assertThat(route.steps().getLast().acquired()).containsExactly(1);
      assertThat(route.order()).containsExactly(1);
    }
  }

  /** 선택한 화폐의 징조·홈은 유지되며, 재생에서는 각 등급의 실제 작업대 추첨 풀을 사용한다. */
  @Test
  void mapsOnlyTheFiveGradedOrbFamiliesAndChecksTheSelectedRollPool() {
    var omens = Set.of(WorkbenchOmen.SINISTRAL_CORONATION);
    for (var family : List.of("TRANSMUTATION", "AUGMENTATION", "REGAL", "EXALTED", "CHAOS")) {
      var basic = new RecipeMoves.Move(family, omens, 1);
      for (var prefix : List.of("GREATER_", "PERFECT_")) {
        assertThat(RecipeMoves.baseMove(new RecipeMoves.Move(prefix + family, omens, 1)))
            .isEqualTo(basic);
      }
    }
    for (var material :
        List.of("REGAL", "GREATER_ESSENCE_BODY", "PERFECT_ESSENCE_ENHANCEMENT", "REVEAL")) {
      var original = new RecipeMoves.Move(material);
      assertThat(RecipeMoves.baseMove(original)).isSameAs(original);
    }
    var fixture = new RecipeTransitionsTest();
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    assertThat(
            fixture
                .law
                .successors(start, "GREATER_REGAL", omens, Set.of(fixture.life), 100, false)
                .outcomes())
        .isNotEmpty();
    assertThat(
            fixture
                .law
                .successors(start, "PERFECT_REGAL", omens, Set.of(fixture.life), 100, false)
                .outcomes())
        .isEmpty();
  }
}
