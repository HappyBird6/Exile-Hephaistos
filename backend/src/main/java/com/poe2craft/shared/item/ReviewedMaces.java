package com.poe2craft.shared.item;

import java.util.Map;

/**
 * Maces의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>기존 계약: Ordinary representative endgame Maces; separate class pools retain canonical weapon
 * stats.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASES}: 베이스목록 ({@code Map<String, String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ReviewedMaces()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supports(id)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 * </ul>
 */
public final class ReviewedMaces {
  public static final Map<String, String> BASES =
      BaseRegistry.familyBases("one-hand-maces", "two-hand-maces");

  private ReviewedMaces() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }
}
