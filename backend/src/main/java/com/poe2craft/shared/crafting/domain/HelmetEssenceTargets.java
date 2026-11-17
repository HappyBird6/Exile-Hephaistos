package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ReviewedHelmets;

/**
 * 투구의 출처와 일치하는 에센스 명시 옵션 후보를 제공한다.
 *
 * <p>기존 계약: Source-matched Rusted Greathelm explicit affixes; base Armour is display-only.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Armours/Helmets/FourHelmetStr1"} ({@code
 *       String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code HelmetEssenceTargets()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supports(id)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 * </ul>
 */
public final class HelmetEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Armours/Helmets/FourHelmetStr1";

  private HelmetEssenceTargets() {}

  public static boolean supports(String id) {
    return ReviewedHelmets.supports(id)
        || id.equals(BASE_ID)
        || id.equals("Metadata/Items/Armours/Helmets/FourHelmetStr7Endgame");
  }
}
