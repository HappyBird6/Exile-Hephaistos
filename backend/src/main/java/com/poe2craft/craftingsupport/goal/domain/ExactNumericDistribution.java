package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 검증된 유한 결합 결과에 대한 정확한 수치 확률을 계산한다.
 *
 * <p>기존 계약: Exact finite oracle for externally proven joint outcomes, not a production game roll
 * model. Full ItemState keys retain numeric rolls, family identities, rarity, layer and quality.
 * Joint tuples are indivisible: no product-of-marginals or occupied/qualified merging.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ExactNumericDistribution()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code firstHit(root, steps, evaluation, maxEvaluations)}: 최초·목표도달 처리에서 {@code
 *       frontier.put}, {@code steps.size}을(를) 사용한다. 입력: {@code root}(시작점), {@code steps}(단계목록),
 *       {@code evaluation}(평가), {@code maxEvaluations}(최대값·평가목록). 반환: {@code Bounds}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Outcome}: {@code ExactNumericDistribution}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Bounds}: {@code ExactNumericDistribution}에서 사용하는 경계값의 불변 값이다.
 * </ul>
 */
public final class ExactNumericDistribution {
  private ExactNumericDistribution() {}

  /**
   * {@code ExactNumericDistribution}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   */
  public record Outcome(ItemState item, Fraction probability) {}

  /**
   * {@code ExactNumericDistribution}에서 사용하는 경계값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code success}: 성공 ({@code Fraction}).
   *   <li>{@code failure}: 실패 ({@code Fraction}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code upper()}: 상한 값을 조회·구성한다. 반환: {@code Fraction}.
   * </ul>
   */
  public record Bounds(Fraction success, Fraction failure, Fraction unresolved, boolean complete) {
    public Fraction upper() {
      return success.add(unresolved);
    }
  }

  /** Budgets produce unresolved mass only inside a validated normalized finite kernel. */
  public static Bounds firstHit(
      ItemState root,
      List<Function<ItemState, List<Outcome>>> steps,
      Function<ItemState, Status> evaluation,
      int maxEvaluations) {
    if (maxEvaluations < 1)
      throw new IllegalArgumentException("Positive evaluation budget required");
    var frontier = new LinkedHashMap<ItemState, Fraction>();
    frontier.put(root, Fraction.ONE);
    var success = Fraction.ZERO;
    var failure = Fraction.ZERO;
    var unresolved = Fraction.ZERO;
    int evaluated = 0;
    for (int depth = 0; depth <= steps.size(); depth++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var entry : frontier.entrySet()) {
        if (evaluated++ >= maxEvaluations) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        var status = evaluation.apply(entry.getKey());
        if (status == Status.MATCH) {
          success = success.add(entry.getValue());
          continue;
        }
        if (status == Status.UNKNOWN || status == Status.UNSUPPORTED) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        if (depth == steps.size()) {
          failure = failure.add(entry.getValue());
          continue;
        }
        var outcomes = steps.get(depth).apply(entry.getKey());
        var total = Fraction.ZERO;
        for (var outcome : outcomes) total = total.add(outcome.probability());
        if (!total.equals(Fraction.ONE))
          throw new IllegalArgumentException("Kernel mass must equal one");
        for (var outcome : outcomes)
          next.merge(
              outcome.item(), entry.getValue().multiply(outcome.probability()), Fraction::add);
      }
      frontier = next;
    }
    return new Bounds(success, failure, unresolved, unresolved.equals(Fraction.ZERO));
  }
}
