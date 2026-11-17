package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;

/**
 * 제거 분기와 출처가 있는 고정 결과를 보존하는 자동 제작 계획이다.
 *
 * <p>기존 계약: Shared eligible branches, retaining every removal and sourced fixed outcome.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
 *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
 *   <li>{@code branches}: 분기목록 ({@code List<Branch>}).
 *   <li>{@code uniformCandidates}: 균등·후보목록 ({@code boolean}).
 *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticCraftingPlan(status, reason, branches, uniformCandidates, sourceUrl)}: 전달받은
 *       값으로 구성원을 초기화한다. 입력: {@code status}(상태), {@code reason}(이유), {@code branches}(분기목록), {@code
 *       uniformCandidates}(균등·후보목록), {@code sourceUrl}(출처·출처주소).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Status}: {@code AutomaticCraftingPlan}의 상태 구분값을 정의한다.
 *   <li>{@code Branch}: {@code AutomaticCraftingPlan}에서 사용하는 분기의 불변 값이다.
 * </ul>
 */
public record AutomaticCraftingPlan(
    Status status,
    String reason,
    List<Branch> branches,
    boolean uniformCandidates,
    String sourceUrl) {
  /**
   * {@code AutomaticCraftingPlan}의 상태 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code READY}: 준비완료 구분값.
   *   <li>{@code UNAVAILABLE}: 사용불가 구분값.
   *   <li>{@code UNSUPPORTED}: 미지원 구분값.
   * </ul>
   */
  public enum Status {
    READY,
    UNAVAILABLE,
    UNSUPPORTED
  }

  /**
   * {@code AutomaticCraftingPlan}에서 사용하는 분기의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code remaining}: 남은 ({@code ItemState}).
   *   <li>{@code candidates}: 후보목록 ({@code List<ModifierDefinition>}).
   *   <li>{@code probabilityDenominator}: 확률·분모 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Branch(remaining, candidates)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       remaining}(남은), {@code candidates}(후보목록).
   *   <li>{@code Branch(remaining, candidates, probabilityDenominator)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code remaining}(남은), {@code candidates}(후보목록), {@code probabilityDenominator}(확률·분모).
   * </ul>
   */
  public record Branch(
      ItemState remaining, List<ModifierDefinition> candidates, long probabilityDenominator) {
    public Branch(ItemState remaining, List<ModifierDefinition> candidates) {
      this(remaining, candidates, 0);
    }

    public Branch {
      candidates = List.copyOf(candidates);
    }
  }

  public AutomaticCraftingPlan {
    branches = List.copyOf(branches);
  }
}
