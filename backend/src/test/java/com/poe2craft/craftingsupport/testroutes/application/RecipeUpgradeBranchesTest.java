package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** 승급의 실제 목표 적중과 준비만 끝난 결과를 같은 시작 상태에서 검증한다. */
class RecipeUpgradeBranchesTest {
  /** 냉기 승급은 의도한 획득이다. 추가 접미가 제거되면 냉기를 유지하며 냉기가 제거된 경우에만 다시 얻는다. */
  @Test
  void replacementKeepsOrLosesTheActualUpgradeEssenceGoal() {
    var fixture = new RecipeTransitionsTest();
    String spirit = "amulet:prefix:countess";
    String cold = "amulet:suffix:of-the-yeti";
    var target =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        spirit, List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L)))),
                new RouteCards.Option(
                    2,
                    Map.of(
                        fixture.defences,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null)))),
                new RouteCards.Option(
                    3,
                    Map.of(
                        cold,
                        List.of(
                            new RouteCards.Source("B", null, RouteCards.Frequency.M),
                            new RouteCards.Source("E", "ESSENCE_THAWING", null))))),
            0);
    Predicate<ItemState> complete =
        item ->
            item.explicits().stream()
                .map(ModifierInstance::modifierId)
                .toList()
                .containsAll(List.of(spirit, cold, fixture.defences));
    var upgrade =
        new RecipeRoutePlanner.Step("ESSENCE_THAWING", List.of(), List.of(3), List.of(), null);
    var suffix =
        new RecipeRoutePlanner.Step(
            "EXALTED", List.of("Omen_of_Dextral_Exaltation"), List.of(), List.of(), null);
    var keep =
        new RecipeRoutePlanner.Step(
            "PERFECT_ESSENCE_ENHANCEMENT",
            List.of("Omen_of_Dextral_Crystallisation"),
            List.of(2),
            List.of(),
            null);
    var lose =
        new RecipeRoutePlanner.Step(keep.action(), keep.omens(), List.of(2), List.of(3), null);
    var recover = new RecipeRoutePlanner.Step("EXALTED", List.of(), List.of(3), List.of(), null);
    var magic = fixture.item(ItemState.Rarity.MAGIC, spirit);
    var recovered =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            magic,
            target,
            complete,
            10000,
            true,
            null,
            List.of(upgrade, lose, recover));
    assertThat(recovered.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.lucky()).isFalse();
              assertThat(route.order()).containsExactly(3, 2, 3);
              assertThat(route.steps().get(1).lost()).containsExactly(3);
              assertThat(recovered.states().get(route.stateIndices().get(1)).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder(spirit, fixture.defences);
            });

    for (boolean fromNormal : List.of(false, true)) {
      var steps = new ArrayList<RecipeRoutePlanner.Step>();
      if (fromNormal)
        steps.add(
            new RecipeRoutePlanner.Step("TRANSMUTATION", List.of(), List.of(1), List.of(), null));
      steps.addAll(List.of(upgrade, suffix, keep));
      var kept =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              fromNormal ? fixture.item(ItemState.Rarity.NORMAL) : magic,
              target,
              complete,
              10000,
              true,
              null,
              steps);
      assertThat(kept.routes())
          .singleElement()
          .satisfies(
              route -> {
                assertThat(route.lucky()).isEqualTo(fromNormal);
                assertThat(route.order()).isEqualTo(fromNormal ? List.of(1, 3, 2) : List.of(3, 2));
                int essenceIndex = fromNormal ? 1 : 0;
                var essence = route.steps().get(essenceIndex);
                assertThat(essence.acquired()).containsExactly(3);
                assertThat(essence.dummy()).isFalse();
                assertThat(essence.chance().probability()).isEqualTo(1);
                var filler = route.steps().get(essenceIndex + 1);
                assertThat(filler.dummy()).isTrue();
                assertThat(filler.chance().probability()).isEqualTo(1);
                assertThat(route.steps().getLast().lost()).isEmpty();
                assertThat(route.steps().getLast().chance().probability()).isEqualTo(0.5);
                var frames =
                    route.stateIndices().subList(essenceIndex, steps.size()).stream()
                        .map(i -> kept.states().get(i).explicits())
                        .toList();
                var coldMod =
                    frames.getFirst().stream()
                        .filter(m -> m.modifierId().equals(cold))
                        .findFirst()
                        .orElseThrow();
                assertThat(frames).allSatisfy(mods -> assertThat(mods).contains(coldMod));
                var filled = kept.states().get(route.stateIndices().get(essenceIndex + 1));
                assertThat(
                        new RecipeMoves(fixture.workbench)
                            .choices(filled, target, Map.of(1, spirit, 3, cold)))
                    .noneMatch(c -> c.target() == 2 && c.followup() != null);
              });
    }
  }

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
