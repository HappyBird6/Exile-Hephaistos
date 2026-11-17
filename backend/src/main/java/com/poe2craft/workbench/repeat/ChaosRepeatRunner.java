package com.poe2craft.workbench.repeat;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * 공통 단일 제작 규칙으로 카오스를 순서대로 실행한다.
 *
 * <p>기존 계약: Sequential manual Chaos uses through the existing Workbench apply contract. Supply, for
 * example, {@code new ChaosRepeatRunner(workbenchService::apply)}. Matching syntax and display
 * language belong to the caller; this runner neither formats modifiers nor starts background jobs.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code apply}: 적용 ({@code Apply}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosRepeatRunner(apply)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code apply}(적용).
 *   <li>{@code run(state, action, activeOmens, maximumUses, random, matcher)}: 작업의 실행 루프를 수행한다. 입력:
 *       {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code
 *       maximumUses}(최대값·사용량), {@code random}(난수생성기), {@code matcher}(matcher). 반환: {@code
 *       Outcome}.
 *   <li>{@code run(state, action, activeOmens, maximumUses, random, matcher, stopRequested)}: 작업의
 *       실행 루프를 수행한다. 입력: {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록),
 *       {@code maximumUses}(최대값·사용량), {@code random}(난수생성기), {@code matcher}(matcher), {@code
 *       stopRequested}(stop·Requested). 반환: {@code Outcome}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Apply}: {@code ChaosRepeatRunner} 내부의 적용 처리를 맡는다.
 *   <li>{@code Termination}: {@code ChaosRepeatRunner}의 종료 구분값을 정의한다.
 *   <li>{@code FailureStage}: {@code ChaosRepeatRunner}의 실패·진행단계 구분값을 정의한다.
 *   <li>{@code Outcome}: {@code ChaosRepeatRunner}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class ChaosRepeatRunner {
  private final Apply apply;

  public ChaosRepeatRunner(Apply apply) {
    this.apply = Objects.requireNonNull(apply);
  }

  /**
   * {@code ChaosRepeatRunner} 내부의 적용 처리를 맡는다.
   *
   * <p>기존 계약: The adapter must retain the existing atomic single-use semantics, including omen
   * consumption.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code apply(state, action, activeOmens, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code
   *       state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code random}(난수생성기). 반환:
   *       {@code WorkbenchSimulator.Result}.
   * </ul>
   */
  @FunctionalInterface
  public interface Apply {
    WorkbenchSimulator.Result apply(
        ItemState state, WorkbenchCurrency action, Set<String> activeOmens, RandomGenerator random);
  }

  public Outcome run(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      int maximumUses,
      RandomGenerator random,
      Predicate<ItemState> matcher) {
    return run(state, action, activeOmens, maximumUses, random, matcher, () -> false);
  }

  /**
   * Records only completed uses. Matching begins after the first completed use, even when the input
   * already matches. Stop is observed between uses, including before the first; it cannot interrupt
   * apply or matching. A match on the last allowed use takes precedence over the limit.
   *
   * <p>The outcome is an internal factual result, not an API response or a decision to commit
   * partial work to the user's item. Runtime failures preserve completed work without retaining
   * exception messages or causes. Fatal JVM errors are not intercepted. Memory does not grow with
   * use count: only the last returned attempt, last applied result and counts by consumed omen are
   * retained.
   */
  public Outcome run(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      int maximumUses,
      RandomGenerator random,
      Predicate<ItemState> matcher,
      BooleanSupplier stopRequested) {
    if (state == null
        || action == null
        || activeOmens == null
        || random == null
        || matcher == null
        || stopRequested == null) throw new IllegalArgumentException("Repeat inputs are required");
    if (action != WorkbenchCurrency.CHAOS
        && action != WorkbenchCurrency.GREATER_CHAOS
        && action != WorkbenchCurrency.PERFECT_CHAOS)
      throw new IllegalArgumentException("Only Chaos currencies can repeat");
    if (maximumUses <= 0) throw new IllegalArgumentException("Maximum uses must be positive");

    var remaining = Set.copyOf(activeOmens);
    var consumed = new HashMap<String, Integer>();
    int completed = 0;
    WorkbenchSimulator.Result last = null;
    WorkbenchSimulator.Result lastApplied = null;
    FailureStage stage = FailureStage.STOP_SIGNAL;
    try {
      while (completed < maximumUses) {
        stage = FailureStage.STOP_SIGNAL;
        if (stopRequested.getAsBoolean())
          return new Outcome(
              state,
              completed,
              consumed,
              remaining,
              last,
              lastApplied,
              Termination.STOP_REQUESTED,
              null);

        stage = FailureStage.APPLY;
        last = Objects.requireNonNull(apply.apply(state, action, remaining, random));
        if (!last.applied())
          return new Outcome(
              state,
              completed,
              consumed,
              remaining,
              last,
              lastApplied,
              Termination.INAPPLICABLE,
              null);

        lastApplied = last;
        state = last.state();
        completed++;
        remaining = Set.copyOf(last.remainingOmens());
        for (var id : last.consumedOmens()) consumed.merge(id, 1, Integer::sum);

        stage = FailureStage.MATCHER;
        if (matcher.test(state))
          return new Outcome(
              state, completed, consumed, remaining, last, lastApplied, Termination.MATCH, null);
      }
      return new Outcome(
          state, completed, consumed, remaining, last, lastApplied, Termination.LIMIT, null);
    } catch (RuntimeException failure) {
      return new Outcome(
          state, completed, consumed, remaining, last, lastApplied, Termination.ERROR, stage);
    }
  }

  /**
   * {@code ChaosRepeatRunner}의 종료 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code MATCH}: 일치 구분값.
   *   <li>{@code LIMIT}: 한도 구분값.
   *   <li>{@code INAPPLICABLE}: INAPPLICABLE 구분값.
   *   <li>{@code STOP_REQUESTED}: REQUESTED 구분값.
   *   <li>{@code ERROR}: 오류 구분값.
   * </ul>
   */
  public enum Termination {
    MATCH,
    LIMIT,
    INAPPLICABLE,
    STOP_REQUESTED,
    ERROR
  }

  /**
   * {@code ChaosRepeatRunner}의 실패·진행단계 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code STOP_SIGNAL}: SIGNAL 구분값.
   *   <li>{@code APPLY}: 적용 구분값.
   *   <li>{@code MATCHER}: MATCHER 구분값.
   * </ul>
   */
  public enum FailureStage {
    STOP_SIGNAL,
    APPLY,
    MATCHER
  }

  /**
   * {@code ChaosRepeatRunner}에서 사용하는 결과의 불변 값이다.
   *
   * <p>기존 계약: lastResult is the last returned single-use result, including an unapplied final
   * attempt; it is null if apply never returned. lastAppliedResult is the last result with
   * applied=true, or null when completedUses is zero. It witnesses the final state even if a later
   * attempt is inapplicable or throws. Each result's events and assumptions describe that one use
   * only. The final state, completedUses and consumedOmens describe all completed uses, including
   * before ERROR/STOP.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code ItemState}).
   *   <li>{@code completedUses}: 완료·사용량 ({@code int}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code Map<String, Integer>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code lastResult}: 마지막·결과 ({@code WorkbenchSimulator.Result}).
   *   <li>{@code lastAppliedResult}: 마지막·적용여부·결과 ({@code WorkbenchSimulator.Result}).
   *   <li>{@code termination}: 종료 ({@code Termination}).
   *   <li>{@code failureStage}: 실패·진행단계 ({@code FailureStage}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Outcome(state, completedUses, consumedOmens, remainingOmens, lastResult,
   *       lastAppliedResult, termination, failureStage)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       state}(상태), {@code completedUses}(완료·사용량), {@code consumedOmens}(소비된·징조목록), {@code
   *       remainingOmens}(남은·징조목록), {@code lastResult}(마지막·결과), {@code
   *       lastAppliedResult}(마지막·적용여부·결과), {@code termination}(종료), {@code failureStage}(실패·진행단계).
   * </ul>
   */
  public record Outcome(
      ItemState state,
      int completedUses,
      Map<String, Integer> consumedOmens,
      Set<String> remainingOmens,
      WorkbenchSimulator.Result lastResult,
      WorkbenchSimulator.Result lastAppliedResult,
      Termination termination,
      FailureStage failureStage) {
    public Outcome {
      consumedOmens = Map.copyOf(consumedOmens);
      remainingOmens = Set.copyOf(remainingOmens);
    }
  }
}
