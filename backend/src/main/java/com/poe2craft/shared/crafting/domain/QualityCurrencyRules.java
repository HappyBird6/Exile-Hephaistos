package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.ItemState;

/**
 * 장비 품질 화폐의 사용과 기존 예외 품질 보존을 처리한다.
 *
 * <p>기존 계약: User-selected ordinary equipment quality policy; inherited exceptional quality is
 * preserved.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "user-selected-equipment-quality-maximum-v2"} ({@code
 *       String}).
 *   <li>{@code ORDINARY_MAXIMUM}: 최대값; 초기값 {@code 20} ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code QualityCurrencyRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code equipment(baseItemId)}: 장비 처리에서 {@code baseItemId.startsWith}을(를) 사용한다. 입력: {@code
 *       baseItemId}(베이스·아이템·식별자). 반환: {@code boolean}.
 *   <li>{@code martial(baseItemId)}: martial 처리에서 {@code baseItemId.startsWith}, {@code
 *       baseItemId.contains}을(를) 사용한다. 입력: {@code baseItemId}(베이스·아이템·식별자). 반환: {@code boolean}.
 *   <li>{@code isQuality(action)}: 품질을(를) 여부를 판정한다. 입력: {@code action}(행동). 반환: {@code boolean}.
 *   <li>{@code refusal(state, action)}: refusal 처리에서 {@code state.conditions}, {@code
 *       state.rarity}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code String}.
 *   <li>{@code maximum(state)}: 최대값 처리에서 {@code AugmentCraftingRules.maximumQuality}을(를) 사용한다. 입력:
 *       {@code state}(상태). 반환: {@code int}.
 *   <li>{@code nextQuality(state)}: 다음·품질 처리에서 {@code state.equipmentQuality}을(를) 사용한다. 입력: {@code
 *       state}(상태). 반환: {@code int}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public final class QualityCurrencyRules {
  public static final String VERSION = "user-selected-equipment-quality-maximum-v2";
  public static final int ORDINARY_MAXIMUM = 20;

  private QualityCurrencyRules() {}

  public static boolean equipment(String baseItemId) {
    return baseItemId.startsWith("Metadata/Items/Armours/")
        || baseItemId.startsWith("Metadata/Items/Weapons/");
  }

  private static boolean martial(String baseItemId) {
    return baseItemId.startsWith("Metadata/Items/Weapons/")
        && !baseItemId.contains("/Wands/")
        && !baseItemId.contains("/Staves/")
        && !baseItemId.contains("/Sceptres/");
  }

  public static boolean isQuality(WorkbenchCurrency action) {
    return action == WorkbenchCurrency.ARMOURERS_SCRAP
        || action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE;
  }

  public static String refusal(ItemState state, WorkbenchCurrency action) {
    if (!isQuality(action)) return "A supported equipment quality currency is required.";
    if (!state.conditions().isEmpty()) return "This item cannot be modified.";
    if (state.rarity() == ItemState.Rarity.UNIQUE)
      return "Unique equipment remains outside the supported quality scope.";
    boolean armour = state.baseItemId().startsWith("Metadata/Items/Armours/");
    boolean martial = martial(state.baseItemId());
    if ((action == WorkbenchCurrency.ARMOURERS_SCRAP && !armour)
        || (action == WorkbenchCurrency.BLACKSMITHS_WHETSTONE && !martial))
      return "This quality currency does not support this item class.";
    if (state.equipmentQuality() == null) return "The item's current equipment quality is unknown.";
    if (state.equipmentQuality() >= maximum(state))
      return "This item has reached the ordinary quality currency limit.";
    return "";
  }

  public static int maximum(ItemState state) {
    return Math.max(ORDINARY_MAXIMUM, AugmentCraftingRules.maximumQuality(state));
  }

  /** One simulator use reaches the equipped quality cap. Existing over-cap quality is preserved. */
  public static int nextQuality(ItemState state) {
    if (state.equipmentQuality() == null || state.equipmentQuality() >= maximum(state))
      throw new IllegalArgumentException(
          "Known equipment quality below the active limit is required");
    return maximum(state);
  }
}
