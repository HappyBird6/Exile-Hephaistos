package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemState;

/**
 * 일반 장착 용량과 앱의 추가 슬롯 정책을 판정한다.
 *
 * <p>기존 계약: Workbench policy: ordinary capacity plus one; not an in-game Artificer guarantee.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code STOCKY_BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Armours/Gloves/FourGlovesStr1"}
 *       ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AugmentSocketRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supportedState(state)}: 지원·상태 처리에서 {@code state.augmentSockets}, {@code
 *       BaseRegistry.socketExecutionMaximum}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code boolean}.
 *   <li>{@code refusal(state)}: refusal 처리에서 {@code state.observations}, {@code
 *       BaseRegistry.socketExecutionMaximum}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code String}.
 * </ul>
 */
public final class AugmentSocketRules {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  private AugmentSocketRules() {}

  public static boolean supportedState(ItemState state) {
    return state.augmentSockets() == null
        || (BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null
            && state.augmentSockets() >= 0
            && state.augmentSockets() <= BaseRegistry.socketExecutionMaximum(state.baseItemId()));
  }

  public static String refusal(ItemState state) {
    if (state.observations() != null && state.observations().socketCount() != null)
      return "The imported socket count is preserved, but its Artificer capacity has not been established.";
    if (BaseRegistry.socketExecutionMaximum(state.baseItemId()) == null)
      return "Artificer's Orb socket rules are not supported for this equipment base.";
    if (state.augmentSockets() == null)
      return "This saved item's socket count is unknown. Place a new supported equipment base.";
    if (state.augmentSockets() >= BaseRegistry.socketExecutionMaximum(state.baseItemId()))
      return "This base has reached the workbench socket maximum (ordinary capacity plus one).";
    return "";
  }
}
