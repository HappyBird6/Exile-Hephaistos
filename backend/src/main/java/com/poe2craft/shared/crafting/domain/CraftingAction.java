package com.poe2craft.shared.crafting.domain;

/**
 * 기존 제작 엔진이 지원하는 기본 화폐 행동을 구분한다.
 *
 * <p>필드·상수·열거값:
 *
 * <ul>
 *   <li>{@code TRANSMUTATION}: 진화 구분값.
 *   <li>{@code AUGMENTATION}: 확장 구분값.
 *   <li>{@code REGAL}: 제왕 구분값.
 *   <li>{@code EXALTED}: 엑잘티드 구분값.
 *   <li>{@code ANNULMENT}: 소멸 구분값.
 *   <li>{@code CHAOS}: 카오스 구분값.
 * </ul>
 */
public enum CraftingAction {
  TRANSMUTATION,
  AUGMENTATION,
  REGAL,
  EXALTED,
  ANNULMENT,
  CHAOS
}
