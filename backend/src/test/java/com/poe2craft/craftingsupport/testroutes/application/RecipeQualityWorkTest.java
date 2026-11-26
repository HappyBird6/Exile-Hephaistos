package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
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

  /** 지정한 일반 티어 자체를 목표로 두어 뼈·에센스가 같은 행의 별도 후보를 만들지 않도록 한다. */
  private RouteCards.Option baseOption(int number, String tier) {
    return new RouteCards.Option(
        number, Map.of(tier, List.of(new RouteCards.Source("B", null, RouteCards.Frequency.M))));
  }

  /** 희귀 빈 베이스에서 최대 품질용 더미의 방향은 어느 후속 경로에서도 징조를 요구하지 않는다. */
  @Test
  void freshRareUsesOnlyPlainExaltAndBreachForTheFirstQualitySlot() {
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench,
            fixture.item(ItemState.Rarity.RARE),
            List.of(empty),
            s -> true,
            100000,
            true,
            40);
    assertThat(found.routes())
        .isNotEmpty()
        .allSatisfy(
            route -> {
              var first = route.steps().getFirst();
              var second = route.steps().get(1);
              assertThat(first.action()).isEqualTo("EXALTED");
              assertThat(first.dummy()).isTrue();
              assertThat(second.action()).isEqualTo("ESSENCE_BREACH");
              assertThat(first.omens()).isEmpty();
              assertThat(second.omens()).isEmpty();
              assertThat(second.chance().probability()).isOne();
            });
  }

  /** 품질 작업 전에 유일한 제작 접두를 치우는 소멸에는 좌측 징조가 필요 없다. */
  @Test
  void clearsTheOnlyCraftedAffixWithoutADirectionalOmen() {
    var start = fixture.item(ItemState.Rarity.RARE, fixture.defences);
    var result =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(empty), item -> true, 100000, true, 40);
    assertThat(result.routes())
        .isNotEmpty()
        .allSatisfy(
            route -> {
              var clear = route.steps().getFirst();
              assertThat(clear.action()).isEqualTo("ANNULMENT");
              assertThat(clear.omens()).isEmpty();
              assertThat(clear.chance().probability()).isOne();
            });
  }

  /** 소멸을 먼저 쓰면 나중에 뽑을 접두가 제거 후보에 섞이지 않는다. */
  @Test
  void searchOffersRemovingTheCapBeforeDrawingTheNextPrefix() {
    var start = fixture.item(ItemState.Rarity.RARE, QualityLimitRules.BREACH_ID);
    var card = new RouteCards.Card(List.of(baseOption(1, fixture.life)), 0);
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(card), s -> true, 12000, true, 40);
    assertThat(found.routes())
        .anySatisfy(
            route -> {
              assertThat(route.steps())
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("QUALITY_40", "ANNULMENT", "EXALTED");
              var cleanup = route.steps().get(1);
              assertThat(cleanup.omens()).isEmpty();
              assertThat(cleanup.chance().probability()).isOne();
              assertThat(found.states().get(route.stateIndices().get(1)).explicits()).isEmpty();
              assertThat(found.states().get(route.stateIndices().getLast()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactly(fixture.life);
            });
  }

  /** 단독 소멸은 전체 제거 후보를 세고, 접미가 있어서 제거 확률이 실제로 달라지는 경우에만 좌측 징조를 추가한다. */
  @Test
  void annulmentUsesTheActualCapRemovalPoolAndOnlyMeaningfulDirection() {
    for (boolean hasSuffix : List.of(false, true)) {
      var start =
          hasSuffix
              ? fixture.item(
                  ItemState.Rarity.RARE,
                  QualityLimitRules.BREACH_ID,
                  fixture.life,
                  fixture.strength)
              : fixture.item(ItemState.Rarity.RARE, QualityLimitRules.BREACH_ID, fixture.life);
      var found =
          RecipeRoutePlanner.calculate(
              fixture.workbench, start, List.of(empty), s -> true, 3000, true, 40);
      var rates = new java.util.HashMap<List<String>, Double>();
      for (var route : found.routes()) {
        assertThat(route.steps())
            .extracting(RecipeRoutePlanner.Step::action)
            .containsExactly("QUALITY_40", "ANNULMENT");
        var annul = route.steps().getLast();
        assertThat(annul.chance().probability()).isEqualTo(route.probability());
        rates.put(annul.omens(), annul.chance().probability());
        assertThat(found.states().get(route.stateIndices().getLast()).explicits())
            .containsExactlyElementsOf(
                start.explicits().stream()
                    .filter(m -> !m.modifierId().equals(QualityLimitRules.BREACH_ID))
                    .toList());
      }
      var expected =
          hasSuffix
              ? Map.of(List.of(WorkbenchOmen.SINISTRAL_ANNULMENT.id()), 0.5)
              : Map.of(List.<String>of(), 0.5);
      assertThat(rates).isEqualTo(expected);
    }
  }

  /** 접두를 얻은 뒤에는 절사로 상한을 제거하면서 남은 접미를 직접 얻는 분기만 성공이다. */
  @Test
  void delayedWhittlingSplitsTheActualCapReplacementIntoThreeOutcomes() {
    String cold = "amulet:suffix:of-the-yeti";
    var start = fixture.item(ItemState.Rarity.RARE, QualityLimitRules.BREACH_ID);
    var card = new RouteCards.Card(List.of(baseOption(1, fixture.life), baseOption(2, cold)), 0);
    var whittle =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(2), List.of(), null);
    var replay =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            s -> true,
            12000,
            true,
            40,
            List.of(step("QUALITY_40"), step("EXALTED", 1), whittle));
    var route = replay.routes().getFirst();
    var afterFirst = replay.states().get(route.stateIndices().get(1));
    assertThat(afterFirst.explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactlyInAnyOrder(QualityLimitRules.BREACH_ID, fixture.life);
    var plan =
        fixture.workbench.recipePlan(
            afterFirst, WorkbenchCurrency.CHAOS, Set.of(WorkbenchOmen.WHITTLING));
    assertThat(plan.branches()).hasSize(1);
    var removal = plan.branches().getFirst();
    assertThat(removal.remaining().explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactly(fixture.life);
    var level = fixture.catalog.find(fixture.life).orElseThrow().requiredItemLevel();
    long total = 0, hit = 0, failed = 0;
    for (var candidate : removal.candidates()) {
      total += candidate.weight();
      if (candidate.id().equals(cold)) hit += candidate.weight();
      // 새 접미는 접미 말소로 다시 제거 가능하다. 새 접두는 생명력보다 요구 레벨이 높으면 다시 지울 수 없다.
      else if (candidate.affixType() == ModifierDefinition.AffixType.PREFIX
          && candidate.requiredItemLevel() > level) failed += candidate.weight();
    }
    assertThat(hit).isPositive();
    assertThat(failed).isPositive();
    assertThat(total - hit - failed).isPositive();
    var step = route.steps().getLast();
    var odds = step.omenOutcomes();
    assertThat(odds.success().probability()).isCloseTo((double) hit / total, offset(1e-14));
    assertThat(odds.failure().probability()).isCloseTo((double) failed / total, offset(1e-14));
    assertThat(odds.recoverable().probability())
        .isCloseTo((double) (total - hit - failed) / total, offset(1e-14));
    assertThat(step.chance()).isEqualTo(odds.success());
    assertThat(route.probability())
        .isCloseTo(route.steps().get(1).chance().probability() * hit / total, offset(1e-14));
    assertThat(replay.states().get(route.stateIndices().getLast()).explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactlyInAnyOrder(fixture.life, cold);
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(card), s -> true, 12000, true, 40);
    assertThat(found.routes())
        .anySatisfy(
            candidate -> {
              assertThat(candidate.steps())
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("QUALITY_40", "EXALTED", "CHAOS");
              assertThat(candidate.steps().get(1).acquired()).containsExactly(1);
              assertThat(candidate.steps().getLast().omens())
                  .containsExactly(WorkbenchOmen.WHITTLING.id());
              assertThat(candidate.steps().getLast().acquired()).containsExactly(2);
              assertThat(candidate.steps().getLast().omenOutcomes()).isEqualTo(odds);
            });
  }

  /** 동률인 비목표 접두가 지워진 자리에서 목표를 뽑으면 여전히 상한이 남아 이번 절사 성공에 포함되지 않는다. */
  @Test
  void hittingTheTargetWithoutReplacingTheCapIsNotCleanupSuccess() {
    var start =
        fixture.item(ItemState.Rarity.RARE, QualityLimitRules.BREACH_ID, "amulet:prefix:hale");
    String target = "amulet:suffix:of-the-yeti";
    var card = new RouteCards.Card(List.of(baseOption(1, target)), 0);
    var whittle =
        new RecipeRoutePlanner.Step(
            "CHAOS", List.of(WorkbenchOmen.WHITTLING.id()), List.of(1), List.of(), null);
    var replay =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench,
            start,
            card,
            s -> true,
            5000,
            true,
            40,
            List.of(step("QUALITY_40"), whittle));
    var plan =
        fixture.workbench.recipePlan(
            start, WorkbenchCurrency.CHAOS, Set.of(WorkbenchOmen.WHITTLING));
    assertThat(plan.branches()).hasSize(2);
    var removeCap =
        plan.branches().stream()
            .filter(
                b ->
                    b.remaining().explicits().stream()
                        .noneMatch(m -> m.modifierId().equals(QualityLimitRules.BREACH_ID)))
            .findFirst()
            .orElseThrow();
    assertThat(plan.branches())
        .allSatisfy(
            b -> assertThat(b.candidates()).extracting(ModifierDefinition::id).contains(target));
    long total = removeCap.candidates().stream().mapToLong(ModifierDefinition::weight).sum();
    long hit =
        removeCap.candidates().stream()
            .filter(d -> d.id().equals(target))
            .mapToLong(ModifierDefinition::weight)
            .sum();
    var route = replay.routes().getFirst();
    var odds = route.steps().getLast().omenOutcomes();
    assertThat(odds.success().probability()).isCloseTo((double) hit / (2 * total), offset(1e-14));
    assertThat(route.probability()).isEqualTo(odds.success().probability());
    assertThat(replay.states().get(route.stateIndices().getLast()).explicits())
        .extracting(ModifierInstance::modifierId)
        .containsExactlyInAnyOrder("amulet:prefix:hale", target);
  }

  /** 임시 상한만 남은 경우 품질 작업 직후 단독 소멸이 확정이며 임의 작업은 가상 타입을 추가하지 않는다. */
  @Test
  void obtainsCapAndCompletesUntypedWorkOnce() {
    var start = fixture.item(ItemState.Rarity.RARE, fixture.strength);
    var found =
        RecipeRoutePlanner.calculate(
            fixture.workbench, start, List.of(empty), s -> true, 3000, true, 40);
    assertThat(found.routes())
        .isNotEmpty()
        .allSatisfy(
            route ->
                assertThat(found.states().get(route.stateIndices().getLast()).explicits())
                    .extracting(ModifierInstance::modifierId)
                    .doesNotContain(QualityLimitRules.BREACH_ID))
        .anySatisfy(
            route -> {
              assertThat(route.steps())
                  .extracting(RecipeRoutePlanner.Step::action)
                  .containsExactly("ESSENCE_BREACH", "QUALITY_40", "ANNULMENT");
              assertThat(route.probability()).isEqualTo(1.0);
              var capped = found.states().get(route.stateIndices().getFirst());
              assertThat(capped.explicits())
                  .extracting(ModifierInstance::modifierId)
                  .contains(QualityLimitRules.BREACH_ID);
              assertThat(found.states().get(route.stateIndices().get(1))).isEqualTo(capped);
              assertThat(found.states().get(route.stateIndices().getLast()).catalystQuality())
                  .isNull();
              assertThat(route.steps().getLast().omens()).isEmpty();
              assertThat(route.steps().getLast().chance().probability()).isEqualTo(1.0);
              assertThat(route.steps().getLast().expectedUses()).isNull();
            });
    for (var incomplete :
        List.of(List.of(step("QUALITY_40")), List.of(step("ESSENCE_BREACH"), step("QUALITY_40"))))
      assertThat(
              RecipeRoutePlanner.evaluateFixed(
                      fixture.workbench, start, empty, s -> true, 1000, true, 40, incomplete)
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
            List.of(step("ESSENCE_BREACH"), step(catalyst.name()), step("ANNULMENT")),
            null,
            catalyst);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            route -> {
              assertThat(route.steps().get(1).expectedUses()).isEqualTo(40.0);
              assertThat(result.states().get(route.stateIndices().getLast()).explicits()).isEmpty();
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

  /** 제작 한도가 하나면 새 제작 목표와 그 더미는 균열 작업이 끝난 후에만 준비한다. */
  @Test
  void reservesLastCraftedSlotUntilQualityWorkIsDone() {
    var item = fixture.item(ItemState.Rarity.RARE);
    var card =
        new RouteCards.Card(
            List.of(
                new RouteCards.Option(
                    1,
                    Map.of(
                        fixture.defences,
                        List.of(new RouteCards.Source("C", "PERFECT_ESSENCE_ENHANCEMENT", null))))),
            0);
    var moves = new RecipeMoves(fixture.workbench);
    assertThat(moves.choices(item, card, Map.of(), false, true))
        .noneMatch(
            c -> c.move().action().equals("PERFECT_ESSENCE_ENHANCEMENT") || c.followup() != null);
    assertThat(moves.choices(item, card, Map.of(), false, false))
        .anyMatch(
            c ->
                c.followup() != null
                    && c.followup().action().equals("PERFECT_ESSENCE_ENHANCEMENT"));
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
            step("QUALITY_40"),
            new RecipeRoutePlanner.Step(
                "ANNULMENT", List.of("Omen_of_Sinistral_Annulment"), List.of(), List.of(), null));
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
                  .containsExactly(cold);
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
            step("QUALITY_40"),
            step("ANNULMENT"));
    var result =
        RecipeRoutePlanner.evaluateFixed(
            fixture.workbench, start, card, s -> true, 4000, true, 40, steps);
    assertThat(result.routes())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.probability()).isEqualTo(0.5);
              assertThat(r.steps().getLast().chance().probability()).isEqualTo(0.5);
              assertThat(r.lucky()).isFalse();
              assertThat(r.order()).isEmpty();
              assertThat(result.states().get(r.stateIndices().getLast()).explicits())
                  .extracting(ModifierInstance::modifierId)
                  .containsExactly(spirit);
            });
  }
}
