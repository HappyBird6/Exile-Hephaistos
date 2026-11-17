package com.poe2craft.shared.crafting.domain;

/**
 * 벨트 에센스의 명시 옵션 후보를 제공한다. 플라스크·참의 미확정 속성은 계산하지 않는다.
 *
 * <p>기존 계약: Explicit-affix projection only. Variable flask recovery/Charm slots are unknown.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Belts/FourBelt1"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BeltEssenceTargets()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 */
public final class BeltEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Belts/FourBelt1";

  private BeltEssenceTargets() {}
}
