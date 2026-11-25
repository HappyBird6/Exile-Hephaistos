package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 품질 40의 상한 접두, 작업, 제거 후 유지와 지정한 기폭제의 실제 상태까지 고정 경로로 검증한다. */
class RecipeQualityWorkTest {
  /** 태양 목걸이의 검증된 티어와 작업대다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  /** 품질만 요구하는 카드다. */
  private final RouteCards.Card empty = new RouteCards.Card(List.of(), 0);

  /** 목표가 없는 단계와 목표 제작 적중을 등급 재생 요청과 같은 형식으로 만든다. */
  private RecipeRoutePlanner.Step step(String action, Integer... acquired) {
    return new RecipeRoutePlanner.Step(action, List.of(), List.of(acquired), List.of(), null);
  }

  /** 준비 중에는 실제로 붙은 상한을 확인하고 임의 작업은 아이템에 가상의 타입을 추가하지 않는다. */
  @Test
  void obtainsCapAndCompletesUntypedWorkOnce() {
    var start = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(empty), s -> true, 3000, true, 40);
    assertThat(found.routes())
        .anySatisfy(
            route -> {
              assertThat(route.steps())
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("ESSENCE_BREACH", "QUALITY_40");
              assertThat(route.probability()).isEqualTo(1.0);
              var capped = found.states().get(route.stateIndices().getFirst());
              assertThat(capped.explicits())
                  .extracting(ModifierInstance::modifierId)
                  .contains(QualityLimitRules.BREACH_ID);
              assertThat(found.states().get(route.stateIndices().getLast())).isEqualTo(capped);
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1.0);
              assertThat(route.steps().getLast().expectedUses()).isNull();
            });
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    fixture.workbench,
                    start,
                    empty,
                    s -> true,
                    1000,
                    true,
                    40,
                    List.of(step("QUALITY_40")))
                .routes())
        .isEmpty();
  }

  /** 상한이 사라진 품질 40을 같은 종류로는 재작업하지 않으며 새 종류는 접두를 다시 붙여야 한다. */
  @Test
  void existingWrongTypeNeedsTheCapAgain() {
    var plain = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    var start =
        new ItemState(
            plain.snapshotId(),
            plain.baseItemId(),
            plain.itemLevel(),
            plain.rarity(),
            plain.implicits(),
            plain.explicits(),
            Set.of(),
            null,
            new CatalystQuality(CatalystQuality.Type.NEURAL, 40));
    var catalyst = WorkbenchCurrency.CATALYST_FLESH;
    assertThat(
            RecipeRoutePlanner.evaluateFixed(
                    fixture.workbench,
                    start,
                    empty,
                    s -> true,
                    1000,
                    true,
                    40,
                    List.of(step(catalyst.name())),
                    null,
                    catalyst)
                .routes())
        .isEmpty();
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            empty,
            s -> true,
            1000,
            true,
            40,
            List.of(step("ESSENCE_BREACH"), step(catalyst.name())),
            null,
            catalyst);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().getLast().expectedUses()).isEqualTo(40.0);
              assertThat(result.states().get(route.stateIndices().getLast()).catalystQuality())
                  .isEqualTo(new CatalystQuality(CatalystQuality.Type.FLESH, 40));
            });
    assertThat(
            RecipeRoutePlanner.calculate(
                    fixture.workbench,
                    start,
                    List.of(empty),
                    s -> true,
                    1000,
                    true,
                    40,
                    null,
                    WorkbenchCurrency.CATALYST_NEURAL)
                .routes())
        .singleElement()
        .satisfies(r -> assertThat(r.steps()).isEmpty());
  }

  /** 임시 제작 접두를 지우고 다른 에센스를 쓰더라도 지정·임의 품질 작업은 다시 요구하지 않는다. */
  @Test
  void carriesQualityPastRemovalWhenAnotherCraftedModifierIsRequired() {
    var start = fixture.item(ItemState.Rarity.RARE, QualityLimitRules.BREACH_ID, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        fixture.defences,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null))))),
            0);
    for (var typed : List.of(true, false)) {
      var catalyst = typed ? WorkbenchCurrency.CATALYST_FLESH : null;
      var steps =
          List.of(
              step(typed ? catalyst.name() : "QUALITY_40"),
              new RecipeRoutePlanner.Step(
                  "ANNULMENT", List.of("Omen_of_Sinistral_Annulment"), List.of(), List.of(), null),
              step("PERFECT_ESSENCE_ENHANCEMENT", 1));
      var result =
          RecipeRoutePlanner.evaluateFixed(
              fixture.workbench, start, card, s -> true, 5000, true, 40, steps, null, catalyst);
      assertThat(result.routes())
          .singleElement()
          .satisfies(
              route -> {
                assertThat(route.probability()).isEqualTo(1.0);
                var item = result.states().get(route.stateIndices().getLast());
                assertThat(item.explicits())
                    .extracting(ModifierInstance::modifierId)
                    .containsExactly(fixture.defences);
                if (typed)
                  assertThat(item.catalystQuality())
                      .isEqualTo(new CatalystQuality(CatalystQuality.Type.FLESH, 40));
                else assertThat(item.catalystQuality()).isNull();
              });
    }
  }

  /** 품질 준비에 쓰는 마법 승급 에센스가 목표를 부여하면 일반 경로로 기록하고 그 접미를 보존한다. */
  @Test
  void qualityWorkKeepsTheMagicToRareEssenceAndCanProtectTheUpgradeHit() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.life);
    String cold = "amulet:suffix:of-the-yeti";
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1, Map.of(cold, List.of(new RouteCards.Source("E", "ESSENCE_THAWING", null))))),
            0);
    var steps =
        List.of(
            step("ESSENCE_THAWING", 1),
            new RecipeRoutePlanner.Step(
                "ESSENCE_BREACH",
                List.of("Omen_of_Sinistral_Crystallisation"),
                List.of(),
                List.of(),
                null),
            step("QUALITY_40"));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, s -> true, 2000, true, 40, steps);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.lucky()).isFalse();
              assertThat(r.probability()).isEqualTo(1.0);
              assertThat(result.states().get(r.stateIndices().getLast()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder(cold, QualityLimitRules.BREACH_ID);
            });
  }

  /** 최대 품질 접두 자체를 목표로 선택한 경우 균열은 그 번호를 직접 얻으며 이미 얻은 희소 목표는 보존한다. */
  @Test
  void theRequiredCapIsAnOrdinaryAcquisitionAndKeepsAProtectedPrefix() {
    String spirit = "amulet:prefix:countess";
    var start = fixture.item(ItemState.Rarity.RARE, spirit, fixture.strength);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        spirit, List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L)))),
                new RouteCards.Option(
                    2,
                    Map.of(
                        QualityLimitRules.BREACH_ID,
                        List.of(new RouteCards.Source("C", "ESSENCE_BREACH", null))))),
            0);
    var steps =
        List.of(
            new RecipeRoutePlanner.Step(
                "ESSENCE_BREACH",
                List.of("Omen_of_Dextral_Crystallisation"),
                List.of(2),
                List.of(),
                null),
            step("QUALITY_40"));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, s -> true, 2000, true, 40, steps);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.order()).containsExactly(2);
              assertThat(r.lucky()).isFalse();
              assertThat(r.probability()).isEqualTo(1.0);
              assertThat(result.states().get(r.stateIndices().getLast()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder(spirit, QualityLimitRules.BREACH_ID);
            });
  }

  /** 지켜야 할 접두 하나만 있으면 반대편 제거 대상을 마련하고 그 자리만 균열로 교체한다. */
  @Test
  void preparesAnExpendableSuffixBeforeApplyingBreachToASingleProtectedGoal() {
    String spirit = "amulet:prefix:countess";
    var start = fixture.item(ItemState.Rarity.RARE, spirit);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        spirit,
                        List.of(new RouteCards.Source("B", null, RouteCards.Frequency.L))))),
            0);
    var steps =
        List.of(
            new RecipeRoutePlanner.Step(
                "EXALTED", List.of("Omen_of_Dextral_Exaltation"), List.of(), List.of(), null),
            new RecipeRoutePlanner.Step(
                "ESSENCE_BREACH",
                List.of("Omen_of_Dextral_Crystallisation"),
                List.of(),
                List.of(),
                null),
            step("QUALITY_40"));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, s -> true, 4000, true, 40, steps);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.probability()).isEqualTo(1.0);
              assertThat(r.lucky()).isFalse();
              assertThat(r.order()).isEmpty();
              assertThat(result.states().get(r.stateIndices().getLast()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactlyInAnyOrder(spirit, QualityLimitRules.BREACH_ID);
            });
  }
}
