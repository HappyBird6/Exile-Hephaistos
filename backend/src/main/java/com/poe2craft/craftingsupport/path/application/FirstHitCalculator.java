package com.poe2craft.craftingsupport.path.application;

import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.*;

/**
 * 목표 도달 상태를 흡수하면서 최초 성공까지의 상태 확률을 전파한다.
 *
 * <p>기존 계약: State propagation with absorbing hits. Budgets describe computation, never crafting
 * limits.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code calculate(root, observations, goal, transition, budget, cancelled)}: 해당 모형의 계산 결과를
 *       구한다. 입력: {@code root}(시작점), {@code observations}(관측값목록), {@code goal}(목표), {@code
 *       transition}(전이), {@code budget}(계산예산), {@code cancelled}(취소여부). 반환: {@code Result}.
 *   <li>{@code calculate(root, observations, goal, transition, budget, cancelled, policyEnded)}: 해당
 *       모형의 계산 결과를 구한다. 입력: {@code root}(시작점), {@code observations}(관측값목록), {@code goal}(목표),
 *       {@code transition}(전이), {@code budget}(계산예산), {@code cancelled}(취소여부), {@code
 *       policyEnded}(정책·Ended). 반환: {@code Result}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code bits(mass)}: 비트수 처리에서 {@code mass.numerator}, {@code mass.denominator}을(를) 사용한다. 입력:
 *       {@code mass}(확률질량). 반환: {@code int}.
 *   <li>{@code observationPoints(observations)}: 관측값·지점목록 처리에서 {@code observations.isEmpty}, {@code
 *       observations.size}을(를) 사용한다. 입력: {@code observations}(관측값목록). 반환: {@code List<Long>}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Match}: {@code FirstHitCalculator}의 일치 구분값을 정의한다.
 *   <li>{@code Edge}: {@code FirstHitCalculator}에서 사용하는 간선의 불변 값이다.
 *   <li>{@code Kernel}: {@code FirstHitCalculator}에서 사용하는 전이커널의 불변 값이다.
 *   <li>{@code Budget}: {@code FirstHitCalculator}에서 사용하는 계산예산의 불변 값이다.
 *   <li>{@code Point}: {@code FirstHitCalculator}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Result}: {@code FirstHitCalculator}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class FirstHitCalculator<S> {
  /**
   * {@code FirstHitCalculator}의 일치 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code HIT}: 목표도달 구분값.
   *   <li>{@code MISS}: MISS 구분값.
   *   <li>{@code UNKNOWN}: 미확정 구분값.
   * </ul>
   */
  public enum Match {
    HIT,
    MISS,
    UNKNOWN
  }

  /**
   * {@code FirstHitCalculator}에서 사용하는 간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code S}).
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
   * </ul>
   */
  public record Edge<S>(S state, Fraction mass) {}

  /**
   * {@code FirstHitCalculator}에서 사용하는 전이커널의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code edges}: 간선목록 ({@code List<Edge<S>>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Kernel(edges, unresolved, unavailable)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       edges}(간선목록), {@code unresolved}(미해결), {@code unavailable}(사용불가). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Kernel<S>(List<Edge<S>> edges, Fraction unresolved, boolean unavailable) {
    public Kernel {
      edges = List.copyOf(edges);
      var total = unresolved;
      for (var edge : edges) total = total.add(edge.mass());
      if (unavailable
          ? !edges.isEmpty() || !unresolved.equals(Fraction.ZERO)
          : !total.equals(Fraction.ONE)) throw new IllegalArgumentException("Invalid kernel mass");
    }
  }

  /**
   * {@code FirstHitCalculator}에서 사용하는 계산예산의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code evaluations}: 평가목록 ({@code long}).
   *   <li>{@code frontierStates}: 미탐색경계·상태목록 ({@code int}).
   *   <li>{@code fractionBits}: 유리수·비트수 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Budget(evaluations, frontierStates, fractionBits)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code evaluations}(평가목록), {@code frontierStates}(미탐색경계·상태목록), {@code
   *       fractionBits}(유리수·비트수). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Budget(long evaluations, int frontierStates, int fractionBits) {
    public Budget {
      if (evaluations < 1 || frontierStates < 1 || fractionBits < 64)
        throw new IllegalArgumentException("Positive computation budget required");
    }
  }

  /**
   * {@code FirstHitCalculator}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code long}).
   *   <li>{@code lower}: 하한 ({@code Fraction}).
   *   <li>{@code upper}: 상한 ({@code Fraction}).
   *   <li>{@code active}: 활성 ({@code Fraction}).
   *   <li>{@code dead}: 종료된 ({@code Fraction}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   * </ul>
   */
  public record Point(
      long attempts,
      Fraction lower,
      Fraction upper,
      Fraction active,
      Fraction dead,
      Fraction unresolved,
      String status) {}

  /**
   * {@code FirstHitCalculator}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   *   <li>{@code evaluations}: 평가목록 ({@code long}).
   *   <li>{@code peakFrontier}: peak·미탐색경계 ({@code int}).
   *   <li>{@code peakFractionBits}: peak·유리수·비트수 ({@code int}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   */
  public record Result(
      List<Point> points,
      long evaluations,
      int peakFrontier,
      int peakFractionBits,
      String reason) {}

  public Result calculate(
      S root,
      List<Long> observations,
      Function<S, Match> goal,
      BiFunction<S, Long, Kernel<S>> transition,
      Budget budget,
      BooleanSupplier cancelled) {
    return calculate(root, observations, goal, transition, budget, cancelled, step -> false);
  }

  public Result calculate(
      S root,
      List<Long> observations,
      Function<S, Match> goal,
      BiFunction<S, Long, Kernel<S>> transition,
      Budget budget,
      BooleanSupplier cancelled,
      LongPredicate policyEnded) {
    Objects.requireNonNull(root);
    Objects.requireNonNull(goal);
    Objects.requireNonNull(transition);
    Objects.requireNonNull(budget);
    Objects.requireNonNull(cancelled);
    var sorted = observationPoints(observations);
    Objects.requireNonNull(policyEnded);
    var frontier = new LinkedHashMap<S, Fraction>();
    var hit = Fraction.ZERO;
    var dead = Fraction.ZERO;
    var unknown = Fraction.ZERO;
    switch (goal.apply(root)) {
      case HIT -> hit = Fraction.ONE;
      case UNKNOWN -> unknown = Fraction.ONE;
      case MISS -> frontier.put(root, Fraction.ONE);
    }
    long depth = 0, evaluations = 0;
    int peakFrontier = frontier.size(), peakBits = 1;
    String reason = "";
    var points = new ArrayList<Point>();
    for (long observation : sorted) {
      while (depth < observation && !frontier.isEmpty()) {
        if (policyEnded.test(depth)) {
          for (var mass : frontier.values()) dead = dead.add(mass);
          frontier.clear();
          break;
        }
        if (cancelled.getAsBoolean() || evaluations >= budget.evaluations()) {
          reason = cancelled.getAsBoolean() ? "CANCELLED" : "EVALUATION_LIMIT";
          for (var mass : frontier.values()) unknown = unknown.add(mass);
          frontier.clear();
          break;
        }
        var next = new LinkedHashMap<S, Fraction>();
        long frontierBits = 0;
        for (var mass : frontier.values()) frontierBits += bits(mass);
        for (var entry : frontier.entrySet()) {
          if (evaluations >= budget.evaluations() || cancelled.getAsBoolean()) {
            unknown = unknown.add(entry.getValue());
            reason = "EVALUATION_LIMIT_OR_CANCELLED";
            continue;
          }
          evaluations++;
          var kernel = transition.apply(entry.getKey(), depth);
          // Bound all arithmetic in this expansion before allocating multiplied fractions.
          long bits =
              frontierBits + bits(entry.getValue()) + bits(hit) + bits(dead) + bits(unknown);
          for (var value : next.values()) bits += bits(value);
          bits += bits(kernel.unresolved());
          for (var edge : kernel.edges()) bits += bits(edge.mass());
          if (bits * 2 > budget.fractionBits()) {
            unknown = unknown.add(entry.getValue());
            reason = "FRACTION_COMPLEXITY_LIMIT";
            continue;
          }
          if (kernel.unavailable()) {
            dead = dead.add(entry.getValue());
            continue;
          }
          unknown = unknown.add(entry.getValue().multiply(kernel.unresolved()));
          if (!kernel.unresolved().equals(Fraction.ZERO) && reason.isEmpty())
            reason = "TRANSITION_UNRESOLVED";
          for (var edge : kernel.edges()) {
            var mass = entry.getValue().multiply(edge.mass());
            if (mass.equals(Fraction.ZERO)) continue;
            switch (goal.apply(edge.state())) {
              case HIT -> hit = hit.add(mass);
              case UNKNOWN -> unknown = unknown.add(mass);
              case MISS -> {
                if (!next.containsKey(edge.state()) && next.size() >= budget.frontierStates()) {
                  unknown = unknown.add(mass);
                  reason = "FRONTIER_LIMIT";
                } else next.merge(edge.state(), mass, Fraction::add);
              }
            }
          }
          peakBits = Math.max(peakBits, Math.max(bits(hit), Math.max(bits(unknown), bits(dead))));
          for (var value : next.values()) peakBits = Math.max(peakBits, bits(value));
        }
        frontier = next;
        peakFrontier = Math.max(peakFrontier, frontier.size());
        depth++;
      }
      // Policy termination requires no transition evaluation, even at an exhausted budget boundary.
      if (policyEnded.test(depth)) {
        for (var mass : frontier.values()) dead = dead.add(mass);
        frontier.clear();
      }
      var active = Fraction.ZERO;
      for (var mass : frontier.values()) active = active.add(mass);
      peakBits =
          Math.max(
              peakBits,
              Math.max(
                  bits(active),
                  Math.max(bits(hit.add(unknown)), Math.max(bits(dead), bits(unknown)))));
      if (!hit.add(dead).add(unknown).add(active).equals(Fraction.ONE))
        throw new IllegalStateException("First-hit mass was not conserved");
      points.add(
          new Point(
              observation,
              hit,
              hit.add(unknown),
              active,
              dead,
              unknown,
              unknown.equals(Fraction.ZERO)
                  ? "COMPLETE"
                  : hit.equals(Fraction.ZERO) ? "UNKNOWN" : "PARTIAL"));
    }
    return new Result(List.copyOf(points), evaluations, peakFrontier, peakBits, reason);
  }

  private static int bits(Fraction mass) {
    return Math.max(mass.numerator().bitLength(), mass.denominator().bitLength());
  }

  static List<Long> observationPoints(List<Long> observations) {
    if (observations == null || observations.isEmpty() || observations.size() > 32)
      throw new IllegalArgumentException("One to 32 observation points required");
    if (observations.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Non-null observation points required");
    var sorted = observations.stream().distinct().sorted().toList();
    if (sorted.size() != observations.size() || sorted.getFirst() < 0)
      throw new IllegalArgumentException("Distinct nonnegative observation points required");
    return sorted;
  }
}
