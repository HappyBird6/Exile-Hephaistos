package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.QualityCapChangePolicy;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 가중치가 1·3·2인 세 후보로 같은 아이템에 반복하는 카오스의 적중 질량과 중단 시 사용량을 검사한다. */
class BoundedChaosRepeatTest {
  /** a는 접두(1), b는 접미(3)이며 공통 자료에서 같은 아이템 레벨과 슬롯 규칙을 가져온다. */
  private final BasicCurrencyFixture fixture = new BasicCurrencyFixture();

  /** 접두 c(2)를 더해 목표 적중, 재시도 가능 실패, 재시도 불가능 실패를 독립적으로 구분한다. */
  private final ItemCatalog catalog =
      fixture.catalog(
          List.of(
              fixture.implicit,
              fixture.a,
              fixture.b,
              fixture.definition("c", ModifierDefinition.AffixType.PREFIX, 1, 1, 2)));

  /** 모든 제거와 추가는 실제 작업대에서 처리한다. */
  private final WorkbenchSimulator workbench =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));

  /** 수치 범위가 없는 목표이므로 같은 티어의 결과는 합친다. */
  private final RecipeTransitions transitions = new RecipeTransitions(workbench, true);

  /** 원하는 명시 옵션을 보유한 희귀 아이템을 준비한다. */
  private ItemState rare(ModifierInstance... mods) {
    return fixture.item(ItemState.Rarity.RARE, List.of(mods));
  }

  /** 해당 옵션을 가진 적중 상태들의 확률을 원래 분모 그대로 합산한다. */
  private Fraction mass(RecipeTransitions.Law law, String id) {
    return law.outcomes().stream()
        .filter(o -> o.state().explicits().stream().anyMatch(m -> m.modifierId().equals(id)))
        .map(RecipeTransitions.Outcome::probability)
        .reduce(Fraction.ZERO, Fraction::add);
  }

  /** 같은 추첨 풀이 계속되는 경우에도 최대 사용 횟수에서 계산을 중단한다. */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 1000})
  void stopsAtTheSpecifiedCapOnTheSameBase(int limit) {
    var start = rare(fixture.mod("b", 3));
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a"),
            Set.of(),
            limit,
            10000);
    var first = Fraction.of(1, 6);
    var withinLimit = Fraction.of(5, 6).pow(limit).complement();
    assertThat(result.firstHits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "a")).isEqualTo(first);
    assertThat(result.hits().complete()).isTrue();
    assertThat(mass(result.hits(), "a")).isEqualTo(withinLimit);
    assertThat(result.expectedUses()).isEqualTo(withinLimit.divide(first));
  }

  /** 접미 제거 징조는 b가 다시 나올 때만 재시도할 수 있다. 접두 c가 뜬 뒤에는 화폐를 소모하지 않는다. */
  @Test
  void rerunsOnlyTheMissesThatStillHaveARemovableSuffix() {
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            rare(fixture.mod("b", 3)),
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.DEXTRAL_ERASURE),
            Set.of("a"),
            Set.of(),
            2,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "a")).isEqualTo(Fraction.of(1, 6));
    // 첫 시도 1/6에 재시도 가능한 3/6 중 1/6을 더한다.
    assertThat(mass(result.hits(), "a")).isEqualTo(Fraction.of(1, 4));
    assertThat(result.expectedUses()).isEqualTo(Fraction.of(3, 2));
  }

  /** 명시 옵션이 두 개면 카오스 반복에 진입하지 않는다. */
  @Test
  void twoExplicitOptionsDoNotStartTheRepeat() {
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            rare(fixture.mod("a", 1), fixture.mod("b", 3)),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("c"),
            Set.of(),
            1000,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(result.hits().outcomes()).isEmpty();
    assertThat(result.expectedUses()).isEqualTo(Fraction.ZERO);
  }

  /** 분열 a가 별도로 붙은 상태에서는 비분열 b 하나만 교체하고 a의 원래 수치를 그대로 남긴다. */
  @Test
  void aFractureAndOneMutableAffixCanRepeatOnThatSameBase() {
    var fracture = new ModifierInstance("a", java.util.Map.of("a", 1L), true);
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            rare(fracture, fixture.mod("b", 3)),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a", "c"),
            Set.of("a"),
            2,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "c")).isEqualTo(Fraction.of(2, 5));
    assertThat(mass(result.hits(), "c")).isEqualTo(Fraction.of(16, 25));
    assertThat(result.expectedUses()).isEqualTo(Fraction.of(8, 5));
    assertThat(result.hits().outcomes())
        .allSatisfy(
            o -> {
              assertThat(o.state().explicits()).contains(fracture);
              assertThat(o.removedModifierIds()).doesNotContain("a");
            });
  }

  /** 다른 목표 c가 먼저 나오면 그 결과 역시 흡수되어 이후의 a 확률에 다시 쓰이지 않는다. */
  @Test
  void anyFirstAcquisitionOfEitherGoalStopsTheRepeat() {
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            rare(fixture.mod("b", 3)),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a", "c"),
            Set.of(),
            2,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "c")).isEqualTo(Fraction.of(1, 3));
    assertThat(mass(result.hits(), "a")).isEqualTo(Fraction.of(1, 4));
    assertThat(mass(result.hits(), "c")).isEqualTo(Fraction.of(1, 2));
    assertThat(result.expectedUses()).isEqualTo(Fraction.of(3, 2));
  }

  /** 목표의 두 수치만 열거할 수 있어도 같은 풀의 모든 비목표 가중치와 실제 종착 수치를 보존한다. */
  @Test
  void budgetsOnlyTheTargetRollsWhenEveryMissRestoresThePool() {
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            new RecipeTransitions(workbench),
            rare(fixture.mod("c", 1)),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a"),
            Set.of(),
            2,
            2);

    assertThat(result.work()).isEqualTo(2);
    assertThat(result.firstHits().complete()).isTrue();
    assertThat(result.firstHits().removableModifierIds()).containsExactly("c");
    assertThat(result.firstHits().outcomes())
        .extracting(RecipeTransitions.Outcome::probability)
        .containsExactly(Fraction.of(1, 12), Fraction.of(1, 12));
    assertThat(result.hits().complete()).isTrue();
    assertThat(result.hits().removableModifierIds()).containsExactlyInAnyOrder("b", "c");
    assertThat(result.hits().outcomes())
        .extracting(o -> o.state().explicits().getFirst().values().get("a"))
        .containsExactly(1L, 2L);
    assertThat(result.hits().outcomes())
        .extracting(RecipeTransitions.Outcome::probability)
        .containsExactly(Fraction.of(11, 72), Fraction.of(11, 72));
    assertThat(result.expectedUses()).isEqualTo(Fraction.of(11, 6));
  }

  /** 품질 상한 제거를 금지한 작업대에서는 해당 옵션이 뜬 뒤 반복이 중단되는 전이를 보존한다. */
  @Test
  void aMissThatLocksTheNextRemovalCannotBeTreatedAsARetry() {
    var maximumQuality =
        new ModifierDefinition(
            QualityLimitRules.BREACH_ID,
            "maximum quality",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("LocalMaximumQuality"),
            1,
            2,
            1,
            "+20 to maximum quality",
            List.of(new ModifierDefinition.StatRange(QualityLimitRules.STAT_ID, 20, 20)),
            Set.of(),
            "https://example.test/maximum-quality");
    var rules = fixture.catalog(List.of(fixture.implicit, fixture.a, fixture.b, maximumQuality));
    var rejecting =
        new WorkbenchSimulator(
            rules,
            new CraftingEngine(rules),
            Set.of(),
            Map.of(),
            Map.of(),
            QualityCapChangePolicy.REJECT_OVERCAP);
    var base = rare(fixture.mod("b", 3));
    var start =
        new ItemState(
            base.snapshotId(),
            base.baseItemId(),
            base.itemLevel(),
            base.rarity(),
            base.implicits(),
            base.explicits(),
            base.conditions(),
            null,
            new CatalystQuality(CatalystQuality.Type.NEURAL, 30));
    var result =
        BoundedChaosRepeat.calculate(
            rejecting,
            new RecipeTransitions(rejecting, true),
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a"),
            Set.of(),
            2,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "a")).isEqualTo(Fraction.of(1, 6));
    assertThat(mass(result.hits(), "a")).isEqualTo(Fraction.of(1, 4));
    assertThat(result.expectedUses()).isEqualTo(Fraction.of(3, 2));
  }

  /** 단독으로 남은 L이 제거되면 같은 옵션이나 다른 목표를 다시 뽑아도 실패로 끝난다. */
  @Test
  void neverRetriesOrCreditsAnOutcomeThatRemovedAProtectedOption() {
    var start = rare(fixture.mod("a", 1));
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("c"),
            Set.of("a"),
            2,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(result.hits().outcomes()).isEmpty();
    assertThat(mass(result.firstHits(), "c")).isEqualTo(Fraction.ZERO);
    assertThat(result.expectedUses()).isEqualTo(Fraction.ONE);
  }

  /** 적법한 결과를 모두 셀 수 없거나 카오스에서 확인되지 않은 징조이면 사용량은 미확정이다. */
  @Test
  void incompleteBudgetOrUnsupportedOmenLeavesExpectedUsesUnknown() {
    var start = rare(fixture.mod("a", 1));
    var exhausted =
        BoundedChaosRepeat.calculate(
            workbench,
            new RecipeTransitions(workbench),
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("b"),
            Set.of(),
            5,
            1);
    assertThat(exhausted.hits().complete()).isFalse();
    assertThat(exhausted.firstHits().complete()).isFalse();
    // 목표 b의 가중치 3/6 중 값 3이 나오는 절반만 열거했다. 이후에 같은 상태로 적중할 질량은 아직 모른다.
    assertThat(exhausted.firstHits().outcomes())
        .singleElement()
        .satisfies(
            o -> {
              assertThat(o.probability()).isEqualTo(Fraction.of(1, 4));
              assertThat(o.state().explicits()).containsExactly(fixture.mod("b", 3));
            });
    assertThat(exhausted.hits().outcomes()).isEqualTo(exhausted.firstHits().outcomes());
    var full =
        BoundedChaosRepeat.calculate(
            workbench,
            new RecipeTransitions(workbench),
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("b"),
            Set.of(),
            5,
            100);
    assertThat(full.hits().complete()).isTrue();
    assertThat(full.hits().outcomes())
        .filteredOn(o -> o.state().equals(exhausted.hits().outcomes().getFirst().state()))
        .singleElement()
        .satisfies(o -> assertThat(o.probability()).isEqualTo(Fraction.of(31, 64)));
    assertThat(exhausted.expectedUses()).isNull();

    var unsupported =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT),
            Set.of("c"),
            Set.of(),
            5,
            100);
    assertThat(unsupported.hits().complete()).isFalse();
    assertThat(unsupported.expectedUses()).isNull();
  }

  /** 방향 징조의 첫 시도는 완전히 셌지만 이후 재시도가 예산을 넘으면 첫 시도의 적중 질량은 여전히 하한이다. */
  @Test
  void keepsTheVerifiedFirstHitWhenLaterDirectedAttemptsAreIncomplete() {
    var start = rare(fixture.mod("b", 3));
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            start,
            WorkbenchCurrency.CHAOS,
            Set.of(WorkbenchOmen.DEXTRAL_ERASURE),
            Set.of("a"),
            Set.of(),
            2,
            3);
    assertThat(result.firstHits().complete()).isTrue();
    assertThat(mass(result.firstHits(), "a")).isEqualTo(Fraction.of(1, 6));
    assertThat(result.hits().complete()).isFalse();
    assertThat(mass(result.hits(), "a")).isEqualTo(Fraction.of(1, 6));
    assertThat(result.expectedUses()).isNull();
  }

  /** 시작부터 제거할 옵션이 없으면 목표 적중과 화폐 소비가 모두 0임이 확정된다. */
  @Test
  void aKnownInapplicableFirstUseCostsNothing() {
    var result =
        BoundedChaosRepeat.calculate(
            workbench,
            transitions,
            rare(),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            Set.of("a"),
            Set.of(),
            1000,
            100);
    assertThat(result.hits().complete()).isTrue();
    assertThat(result.hits().outcomes()).isEmpty();
    assertThat(result.expectedUses()).isEqualTo(Fraction.ZERO);
  }
}
