package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 후속 제작 결과가 같은 비목표 티어만 묶고 절사의 징조·수치 목표·분열에는 실제 차이를 보존한다. */
class RecipeStateGroupsTest {
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();
  private final String upperLife = "amulet:prefix:virile";
  private final ItemState lower = fixture.item(ItemState.Rarity.RARE, fixture.life);
  private final ItemState upper = fixture.item(ItemState.Rarity.RARE, upperLife);

  /** 교체형 에센스만 남은 경우의 생명력 티어는 같은 쪽·같은 차단 계열이다. 미보유 B(M) 목표에 절사의 징조가 가능하면 등급 순서를 지킨다. */
  @Test
  void equalFamilyTiersShareMassOnlyWhenTheirLevelAndValuesAreIrrelevant() {
    var craft = new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null);
    var crafted =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.defences, List.of(craft)))), 0);
    var writing =
        new RecipeStateGroups(fixture.catalog, crafted, Set.of(fixture.defences), true, Set.of());
    assertThat(writing.key(lower)).isEqualTo(writing.key(upper));
    var numeric =
        new RecipeStateGroups(
            fixture.catalog, crafted, Set.of(fixture.defences), false, Set.of("base_maximum_life"));
    assertThat(numeric.key(lower)).isNotEqualTo(numeric.key(upper));
    var fracture = new ModifierInstance(fixture.life, lower.explicits().getFirst().values(), true);
    var original = RecipeStateGroups.withAffixes(lower, lower.rarity(), List.of(fracture));
    assertThat(writing.key(original)).isSameAs(original);
    var draw = new RouteCards.Source("B", null, RouteCards.Frequency.M);
    var target =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.strength, List.of(draw)))), 0);
    var chaos =
        new RecipeStateGroups(fixture.catalog, target, Set.of(fixture.strength), true, Set.of());
    assertThat(chaos.key(lower)).isNotEqualTo(chaos.key(upper));
  }

  /** 카오스 주문 레벨이 +7로 고정된 지팡이와 +5~6인 지팡이는 Divine 사용 가능 여부가 다르다. */
  @Test
  void fixedAndRerollableNonTargetStaffTiersStaySeparate() {
    var catalog = ItemCatalogLoader.loadTopBase("perching-staff");
    var workbench = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var stat = "chaos_spell_skill_gem_level_+";
    var fixedMod =
        new ModifierInstance(
            "staves:suffix:of-armageddon:GlobalChaosSpellGemsLevelTwoHandWeapon5",
            Map.of(stat, 7L));
    var variableMod =
        new ModifierInstance(
            "staves:suffix:of-havoc:GlobalChaosSpellGemsLevelTwoHandWeapon4", Map.of(stat, 5L));
    var fixed =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(fixedMod),
            Set.of());
    var variable = RecipeStateGroups.withAffixes(fixed, fixed.rarity(), List.of(variableMod));
    var groups =
        new RecipeStateGroups(
            catalog, new RouteCards.Card(List.of(), 0), Set.of(), false, Set.of());
    assertThat(workbench.availability(fixed, WorkbenchCurrency.DIVINE, Set.of()).available())
        .isFalse();
    assertThat(workbench.availability(variable, WorkbenchCurrency.DIVINE, Set.of()).available())
        .isTrue();
    assertThat(groups.key(fixed)).isNotEqualTo(groups.key(variable));
  }

  /** 현재 수치가 통과한 마법 아이템에는 희귀 승급만 필요하고, 미달이라면 실제 Divine 수치 변경 후 승급 분기가 남는다. */
  @Test
  void rarityAloneDoesNotRequireADivine() {
    var source = new RouteCards.Source("B", null, RouteCards.Frequency.M);
    var card =
        new RouteCards.Card(
            List.of(new RouteCards.Option(1, Map.of(fixture.life, List.of(source)))), 0);
    var divine = new RecipeRoutePlanner.Step("DIVINE", List.of(), List.of(), List.of(), null);
    var regal = new RecipeRoutePlanner.Step("REGAL", List.of(), List.of(), List.of(), null);
    var shape = fixture.item(ItemState.Rarity.MAGIC, fixture.life);
    java.util.function.Predicate<ItemState> goal =
        s ->
            s.rarity() == ItemState.Rarity.RARE
                && s.explicits().stream()
                    .anyMatch(
                        m ->
                            m.modifierId().equals(fixture.life)
                                && m.values().get("base_maximum_life") >= 99);
    for (long value : List.of(85L, 99L)) {
      var magic =
          RecipeStateGroups.withAffixes(
              shape,
              shape.rarity(),
              List.of(new ModifierInstance(fixture.life, Map.of("base_maximum_life", value))));
      var result =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench,
              magic,
              card,
              goal,
              100000,
              false,
              null,
              List.of(divine, regal),
              Set.of("base_maximum_life"));
      assertThat(result.routes())
          .singleElement()
          .satisfies(r -> assertThat(r.probability()).isEqualTo(1.0 / 15));
      var suggestions =
          RecipeRoutePlanner.calculate(
              fixture.workbench,
              magic,
              List.of(card),
              goal,
              100000,
              false,
              null,
              Set.of("base_maximum_life"));
      assertThat(
              suggestions.routes().stream()
                  .anyMatch(
                      r ->
                          r.steps().stream()
                              .map(RecipeRoutePlanner.Step::action)
                              .toList()
                              .equals(List.of("DIVINE", "REGAL"))))
          .isEqualTo(value < 99);
    }
  }
}
