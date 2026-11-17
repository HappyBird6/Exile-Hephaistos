package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;

/**
 * 품질 상한 감소 시 기존 품질을 보존하는 앱 정책을 적용한다.
 *
 * <p>기존 계약: User-confirmed cap loss preserves quality; catalyst reuse remains a simulator policy.
 *
 * <p>필드·상수·열거값:
 *
 * <ul>
 *   <li>{@code PRESERVE_EXISTING}: 기존 구분값.
 *   <li>{@code REJECT_OVERCAP}: OVERCAP 구분값.
 *   <li>{@code VERSION}: 버전; 초기값 {@code "user-confirmed-quality-cap-preserve-v2"} ({@code String}).
 *   <li>{@code DEFAULT}: 기본값 구분값.
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code afterAcceptedOperation(state, catalog)}: {@code state} 값을 조회한다. 입력: {@code
 *       state}(상태), {@code catalog}(카탈로그). 반환: {@code ItemState}.
 *   <li>{@code catalystAmount(state, cap)}: 기폭제·양 처리에서 {@code state.catalystQuality}을(를) 사용한다. 입력:
 *       {@code state}(상태), {@code cap}(상한). 반환: {@code int}.
 * </ul>
 */
public enum QualityCapChangePolicy {
  PRESERVE_EXISTING,
  REJECT_OVERCAP;
  public static final String VERSION = "user-confirmed-quality-cap-preserve-v2";
  public static final QualityCapChangePolicy DEFAULT = PRESERVE_EXISTING;

  public ItemState afterAcceptedOperation(ItemState state, ItemCatalog catalog) {
    return state;
  }

  public int catalystAmount(ItemState state, int cap) {
    return Math.max(cap, state.catalystQuality() == null ? 0 : state.catalystQuality().amount());
  }
}
