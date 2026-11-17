package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.List;

/**
 * 징조 없는 단일 제거·추가 계획을 표본 실행과 전체 분포 계산에 공유한다.
 *
 * <p>기존 계약: No-omen one-removal/one-addition plan shared by sampling and exhaustive enumeration.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code available}: 사용가능 ({@code boolean}).
 *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
 *   <li>{@code branches}: 분기목록 ({@code List<Branch>}).
 *   <li>{@code ACTIONS}: 행동목록 ({@code List<WorkbenchCurrency>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicCurrencyPlan(available, reason, branches)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       available}(사용가능), {@code reason}(이유), {@code branches}(분기목록).
 *   <li>{@code removals()}: removals 값을 조회·구성한다. 반환: {@code List<ModifierInstance>}.
 *   <li>{@code candidatesFor(remaining)}: 후보목록·대상 처리에서 {@code branches.stream}, {@code
 *       b.remaining}을(를) 사용한다. 입력: {@code remaining}(남은). 반환: {@code List<ModifierDefinition>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Branch}: {@code BasicCurrencyPlan}에서 사용하는 분기의 불변 값이다.
 * </ul>
 */
record BasicCurrencyPlan(boolean available, String reason, List<Branch> branches) {
  static final List<WorkbenchCurrency> ACTIONS =
      List.of(
          WorkbenchCurrency.TRANSMUTATION,
          WorkbenchCurrency.GREATER_TRANSMUTATION,
          WorkbenchCurrency.PERFECT_TRANSMUTATION,
          WorkbenchCurrency.AUGMENTATION,
          WorkbenchCurrency.GREATER_AUGMENTATION,
          WorkbenchCurrency.PERFECT_AUGMENTATION,
          WorkbenchCurrency.REGAL,
          WorkbenchCurrency.GREATER_REGAL,
          WorkbenchCurrency.PERFECT_REGAL,
          WorkbenchCurrency.EXALTED,
          WorkbenchCurrency.GREATER_EXALTED,
          WorkbenchCurrency.PERFECT_EXALTED,
          WorkbenchCurrency.ANNULMENT,
          WorkbenchCurrency.CHAOS,
          WorkbenchCurrency.GREATER_CHAOS,
          WorkbenchCurrency.PERFECT_CHAOS);

  BasicCurrencyPlan {
    branches = List.copyOf(branches);
  }

  /**
   * {@code BasicCurrencyPlan}에서 사용하는 분기의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code removed}: 제거된 ({@code ModifierInstance}).
   *   <li>{@code remaining}: 남은 ({@code ItemState}).
   *   <li>{@code candidates}: 후보목록 ({@code List<ModifierDefinition>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Branch(removed, remaining, candidates)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       removed}(제거된), {@code remaining}(남은), {@code candidates}(후보목록).
   * </ul>
   */
  record Branch(
      ModifierInstance removed, ItemState remaining, List<ModifierDefinition> candidates) {
    Branch {
      candidates = List.copyOf(candidates);
    }
  }

  List<ModifierInstance> removals() {
    return branches.stream().map(Branch::removed).filter(java.util.Objects::nonNull).toList();
  }

  List<ModifierDefinition> candidatesFor(ItemState remaining) {
    return branches.stream()
        .filter(b -> b.remaining().equals(remaining))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("State is outside the basic currency plan"))
        .candidates();
  }
}
