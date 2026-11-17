package com.poe2craft.shared.item;

import java.util.Map;

/**
 * Amulets의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>기존 계약: Distinct source-reviewed Amulet implicits; not the complete Amulet catalog.
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
 *   <li>{@code ReviewedAmulets()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supports(id)}: 호출 대상의 지원 여부를 판정한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code reviewedImplicit(d)}: 검토된·암시옵션 처리에서 {@code d.id}, {@code d.layer}을(를) 사용한다. 입력:
 *       {@code d}(d). 반환: {@code boolean}.
 * </ul>
 */
public final class ReviewedAmulets {
  public static final Map<String, String> BASES = BaseRegistry.familyBases("amulets");

  private ReviewedAmulets() {}

  public static boolean supports(String id) {
    return BASES.containsValue(id);
  }

  public static boolean reviewedImplicit(ModifierDefinition d) {
    return java.util.Set.of(
                "stellar:implicit:allattributes",
                "amber:implicit:strength",
                "bloodstone:implicit:increasedlife",
                "lunar:implicit:increasedenergyshield",
                "azure:implicit:manaregeneration",
                "crimson:implicit:liferegeneration",
                "pearlescent:implicit:allresistances")
            .contains(d.id())
        && d.layer() == ModifierDefinition.Layer.IMPLICIT;
  }
}
