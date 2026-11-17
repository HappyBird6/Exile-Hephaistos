package com.poe2craft.shared.item;

import java.util.Map;

/**
 * Rings의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>기존 계약: Distinct source-reviewed Ring implicits; this is not the complete Ring catalog.
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
 *   <li>{@code ReviewedRings()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supports(id)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code reviewedImplicit(d)}: 검토된·암시옵션 처리에서 {@code d.id}, {@code d.layer}을(를) 사용한다. 입력:
 *       {@code d}(d). 반환: {@code boolean}.
 * </ul>
 */
public final class ReviewedRings {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("rings");

  private ReviewedRings() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }

  public static boolean reviewedImplicit(ModifierDefinition d) {
    return java.util.Set.of(
                "kinetic:implicit:physicaldamage",
                "vitalic:implicit:increasedlife",
                "mnemonic:implicit:maximummanaincreasepercent",
                "pearl:implicit:increasedcastspeed",
                "amethyst:implicit:chaosresistance",
                "prismatic:implicit:allresistances",
                "ruby-ring:implicit:fireresistance",
                "two-stone-fire-cold:implicit:fireandcoldresistance")
            .contains(d.id())
        && d.layer() == ModifierDefinition.Layer.IMPLICIT;
  }
}
