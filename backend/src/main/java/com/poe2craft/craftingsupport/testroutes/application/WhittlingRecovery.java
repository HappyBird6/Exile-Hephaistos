package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** 절사 카오스가 빗나간 실제 아이템에서 새 옵션을 제거하고 기존 목표를 다시 뽑을 수 있는지 검사한다. */
final class WhittlingRecovery {
  /** 제작 경로에 쓸 수 있는 절사 단독·왼쪽 말소 병용·오른쪽 말소 병용 조합이다. */
  static final List<Set<WorkbenchOmen>> OMEN_COMBINATIONS =
      List.of(
          Set.of(WorkbenchOmen.WHITTLING),
          Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.SINISTRAL_ERASURE),
          Set.of(WorkbenchOmen.WHITTLING, WorkbenchOmen.DEXTRAL_ERASURE));

  /** RECOVERABLE은 한 번 더 노릴 수 있음, FAILURE는 보호 위반 또는 재시도 불가, UNKNOWN은 전이 미확정이다. */
  enum Kind {
    RECOVERABLE,
    FAILURE,
    UNKNOWN
  }

  /** 계산은 아이템이나 작업대의 공개 상태를 변경하지 않는다. */
  private WhittlingRecovery() {}

  /**
   * 성공은 호출자가 실제 획득 단계에서 검사한다. 미적중 때에는 이번에 제거된 보호 옵션이 있으면 실패다. 그 외에는 새 옵션이 다음 제거 후보에 들고, 제거한 그 자리에서
   * 목표를 추첨할 수 있는지를 같은 아이템의 작업대 계획으로 검사한다. 동률에서 새 옵션을 지울 수 있는 분기 하나면 충분하다.
   */
  static Kind classifyMiss(
      WorkbenchSimulator simulator,
      WorkbenchCurrency currency,
      Set<WorkbenchOmen> omens,
      ItemState after,
      String addedId,
      Set<String> removedIds,
      Set<String> protectedIds,
      Set<String> targetIds) {
    return classifyMiss(
        simulator, currency, omens, after, addedId, removedIds, protectedIds, targetIds, null);
  }

  /** 제거할 옵션을 지정했는데 동률로 다른 옵션이 지워졌다면, 남은 지정 옵션을 실제로 교체할 수 있어야 복구 가능하다. */
  static Kind classifyMiss(
      WorkbenchSimulator simulator,
      WorkbenchCurrency currency,
      Set<WorkbenchOmen> omens,
      ItemState after,
      String addedId,
      Set<String> removedIds,
      Set<String> protectedIds,
      Set<String> targetIds,
      String requiredRemoval) {
    if (currency.baseAction() != CraftingAction.CHAOS || !OMEN_COMBINATIONS.contains(omens))
      return Kind.UNKNOWN;
    if (!Collections.disjoint(removedIds, protectedIds) || protectedIds.contains(addedId))
      return Kind.FAILURE;
    if (targetIds.isEmpty()
        || after.explicits().stream().noneMatch(m -> m.modifierId().equals(addedId)))
      return Kind.UNKNOWN;
    // 미공개 훼손 슬롯은 제거 후보의 요구 레벨이 검증되지 않아 절사 재시도를 판정할 수 없다.
    if (after.desecrated().stream().anyMatch(s -> s.modifierId() == null)) return Kind.UNKNOWN;
    var workbench = simulator.forItem(after);
    var known =
        after.desecrated().isEmpty()
            ? after
            : AbyssCraftingState.from(after, workbench.catalog()).knownItem();
    String retryRemoval =
        requiredRemoval != null && !removedIds.contains(requiredRemoval)
            ? requiredRemoval
            : addedId;
    boolean uncertain = false;
    for (var retryOmens : OMEN_COMBINATIONS) {
      // 제거할 옵션이 최저 요구 레벨 후보가 아니면 이 조합의 모든 교체 풀을 다시 만들 필요가 없다.
      if (workbench.chaosRemovalCandidates(known, currency, retryOmens).stream()
          .noneMatch(m -> m.modifierId().equals(retryRemoval))) continue;
      var plan = workbench.recipePlan(known, currency, retryOmens);
      if (plan.status() == AutomaticCraftingPlan.Status.UNSUPPORTED) uncertain = true;
      for (var branch : plan.branches()) {
        if (branch.remaining().explicits().stream()
            .anyMatch(m -> m.modifierId().equals(retryRemoval))) continue;
        if (branch.candidates().stream()
            .anyMatch(d -> d.weight() > 0 && targetIds.contains(d.id()))) return Kind.RECOVERABLE;
      }
    }
    return uncertain ? Kind.UNKNOWN : Kind.FAILURE;
  }
}
