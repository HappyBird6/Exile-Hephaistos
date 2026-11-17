package com.poe2craft.craftingsupport.search.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 자동·개별행동·진행커서 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code f}: f ({@code BasicCurrencyFixture}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code kernel}: 전이커널 ({@code AutomaticCraftingKernel}).
 *   <li>{@code root}: 시작점 ({@code ItemState}).
 *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code AutomaticCraftingAction}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code batchesRetainUnenumeratedMassAndMatchIndependentUniformLaw()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code suspendedExecutionDoesNotChargeTwiceAndRestoresPendingWork()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code limitRemainsUnknownAndCanResumeWithoutRebase()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
class AutomaticPrimitiveCursorTest {
  final BasicCurrencyFixture f = new BasicCurrencyFixture();
  final ItemCatalog catalog =
      f.catalog(
          List.of(
              f.implicit,
              f.definition("other", ModifierDefinition.AffixType.SUFFIX, 257, 512, 1),
              f.definition("wide", ModifierDefinition.AffixType.PREFIX, 1, 256, 1)));
  final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          AutomaticCurrencyPolicyLoader.load(),
          catalog,
          new WorkbenchSimulator(catalog, new CraftingEngine(catalog)),
          "amulets");
  final ItemState root = f.item(ItemState.Rarity.RARE, List.of());
  final AutomaticCraftingAction action =
      new AutomaticCraftingAction(
          WorkbenchCurrency.EXALTED, Set.of(), AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION);

  @Test
  void batchesRetainUnenumeratedMassAndMatchIndependentUniformLaw() {
    var cursor = kernel.deferredCursor(root, Map.of(), action, 1000);
    cursor.advance(3);
    assertThat(cursor.finished()).isFalse();
    assertThat(cursor.retainedStates()).isEqualTo(3);
    assertThat(cursor.result().unresolved()).isEqualTo(Fraction.of(509, 512));
    while (!cursor.finished()) cursor.advance(7);
    var result = cursor.result();
    assertThat(result.outcomes()).hasSize(512);
    assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
    assertThat(result.outcomes())
        .allSatisfy(o -> assertThat(o.probability()).isEqualTo(Fraction.of(1, 512)));
    assertThat(
            result.outcomes().stream()
                .map(o -> o.item().explicits().getFirst().values().values().iterator().next())
                .toList())
        .containsExactlyInAnyOrderElementsOf(
            java.util.stream.LongStream.rangeClosed(1, 512).boxed().toList());
  }

  @Test
  void suspendedExecutionDoesNotChargeTwiceAndRestoresPendingWork() {
    var template = new AutomaticMethodExecution.Template("wide", List.of(), List.of(action));
    try (var execution =
        new AutomaticMethodExecution(
            root,
            template,
            kernel,
            item ->
                item.explicits().isEmpty()
                    ? Optional.empty()
                    : Optional.of(ExactMethodRunner.Exit.CHECKPOINT),
            CertifiedInterval.Precision.standard(),
            1000)) {
      execution.beginCycle();
      execution.step(64);
      var pending = execution.provisional(100);
      assertThat(pending.exits().get(ExactMethodRunner.Exit.REBASE)).isEmpty();
      assertThat(pending.exits().get(ExactMethodRunner.Exit.UNRESOLVED).values())
          .anyMatch(v -> v.contains(Fraction.ONE));
      assertThat(pending.cycleUses().get(action).lower()).isEqualTo(Fraction.ZERO);
      var checkpoint = execution.checkpoint("goal");
      try (var restored =
          AutomaticMethodExecution.restore(
              checkpoint,
              kernel,
              "goal",
              item ->
                  item.explicits().isEmpty()
                      ? Optional.empty()
                      : Optional.of(ExactMethodRunner.Exit.CHECKPOINT))) {
        for (int i = 0; i < 30 && !restored.atBoundary(); i++) restored.step(64);
        assertThat(restored.atBoundary()).isTrue();
        assertThat(restored.observation().cycleUses().get(action))
            .isEqualTo(CertifiedInterval.one(64));
        assertThat(restored.observation().mass()).isEqualTo(CertifiedInterval.one(64));
      }
    }
  }

  @Test
  void limitRemainsUnknownAndCanResumeWithoutRebase() {
    var template = new AutomaticMethodExecution.Template("limited", List.of(), List.of(action));
    try (var execution =
        new AutomaticMethodExecution(
            root,
            template,
            kernel,
            item -> Optional.empty(),
            CertifiedInterval.Precision.standard(),
            1)) {
      execution.beginCycle();
      execution.step(64);
      assertThat(execution.resourceLimited()).isTrue();
      assertThat(execution.provisional(100).exits().get(ExactMethodRunner.Exit.REBASE)).isEmpty();
      execution.increasePrimitiveBudget(1000);
      for (int i = 0; i < 30 && !execution.atBoundary(); i++) execution.step(64);
      assertThat(execution.atBoundary()).isTrue();
      assertThat(execution.observation().cycleUses().get(action))
          .isEqualTo(CertifiedInterval.one(64));
    }
  }
}
