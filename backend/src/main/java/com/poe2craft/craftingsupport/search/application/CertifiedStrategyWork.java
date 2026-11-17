package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.domain.CertifiedOutcomeStrategy;
import java.util.*;
import java.util.function.IntFunction;

/**
 * 유한 전략의 계산 구간을 나눠 이어서 실행한다.
 *
 * <p>기존 계약: Resumable horizon sequencing for the explicitly bounded finite strategy adapter.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code states}: 상태목록 ({@code List<String>}).
 *   <li>{@code factory}: 생성기 ({@code IntFunction<CertifiedOutcomeStrategy<String>>}).
 *   <li>{@code decimals}: 소수자릿수 ({@code int}).
 *   <li>{@code completed}: 완료 ({@code List<Row>}).
 *   <li>{@code current}: 현재 ({@code List<Row>}).
 *   <li>{@code solver}: solver ({@code CertifiedOutcomeStrategy<String>}).
 *   <li>{@code horizon}: 계산기간 ({@code int}).
 *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
 *   <li>{@code pauseReason}: pause·이유 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CertifiedStrategyWork(states, decimals, factory)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       states}(상태목록), {@code decimals}(소수자릿수), {@code factory}(생성기).
 *   <li>{@code revision()}: {@code revision} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code pauseReason()}: {@code pauseReason} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code status()}: 현재 진행 상태를 조회한다. 반환: {@code String}.
 *   <li>{@code rows()}: 행목록 값을 조회·구성한다. 반환: {@code List<Row>}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Row}: {@code CertifiedStrategyWork}에서 사용하는 행의 불변 값이다.
 * </ul>
 */
final class CertifiedStrategyWork {
  /**
   * {@code CertifiedStrategyWork}에서 사용하는 행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stateId}: 상태·식별자 ({@code String}).
   *   <li>{@code cycles}: 주기횟수 ({@code int}).
   *   <li>{@code result}: 결과 ({@code CertifiedOutcomeStrategy.Result}).
   * </ul>
   */
  record Row(String stateId, int cycles, CertifiedOutcomeStrategy.Result result) {}

  private final List<String> states;
  private final IntFunction<CertifiedOutcomeStrategy<String>> factory;
  private final int decimals;
  private final List<Row> completed = new ArrayList<>();
  private List<Row> current = List.of();
  private CertifiedOutcomeStrategy<String> solver;
  private int horizon;
  private long revision;
  private String pauseReason;

  CertifiedStrategyWork(
      List<String> states, int decimals, IntFunction<CertifiedOutcomeStrategy<String>> factory) {
    this.states = List.copyOf(states);
    this.decimals = decimals;
    this.factory = factory;
  }

  long revision() {
    return revision;
  }

  boolean complete() {
    return horizon == MethodSearchCalculation.OBSERVATIONS.size();
  }

  String pauseReason() {
    return pauseReason;
  }

  String status() {
    return complete() ? "COMPLETE" : pauseReason != null ? "PAUSED" : "RUNNING";
  }

  List<Row> rows() {
    var result = new ArrayList<>(completed);
    result.addAll(current);
    return List.copyOf(result);
  }

  void step() {
    if (complete() || pauseReason != null) return;
    int cycles = MethodSearchCalculation.OBSERVATIONS.get(horizon);
    if (solver == null) {
      solver = factory.apply(cycles);
      revision++;
      return;
    }
    solver.refine();
    var next = states.stream().map(id -> new Row(id, cycles, solver.result(id))).toList();
    boolean certified =
        next.stream()
            .allMatch(
                row -> {
                  var result = row.result();
                  return result.value().confirmedPercent(decimals).isPresent()
                      && (!result.methodIds().isEmpty()
                          || Set.of("GOAL", "PROVEN_ZERO").contains(result.status()));
                });
    if (certified) {
      completed.addAll(next);
      current = List.of();
      solver = null;
      horizon++;
    } else {
      if (next.equals(current))
        pauseReason =
            next.stream().anyMatch(r -> r.result().reachableUnresolved())
                ? "STRATEGY_UNRESOLVED"
                : "STRATEGY_PRECISION";
      current = next;
    }
    revision++;
  }
}
