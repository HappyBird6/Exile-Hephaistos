package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import java.util.List;

/**
 * 상태 동치류의 주변 확률과 구체적인 조건부 분포를 구분하는 API 값이다.
 *
 * <p>기존 계약: Additive indexed API: class marginals are distinct from conditional concrete entry
 * laws.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code IndexedMethodProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Started}: {@code IndexedMethodProtocol}에서 사용하는 시작시각의 불변 값이다.
 *   <li>{@code Outcome}: {@code IndexedMethodProtocol}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Outcomes}: {@code IndexedMethodProtocol}에서 사용하는 결과목록의 불변 값이다.
 *   <li>{@code Bind}: {@code IndexedMethodProtocol}에서 사용하는 연결의 불변 값이다.
 *   <li>{@code Advance}: {@code IndexedMethodProtocol}에서 사용하는 진행의 불변 값이다.
 *   <li>{@code Entry}: {@code IndexedMethodProtocol}에서 사용하는 항목의 불변 값이다.
 * </ul>
 */
public final class IndexedMethodProtocol {
  private IndexedMethodProtocol() {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 시작시각의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code search}: 탐색 ({@code MethodSearchProtocol.Snapshot}).
   *   <li>{@code rootEntry}: 시작점·항목 ({@code Entry}).
   * </ul>
   */
  public record Started(MethodSearchProtocol.Snapshot search, Entry rootEntry) {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   * </ul>
   */
  public record Outcome(String kind, String stateId, Probability probability) {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 결과목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code jobId}: 작업·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code methodId}: 제작방법·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code nextCursor}: 다음·진행커서 ({@code String}).
   * </ul>
   */
  public record Outcomes(
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<Outcome> outcomes,
      String nextCursor) {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 연결의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code requestId}: 요청·식별자 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code Long}).
   *   <li>{@code parentEntryId}: 상위값·항목·식별자 ({@code String}).
   *   <li>{@code methodId}: 제작방법·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code Integer}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code targetStateId}: 대상·상태·식별자 ({@code String}).
   * </ul>
   */
  public record Bind(
      Integer version,
      String requestId,
      Long revision,
      String parentEntryId,
      String methodId,
      Integer cycles,
      String kind,
      String targetStateId) {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 진행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code Integer}).
   *   <li>{@code expectedGeneration}: 기대값·세대번호 ({@code Long}).
   *   <li>{@code maxSteps}: 최대값·단계목록 ({@code Integer}).
   *   <li>{@code retainedStateLimit}: 보유·상태·한도 ({@code Integer}).
   * </ul>
   */
  public record Advance(
      Integer version, Long expectedGeneration, Integer maxSteps, Integer retainedStateLimit) {}

  /**
   * {@code IndexedMethodProtocol}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code parentEntryId}: 상위값·항목·식별자 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code conditionalProbability}: 조건부·확률 ({@code Probability}).
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code List<MethodSearchProtocol.ConditionalState>}).
   *   <li>{@code nextOffset}: 다음·시작위치 ({@code Integer}).
   * </ul>
   */
  public record Entry(
      String id,
      String stateId,
      String parentEntryId,
      String status,
      String reason,
      Probability conditionalProbability,
      long generation,
      List<MethodSearchProtocol.ConditionalState> states,
      Integer nextOffset) {}
}
