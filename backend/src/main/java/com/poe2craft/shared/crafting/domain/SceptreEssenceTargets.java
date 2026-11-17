package com.poe2craft.shared.crafting.domain;

import java.util.List;
import java.util.Map;

/**
 * Rattling Sceptre의 기존 호출자가 JSON 에센스 목록을 참조하도록 유지하는 호환 진입점이다.
 *
 * <p>Rattling Sceptre의 기존 호출자가 JSON 에센스 목록을 참조하도록 유지하는 호환 진입점이다.
 *
 * <p>BASE_ID는 기존 베이스의 원본 식별자다. VERIFIED는 JSON의 추가형 에센스 대상 목록이다. private 생성자는 인스턴스 생성을 막는다. 정적 필드는
 * WorkbenchDefinitions 초기화 후 한 번 읽는 불변 map이다. 대상 변경은 crafting/essences/targets.json에서 하며 Java 후보
 * 목록을 추가하지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASE_ID}: 식별자; 초기값 {@code
 *       "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1"} ({@code String}).
 *   <li>{@code VERIFIED}: 검증된 ({@code Map<WorkbenchCurrency, List<String>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SceptreEssenceTargets()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 */
public final class SceptreEssenceTargets {
  private SceptreEssenceTargets() {}

  public static final String BASE_ID =
      "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1";
  public static final Map<WorkbenchCurrency, List<String>> VERIFIED =
      WorkbenchDefinitions.active()
          .targets("Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1")
          .fixed();
}
