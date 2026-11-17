package com.poe2craft.shared.crafting.domain;

/**
 * 반지의 에센스 명시 옵션과 고정 암시 수치 후보를 제공한다.
 *
 * <p>기존 계약: Source-matched Iron Ring explicit affixes and fixed implicit endpoints.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Rings/FourRing1"} ({@code String}).
 *   <li>{@code IMPLICIT_ID}: 식별자; 초기값 {@code "iron-ring:implicit:added-physical-damage-to-attacks"}
 *       ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RingEssenceTargets()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 */
public final class RingEssenceTargets {
  public static final String BASE_ID = "Metadata/Items/Rings/FourRing1";
  public static final String IMPLICIT_ID = "iron-ring:implicit:added-physical-damage-to-attacks";

  private RingEssenceTargets() {}
}
