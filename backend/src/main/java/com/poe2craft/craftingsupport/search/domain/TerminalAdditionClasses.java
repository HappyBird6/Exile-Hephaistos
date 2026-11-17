package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 흡수 상태로 끝나는 유한 옵션 추가 구간에 대한 동치류를 증명한다.
 *
 * <p>기존 계약: A deliberately bounded proof for an absorbing, addition-only suffix of a plan. Every
 * eligible action and every scalar outcome is compared, including its exact mass. Concrete incoming
 * items remain separate transport inputs; no representative is averaged.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code TerminalAdditionClasses()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code prove(item, quality40, progress, choices, kernel, classify, outcomeBudget)}: 증명의 입력에
 *       따른 분기·결과를 처리한다. 입력: {@code item}(아이템), {@code quality40}(품질·40), {@code progress}(진행상황),
 *       {@code choices}(선택목록), {@code kernel}(전이커널), {@code classify}(상태분류), {@code
 *       outcomeBudget}(결과·계산예산). 반환: {@code Optional<Key>}.
 *   <li>{@code prove(item, quality40, progress, choices, kernel, classify, outcomeBudget,
 *       recordWork)}: 증명 처리에서 {@code item.desecrated}, {@code choices.stream}을(를) 사용한다. 입력: {@code
 *       item}(아이템), {@code quality40}(품질·40), {@code progress}(진행상황), {@code choices}(선택목록), {@code
 *       kernel}(전이커널), {@code classify}(상태분류), {@code outcomeBudget}(결과·계산예산), {@code
 *       recordWork}(기록·계산작업). 반환: {@code Optional<Key>}.
 *   <li>{@code transport(representative, actual, result)}: 전송 처리에서 {@code result.explicits}, {@code
 *       representative.explicits}을(를) 사용한다. 입력: {@code representative}(대표상태), {@code actual}(실제값),
 *       {@code result}(결과). 반환: {@code ItemState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code withExplicits(item, modifiers)}: 명시옵션목록을(를) 반영한 값을 구한다. 입력: {@code item}(아이템),
 *       {@code modifiers}(옵션목록). 반환: {@code ItemState}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Row}: {@code TerminalAdditionClasses}에서 사용하는 행의 불변 값이다.
 *   <li>{@code Law}: {@code TerminalAdditionClasses}에서 사용하는 분포의 불변 값이다.
 *   <li>{@code Key}: {@code TerminalAdditionClasses}에서 사용하는 키의 불변 값이다.
 * </ul>
 */
public final class TerminalAdditionClasses {
  /**
   * {@code TerminalAdditionClasses}에서 사용하는 행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code addition}: 추가 ({@code ItemState}).
   *   <li>{@code exit}: 종료 ({@code Exit}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   */
  public record Row(ItemState addition, Exit exit, Fraction probability) {}

  /**
   * {@code TerminalAdditionClasses}에서 사용하는 분포의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code AutomaticCraftingAction}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Row>}).
   * </ul>
   */
  public record Law(AutomaticCraftingAction action, List<Row> outcomes) {}

  /**
   * {@code TerminalAdditionClasses}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code context}: 문맥 ({@code ItemState}).
   *   <li>{@code roles}: roles ({@code List<AutomaticCraftingKernel.RetainedModifierRole>}).
   *   <li>{@code quality40}: 품질·40 ({@code boolean}).
   *   <li>{@code progress}: 진행상황 ({@code AutomaticMethodGraph.Progress}).
   *   <li>{@code laws}: 분포목록 ({@code List<Law>}).
   * </ul>
   */
  public record Key(
      ItemState context,
      List<AutomaticCraftingKernel.RetainedModifierRole> roles,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<Law> laws) {}

  private TerminalAdditionClasses() {}

  public static Optional<Key> prove(
      ItemState item,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<GoalDirectedPlanner.Choice> choices,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      long outcomeBudget) {
    return prove(
        item, quality40, progress, choices, kernel, classify, outcomeBudget, ignored -> {});
  }

  public static Optional<Key> prove(
      ItemState item,
      boolean quality40,
      AutomaticMethodGraph.Progress progress,
      List<GoalDirectedPlanner.Choice> choices,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Optional<Exit>> classify,
      long outcomeBudget,
      java.util.function.LongConsumer recordWork) {
    if (!item.desecrated().isEmpty()
        || choices.stream().anyMatch(c -> c.cycle().stream().anyMatch(a -> a.operation() != null))
        || choices.isEmpty()
        || AutomaticCraftingKernel.hasQualityCap(item)) return Optional.empty();
    // This is only a proof-work cap. An unproved state keeps its ordinary exact execution.
    long remainingProofRows = Math.min(outcomeBudget, 512);
    var laws = new ArrayList<Law>();
    for (var choice : choices) {
      if (!choice.initial().isEmpty() || choice.cycle().size() != 1) return Optional.empty();
      var action = choice.cycle().getFirst();
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
          || action.purpose() == AutomaticCurrencyPolicy.Purpose.TARGET_PREPARATION)
        return Optional.empty();
      var plan = kernel.plan(item, action);
      if (plan.status() != AutomaticCraftingPlan.Status.READY
          || plan.branches().stream()
              .anyMatch(b -> !b.remaining().explicits().equals(item.explicits())))
        return Optional.empty();
      if (remainingProofRows <= 0) return Optional.empty();
      var result = kernel.expand(item, action, remainingProofRows);
      recordWork.accept(result.outcomes().size());
      remainingProofRows -= result.outcomes().size();
      if (result.status() != AutomaticCraftingKernel.Status.COMPLETE) return Optional.empty();
      var rows = new ArrayList<Row>();
      var loss = Fraction.ZERO;
      for (var outcome : result.outcomes()) {
        if (AutomaticCraftingKernel.hasQualityCap(outcome.item())) return Optional.empty();
        var added = new ArrayList<>(outcome.item().explicits());
        for (var existing : item.explicits()) if (!added.remove(existing)) return Optional.empty();
        if (added.size() != 1) return Optional.empty();
        var exit = classify.apply(outcome.item()).orElse(Exit.LOSS);
        if (exit != Exit.GOAL && exit != Exit.LOSS) return Optional.empty();
        if (exit == Exit.LOSS) loss = loss.add(outcome.probability());
        else rows.add(new Row(withExplicits(outcome.item(), added), exit, outcome.probability()));
      }
      // Failed items are absorbed and never continue; only their exact total mass is observable.
      if (!loss.equals(Fraction.ZERO)) rows.add(new Row(null, Exit.LOSS, loss));
      laws.add(new Law(action, List.copyOf(rows)));
    }
    return Optional.of(
        new Key(
            withExplicits(item, List.of()),
            kernel.retainedModifierRoles(item),
            quality40,
            progress,
            List.copyOf(laws)));
  }

  /** Rebuild the exact member's result, preserving its own tier and scalar values. */
  public static ItemState transport(ItemState representative, ItemState actual, ItemState result) {
    var additions = new ArrayList<>(result.explicits());
    for (var existing : representative.explicits())
      if (!additions.remove(existing))
        throw new IllegalArgumentException("Not an addition-only result");
    additions.addAll(actual.explicits());
    return withExplicits(result, additions);
  }

  private static ItemState withExplicits(ItemState item, List<ModifierInstance> modifiers) {
    return new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            modifiers,
            item.conditions(),
            item.augmentSockets(),
            item.catalystQuality())
        .withAugments(item.installedAugments())
        .withQualityInfused(item.qualityInfused());
  }
}
