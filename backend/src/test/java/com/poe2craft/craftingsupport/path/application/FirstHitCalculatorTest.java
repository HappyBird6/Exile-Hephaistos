package com.poe2craft.craftingsupport.path.application;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 목표 도달 상태를 흡수하면서 최초 성공까지의 상태 확률을 전파한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code calculator}: calculator ({@code FirstHitCalculator<String>}).
 *   <li>{@code budget}: 계산예산 ({@code FirstHitCalculator.Budget}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code endedPolicyWinsOverExactBudgetExhaustionAndCancellation()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code frontierAndFractionLimitsRemainUnknownAndLargerObservationsAreAllowed()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code independentBinaryPathEnumerationMatchesCyclesAtEveryDepth()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code kernel(a, b)}: 전이커널 처리에서 {@code Fraction.of}을(를) 사용한다. 입력: {@code a}(a), {@code
 *       b}(b). 반환: {@code FirstHitCalculator.Kernel<String>}.
 *   <li>{@code goal(state)}: 목표 처리에서 {@code state.equals}을(를) 사용한다. 입력: {@code state}(상태). 반환:
 *       {@code FirstHitCalculator.Match}.
 *   <li>{@code geometricOracleAt500AndMassConservation()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code changingStateCycleIsNotIndependentTrials()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code alreadySuccessfulUnreachableAndUnavailable()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *       사용하는 예외: {@code AssertionError}.
 *   <li>{@code partialMassNeverBecomesFailure()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code cancellationAndEvaluationBudgetRetainBounds()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code rejectsNonNormalizedKernelAndDuplicateObservations()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
class FirstHitCalculatorTest {
  @Test
  void endedPolicyWinsOverExactBudgetExhaustionAndCancellation() {
    var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
    var result =
        calculator.calculate(
            "root",
            List.of(1L, 2L, 500L),
            this::goal,
            (state, step) -> {
              cancelled.set(true);
              return kernel("hit", "root");
            },
            new FirstHitCalculator.Budget(1, 1, 64),
            cancelled::get,
            step -> step >= 1);
    assertThat(result.evaluations()).isEqualTo(1);
    for (var point : result.points()) {
      assertThat(point.lower()).isEqualTo(Fraction.of(1, 2));
      assertThat(point.dead()).isEqualTo(Fraction.of(1, 2));
      assertThat(point.unresolved()).isEqualTo(Fraction.ZERO);
      assertThat(point.status()).isEqualTo("COMPLETE");
    }
  }

  @Test
  void frontierAndFractionLimitsRemainUnknownAndLargerObservationsAreAllowed() {
    var frontier =
        calculator.calculate(
            "root",
            List.of(1000L),
            this::goal,
            (state, step) -> kernel("left", "right"),
            new FirstHitCalculator.Budget(100, 1, 65536),
            () -> false);
    assertThat(frontier.points().getFirst().lower()).isEqualTo(Fraction.ZERO);
    assertThat(frontier.points().getFirst().upper()).isEqualTo(Fraction.ONE);
    var fractions =
        calculator.calculate(
            "root",
            List.of(1000L),
            this::goal,
            (state, step) -> kernel("hit", "root"),
            new FirstHitCalculator.Budget(10000, 100, 64),
            () -> false);
    assertThat(fractions.reason()).isEqualTo("FRACTION_COMPLEXITY_LIMIT");
    assertThat(fractions.points().getFirst().status()).isEqualTo("PARTIAL");
    var loop =
        calculator.calculate(
            "root",
            List.of(1000L),
            this::goal,
            (state, step) -> kernel("root", "root"),
            budget,
            () -> false);
    assertThat(loop.points().getFirst().lower()).isEqualTo(Fraction.ZERO);
    assertThat(loop.points().getFirst().unresolved()).isEqualTo(Fraction.ZERO);
  }

  @Test
  void independentBinaryPathEnumerationMatchesCyclesAtEveryDepth() {
    var observations = java.util.stream.LongStream.rangeClosed(0, 10).boxed().toList();
    var result =
        calculator.calculate(
            "root",
            observations,
            this::goal,
            (state, step) -> state.equals("root") ? kernel("next", "root") : kernel("hit", "root"),
            budget,
            () -> false);
    for (int n = 0; n <= 10; n++) {
      int hitPaths = 0;
      for (int sequence = 0; sequence < (1 << n); sequence++) {
        int state = 0;
        for (int step = 0; step < n && state != 2; step++) {
          boolean first = (sequence & (1 << step)) == 0;
          state = first ? state + 1 : 0;
        }
        if (state == 2) hitPaths++;
      }
      assertThat(result.points().get(n).lower()).isEqualTo(Fraction.of(hitPaths, 1 << n));
    }
  }

  final FirstHitCalculator<String> calculator = new FirstHitCalculator<>();
  final FirstHitCalculator.Budget budget = new FirstHitCalculator.Budget(10000, 100, 65536);

  FirstHitCalculator.Kernel<String> kernel(String a, String b) {
    return new FirstHitCalculator.Kernel<>(
        List.of(
            new FirstHitCalculator.Edge<>(a, Fraction.of(1, 2)),
            new FirstHitCalculator.Edge<>(b, Fraction.of(1, 2))),
        Fraction.ZERO,
        false);
  }

  FirstHitCalculator.Match goal(String state) {
    return state.equals("hit") ? FirstHitCalculator.Match.HIT : FirstHitCalculator.Match.MISS;
  }

  @Test
  void geometricOracleAt500AndMassConservation() {
    var result =
        calculator.calculate(
            "root",
            List.of(0L, 100L, 300L, 500L),
            this::goal,
            (state, step) -> kernel("hit", "root"),
            budget,
            () -> false);
    for (var point : result.points()) {
      var denominator = java.math.BigInteger.ONE.shiftLeft((int) point.attempts());
      var expected = new Fraction(denominator.subtract(java.math.BigInteger.ONE), denominator);
      assertThat(point.lower()).isEqualTo(expected);
      assertThat(point.upper()).isEqualTo(expected);
      assertThat(point.lower().add(point.active()).add(point.dead()).add(point.unresolved()))
          .isEqualTo(Fraction.ONE);
    }
  }

  @Test
  void changingStateCycleIsNotIndependentTrials() {
    var result =
        calculator.calculate(
            "root",
            List.of(1L, 2L, 3L),
            this::goal,
            (state, step) -> state.equals("root") ? kernel("next", "root") : kernel("hit", "root"),
            budget,
            () -> false);
    assertThat(result.points().stream().map(FirstHitCalculator.Point::lower))
        .containsExactly(Fraction.ZERO, Fraction.of(1, 4), Fraction.of(3, 8));
  }

  @Test
  void alreadySuccessfulUnreachableAndUnavailable() {
    var hit =
        calculator.calculate(
            "hit",
            List.of(0L, Long.MAX_VALUE),
            this::goal,
            (state, step) -> {
              throw new AssertionError();
            },
            budget,
            () -> false);
    assertThat(hit.points().getLast().lower()).isEqualTo(Fraction.ONE);
    var unavailable =
        calculator.calculate(
            "root",
            List.of(100L),
            this::goal,
            (state, step) -> new FirstHitCalculator.Kernel<>(List.of(), Fraction.ZERO, true),
            budget,
            () -> false);
    assertThat(unavailable.points().getFirst().dead()).isEqualTo(Fraction.ONE);
  }

  @Test
  void partialMassNeverBecomesFailure() {
    var result =
        calculator.calculate(
            "root",
            List.of(1L, 500L),
            this::goal,
            (state, step) ->
                new FirstHitCalculator.Kernel<>(
                    List.of(new FirstHitCalculator.Edge<>("hit", Fraction.of(1, 4))),
                    Fraction.of(3, 4),
                    false),
            budget,
            () -> false);
    assertThat(result.points().getLast().lower()).isEqualTo(Fraction.of(1, 4));
    assertThat(result.points().getLast().upper()).isEqualTo(Fraction.ONE);
    assertThat(result.points().getLast().status()).isEqualTo("PARTIAL");
  }

  @Test
  void cancellationAndEvaluationBudgetRetainBounds() {
    var cancelled =
        calculator.calculate(
            "root",
            List.of(500L),
            this::goal,
            (state, step) -> kernel("hit", "root"),
            budget,
            () -> true);
    assertThat(cancelled.points().getFirst().unresolved()).isEqualTo(Fraction.ONE);
    var limited =
        calculator.calculate(
            "root",
            List.of(1L, 500L),
            this::goal,
            (state, step) -> kernel("hit", "root"),
            new FirstHitCalculator.Budget(1, 100, 65536),
            () -> false);
    assertThat(limited.points().getLast().lower()).isEqualTo(Fraction.of(1, 2));
    assertThat(limited.points().getLast().unresolved()).isEqualTo(Fraction.of(1, 2));
  }

  @Test
  void rejectsNonNormalizedKernelAndDuplicateObservations() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FirstHitCalculator.Kernel<>(List.of(), Fraction.ZERO, false));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                calculator.calculate(
                    "root",
                    List.of(1L, 1L),
                    this::goal,
                    (state, step) -> kernel("hit", "root"),
                    budget,
                    () -> false));
  }
}
