package com.poe2craft.craftingsupport.search.domain;

import java.math.BigInteger;
import java.util.*;

/**
 * 미탐색·미지원·산술 구간 때문에 생기는 추천 순위 불확실성을 구분한다.
 *
 * <p>기존 계약: Ranking uncertainty distinguishes absent methods, unsupported model mass and arithmetic
 * width.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedRecommendations()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code local(choices, allMethodsEnumerated)}: 로컬 처리에서 {@code choices.isEmpty}, {@code
 *       choices.getFirst}을(를) 사용한다. 입력: {@code choices}(선택목록), {@code
 *       allMethodsEnumerated}(all·제작방법목록·Enumerated). 반환: {@code Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Choice}: {@code CertifiedRecommendations}에서 사용하는 선택의 불변 값이다.
 *   <li>{@code Result}: {@code CertifiedRecommendations}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class CertifiedRecommendations {
  private CertifiedRecommendations() {}

  /**
   * {@code CertifiedRecommendations}에서 사용하는 선택의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code goal}: 목표 ({@code CertifiedInterval}).
   *   <li>{@code unresolved}: 미해결 ({@code CertifiedInterval}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Choice(id, goal, unresolved)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
   *       goal}(목표), {@code unresolved}(미해결). 사용하는 예외: {@code IllegalArgumentException}.
   *   <li>{@code potentialUpper()}: 잠재값·상한 값을 조회·구성한다. 반환: {@code BigInteger}.
   * </ul>
   */
  public record Choice(String id, CertifiedInterval goal, CertifiedInterval unresolved) {
    public Choice {
      if (goal.fractionalBits() != unresolved.fractionalBits())
        throw new IllegalArgumentException("Precision mismatch");
    }

    BigInteger potentialUpper() {
      return goal.upperUnits()
          .add(unresolved.upperUnits())
          .min(BigInteger.ONE.shiftLeft(goal.fractionalBits()));
    }
  }

  /**
   * {@code CertifiedRecommendations}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code methodIds}: 제작방법·식별자목록 ({@code List<String>}).
   * </ul>
   */
  public record Result(String status, List<String> methodIds) {}

  public static Result local(List<Choice> choices, boolean allMethodsEnumerated) {
    if (!allMethodsEnumerated) return new Result("PENDING_ENUMERATION", List.of());
    if (choices.isEmpty()) return new Result("NO_CHOICES", List.of());
    int bits = choices.getFirst().goal().fractionalBits();
    if (choices.stream().anyMatch(c -> c.goal().fractionalBits() != bits))
      throw new IllegalArgumentException("Precision mismatch");
    for (var candidate : choices)
      if (candidate.unresolved().zero()
          && choices.stream()
              .allMatch(
                  other ->
                      other == candidate
                          || candidate.goal().lowerUnits().compareTo(other.potentialUpper()) > 0))
        return new Result("CERTIFIED", List.of(candidate.id()));
    var highest =
        choices.stream().map(c -> c.goal().lowerUnits()).max(BigInteger::compareTo).orElseThrow();
    var tied =
        choices.stream()
            .filter(
                c ->
                    c.unresolved().zero()
                        && c.goal().exact()
                        && c.goal().lowerUnits().equals(highest))
            .toList();
    if (!tied.isEmpty()
        && choices.stream()
            .allMatch(c -> tied.contains(c) || c.potentialUpper().compareTo(highest) < 0))
      return new Result("CERTIFIED", tied.stream().map(Choice::id).toList());
    return new Result(
        choices.stream().anyMatch(c -> !c.unresolved().zero())
            ? "PENDING_MODEL"
            : "PENDING_ARITHMETIC",
        List.of());
  }
}
