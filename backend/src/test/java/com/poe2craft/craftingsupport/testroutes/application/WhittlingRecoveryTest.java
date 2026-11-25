package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 요구 레벨·방향·분열·보호 이력이 다른 실제 작업대 분기를 이용해 절사 카오스 미적중의 세부 판정을 검증한다. */
class WhittlingRecoveryTest {
  /** 모두 가중치 1의 고정 수치를 사용한다. 옵션 티어 번호는 같고 제거를 결정하는 요구 레벨만 다르다. */
  private final BasicCurrencyFixture fixture = new BasicCurrencyFixture();

  private final WorkbenchSimulator workbench = workbench();

  /** 접두와 접미의 최저 요구 레벨을 독립적으로 바꿀 수 있는 카탈로그를 만든다. */
  private WorkbenchSimulator workbench() {
    var catalog =
        fixture.catalog(
            List.of(
                fixture.implicit,
                tier("old", AffixType.PREFIX, 5),
                tier("prefix10", AffixType.PREFIX, 10),
                tier("prefix30", AffixType.PREFIX, 30),
                tier("prefix30-tie", AffixType.PREFIX, 30),
                tier("suffix10", AffixType.SUFFIX, 10),
                tier("suffix30", AffixType.SUFFIX, 30),
                tier("suffix50", AffixType.SUFFIX, 50),
                tier("desired", AffixType.SUFFIX, 40)));
    return new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  }

  /** 정의의 requiredItemLevel은 주어진 원본 레벨이며 각 id의 계열은 서로 충돌하지 않는다. */
  private static ModifierDefinition tier(String id, AffixType side, int requiredItemLevel) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        side,
        Set.of(id),
        requiredItemLevel,
        1,
        1,
        id,
        List.of(new ModifierDefinition.StatRange(id, 1, 1)),
        Set.of(),
        "https://example.test/" + id);
  }

  /** 미적중 후 실제로 붙어 있는 비분열 옵션과 암시 옵션을 보존해 희귀 아이템을 만든다. */
  private ItemState rare(String... ids) {
    return fixture.item(
        ItemState.Rarity.RARE, java.util.Arrays.stream(ids).map(id -> fixture.mod(id, 1)).toList());
  }

  /** 원래 왼쪽 말소를 썼어도 반대쪽에서 새 옵션을 제거할 수 있으면 새 회차를 시작할 수 있다. */
  @Test
  void retriesTheMissOnTheOppositeSideOfTheSameItem() {
    var start = rare("prefix10", "suffix50", "old");
    var original = Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.SINISTRAL_ERASURE);
    var miss =
        new RecipeTransitions(workbench)
            .successors(start, "CHAOS", original, Set.of("suffix30"), 100, false)
            .outcomes()
            .getFirst();
    assertThat(miss.removedModifierIds()).containsExactly("old");
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                original,
                miss.state(),
                "suffix30",
                miss.removedModifierIds(),
                Set.of("prefix10", "suffix50"),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.RECOVERABLE);
  }

  /** 접두 최저가 동률이면 보호 접두와 새 접두 중 새 접두를 지울 확률이 실제로 있으므로 재시도 가능하다. */
  @Test
  void acceptsATieForTheNewModifierButRejectsAnAlreadyLostProtectedOne() {
    var item = rare("prefix30", "suffix10", "prefix30-tie");
    var omens = Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.SINISTRAL_ERASURE);
    var removals = workbench.recipePlan(item, WorkbenchCurrency.CHAOS, omens).branches();
    assertThat(removals).hasSize(2);
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                omens,
                item,
                "prefix30-tie",
                Set.of("old"),
                Set.of("prefix30"),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.RECOVERABLE);
    // 보호 옵션을 방금 지우고 같은 ID가 다시 붙었어도 제거 사건은 되돌릴 수 없다.
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                omens,
                item,
                "prefix30",
                Set.of("prefix30"),
                Set.of("prefix30"),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.FAILURE);
  }

  /** 같은 쪽에 더 낮은 기존 옵션이 남아 있으면 실패하고, 그 옵션이 분열이면 새 옵션은 제거할 수 있다. */
  @Test
  void followsTheActualUnfracturedRemovalCandidates() {
    var omens = Set.of(WorkbenchOmen.WHITTLING);
    var item = rare("prefix10", "suffix10", "prefix30");
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                omens,
                item,
                "prefix30",
                Set.of("old"),
                Set.of(),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.FAILURE);
    var fractured =
        fixture.item(
            ItemState.Rarity.RARE,
            List.of(
                new ModifierInstance("prefix10", Map.of("prefix10", 1L), true),
                fixture.mod("suffix10", 1),
                fixture.mod("prefix30", 1)));
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                omens,
                fractured,
                "prefix30",
                Set.of("old"),
                Set.of("prefix10"),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.RECOVERABLE);
  }

  /** 새 옵션을 지워도 목표와 같은 계열이 이미 남아 있으면 실제 다음 풀에서 목표는 나올 수 없다. */
  @Test
  void requiresTargetWeightInThePoolAfterRemovingTheNewModifier() {
    var item = rare("desired", "prefix10");
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                Set.of(WorkbenchOmen.WHITTLING),
                item,
                "prefix10",
                Set.of("old"),
                Set.of(),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.FAILURE);
  }

  /** 미공개 훼손 슬롯의 등장 요구 레벨은 모르므로 실제 제거 분포가 확정될 때까지 미확정으로 유지한다. */
  @Test
  void unknownRequirementOfAnUnrevealedSlotDoesNotProveFailure() {
    var item =
        rare("prefix10")
            .withDesecrated(
                List.of(new ItemState.DesecratedSlot("hidden", AffixType.SUFFIX, null)));
    assertThat(
            WhittlingRecovery.classifyMiss(
                workbench,
                WorkbenchCurrency.CHAOS,
                Set.of(WorkbenchOmen.WHITTLING),
                item,
                "prefix10",
                Set.of("old"),
                Set.of(),
                Set.of("desired")))
        .isEqualTo(WhittlingRecovery.Kind.UNKNOWN);
  }
}
