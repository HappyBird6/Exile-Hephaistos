package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;

/**
 * 다음 화폐 사용의 유한 분기를 표현한다. 수동 공개 선택은 확률 분기가 아니다.
 *
 * <p>기존 계약: Finite next currency step; reveal and manual choices never appear as random branches.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code available}: 사용가능 ({@code boolean}).
 *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
 *   <li>{@code branches}: 분기목록 ({@code List<Branch>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssCraftingPlan(available, reason, branches)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       available}(사용가능), {@code reason}(이유), {@code branches}(분기목록).
 *   <li>{@code blocked(reason)}: 차단의 입력에 따른 분기·결과를 처리한다. 입력: {@code reason}(이유). 반환: {@code
 *       AbyssCraftingPlan}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Branch}: {@code AbyssCraftingPlan}에서 사용하는 분기의 불변 값이다.
 * </ul>
 */
public record AbyssCraftingPlan(boolean available, String reason, List<Branch> branches) {
  public AbyssCraftingPlan {
    branches = List.copyOf(branches);
  }

  /**
   * {@code AbyssCraftingPlan}에서 사용하는 분기의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code removedSlotId}: 제거된·슬롯·식별자 ({@code String}).
   *   <li>{@code removalProbability}: 제거·확률 ({@code double}).
   *   <li>{@code remaining}: 남은 ({@code AbyssCraftingState}).
   *   <li>{@code candidates}: 후보목록 ({@code List<ModifierDefinition>}).
   *   <li>{@code uniformSelection}: 균등·선택 ({@code boolean}).
   *   <li>{@code probabilityDenominator}: 확률·분모 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Branch(removedSlotId, removalProbability, remaining, candidates,
   *       uniformSelection)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code removedSlotId}(제거된·슬롯·식별자),
   *       {@code removalProbability}(제거·확률), {@code remaining}(남은), {@code candidates}(후보목록),
   *       {@code uniformSelection}(균등·선택).
   *   <li>{@code Branch(removedSlotId, removalProbability, remaining, candidates, uniformSelection,
   *       probabilityDenominator)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code removedSlotId}(제거된·슬롯·식별자),
   *       {@code removalProbability}(제거·확률), {@code remaining}(남은), {@code candidates}(후보목록),
   *       {@code uniformSelection}(균등·선택), {@code probabilityDenominator}(확률·분모).
   * </ul>
   */
  public record Branch(
      String removedSlotId,
      double removalProbability,
      AbyssCraftingState remaining,
      List<ModifierDefinition> candidates,
      boolean uniformSelection,
      long probabilityDenominator) {
    public Branch(
        String removedSlotId,
        double removalProbability,
        AbyssCraftingState remaining,
        List<ModifierDefinition> candidates,
        boolean uniformSelection) {
      this(removedSlotId, removalProbability, remaining, candidates, uniformSelection, 0);
    }

    public Branch {
      candidates = List.copyOf(candidates);
    }
  }

  public static AbyssCraftingPlan blocked(String reason) {
    return new AbyssCraftingPlan(false, reason, List.of());
  }
}
