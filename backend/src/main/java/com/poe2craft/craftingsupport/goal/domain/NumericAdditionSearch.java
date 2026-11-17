package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.ExactNumericDistribution.*;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.crafting.domain.AdditionRules;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Function;

/**
 * 유한한 옵션 추가 순서에서 최초 목표 달성 확률을 계산한다.
 *
 * <p>기존 계약: Finite open-loop addition sequences, with first success absorbed before the next
 * action.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code compare(catalog, root, evaluate, budget)}: 비교 처리에서 {@code budget.exhausted}, {@code
 *       comparisons.add}을(를) 사용한다. 입력: {@code catalog}(카탈로그), {@code root}(시작점), {@code
 *       evaluate}(평가), {@code budget}(계산예산). 반환: {@code Result}.
 *   <li>{@code firstHit(root, sequence, kernel, evaluate, budget)}: 최초·목표도달 처리에서 {@code
 *       frontier.put}, {@code sequence.size}을(를) 사용한다. 입력: {@code root}(시작점), {@code sequence}(순서),
 *       {@code kernel}(전이커널), {@code evaluate}(평가), {@code budget}(계산예산). 반환: {@code Comparison}.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code compareFractions(a, b)}: Fractions을(를) 비교한다. 입력: {@code a}(a), {@code b}(b). 반환:
 *       {@code int}.
 *   <li>{@code decimal(f)}: decimal 처리에서 {@code f.numerator}, {@code f.denominator}을(를) 사용한다. 입력:
 *       {@code f}(f). 반환: {@code double}.
 *   <li>{@code sequences(root)}: 순서목록 처리에서 {@code root.rarity}, {@code root.explicits}을(를) 사용한다.
 *       입력: {@code root}(시작점). 반환: {@code List<List<WorkbenchCurrency>>}.
 *   <li>{@code append(result, prefix, rarity, count, level)}: 뒤에 값이나 계산 행을 추가한다. 입력: {@code
 *       result}(결과), {@code prefix}(접두), {@code rarity}(희귀도), {@code count}(개수), {@code level}(레벨).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code ExactMass}: {@code NumericAdditionSearch}에서 사용하는 정확값·확률질량의 불변 값이다.
 *   <li>{@code Comparison}: {@code NumericAdditionSearch}에서 사용하는 비교의 불변 값이다.
 *   <li>{@code Result}: {@code NumericAdditionSearch}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Budget}: {@code NumericAdditionSearch} 내부의 계산예산 처리를 맡는다.
 * </ul>
 */
public final class NumericAdditionSearch {
  /**
   * {@code NumericAdditionSearch}에서 사용하는 정확값·확률질량의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code success}: 성공 ({@code Fraction}).
   *   <li>{@code failure}: 실패 ({@code Fraction}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code terminalFailure}: 종료·실패 ({@code Fraction}).
   *   <li>{@code illegalActionFailure}: illegal·행동·실패 ({@code Fraction}).
   * </ul>
   */
  public record ExactMass(
      Fraction success,
      Fraction failure,
      Fraction unresolved,
      Fraction terminalFailure,
      Fraction illegalActionFailure) {}

  /**
   * {@code NumericAdditionSearch}에서 사용하는 비교의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code sequence}: 순서 ({@code List<String>}).
   *   <li>{@code successLower}: 성공·하한 ({@code double}).
   *   <li>{@code successUpper}: 성공·상한 ({@code double}).
   *   <li>{@code failureProbability}: 실패·확률 ({@code double}).
   *   <li>{@code unresolvedProbability}: 미해결·확률 ({@code double}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code exactMass}: 정확값·확률질량 ({@code ExactMass}).
   * </ul>
   */
  public record Comparison(
      List<String> sequence,
      double successLower,
      double successUpper,
      double failureProbability,
      double unresolvedProbability,
      boolean complete,
      ExactMass exactMass) {}

  /**
   * {@code NumericAdditionSearch}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code comparisons}: comparisons ({@code List<Comparison>}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code totalSequences}: 합계·순서목록 ({@code int}).
   * </ul>
   */
  public record Result(List<Comparison> comparisons, boolean complete, int totalSequences) {}

  /**
   * {@code NumericAdditionSearch} 내부의 계산예산 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code maxStates}: 최대값·상태목록 ({@code int}).
   *   <li>{@code maxEdges}: 최대값·간선목록 ({@code int}).
   *   <li>{@code started}: 시작시각 ({@code long}).
   *   <li>{@code duration}: 소요시간 ({@code long}).
   *   <li>{@code states}: 상태목록 ({@code int}).
   *   <li>{@code edges}: 간선목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Budget(maxStates, maxEdges, maxMillis)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       maxStates}(최대값·상태목록), {@code maxEdges}(최대값·간선목록), {@code maxMillis}(최대값·밀리초).
   *   <li>{@code exhausted()}: exhausted 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code timedOut()}: timed·출력 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code state()}: 현재 상태 표현을 조회한다. 반환: {@code boolean}.
   *   <li>{@code edge()}: 간선 값을 조회·구성한다. 반환: {@code boolean}.
   * </ul>
   */
  public static final class Budget {
    private final int maxStates, maxEdges;
    private final long started = System.nanoTime(), duration;
    private int states, edges;

    public Budget(int maxStates, int maxEdges, int maxMillis) {
      this.maxStates = maxStates;
      this.maxEdges = maxEdges;
      duration = (long) maxMillis * 1_000_000;
    }

    public boolean exhausted() {
      return states >= maxStates || edges >= maxEdges || timedOut();
    }

    private boolean timedOut() {
      return System.nanoTime() - started >= duration;
    }

    boolean state() {
      if (states >= maxStates || timedOut()) return false;
      states++;
      return true;
    }

    boolean edge() {
      if (edges >= maxEdges || timedOut()) return false;
      edges++;
      return true;
    }
  }

  public Result compare(
      ItemCatalog catalog, ItemState root, Function<ItemState, Status> evaluate, Budget budget) {
    var sequences = sequences(root);
    var kernel = new NumericAdditionKernel(catalog);
    var comparisons = new ArrayList<Comparison>();
    for (var sequence : sequences) {
      if (budget.exhausted()) break;
      comparisons.add(firstHit(root, sequence, kernel, evaluate, budget));
    }
    boolean complete =
        comparisons.size() == sequences.size()
            && comparisons.stream().allMatch(Comparison::complete);
    comparisons.sort((a, b) -> compareFractions(b.exactMass().success(), a.exactMass().success()));
    return new Result(List.copyOf(comparisons), complete, sequences.size());
  }

  public Comparison firstHit(
      ItemState root,
      List<WorkbenchCurrency> sequence,
      NumericAdditionKernel kernel,
      Function<ItemState, Status> evaluate,
      Budget budget) {
    var frontier = new LinkedHashMap<ItemState, Fraction>();
    frontier.put(root, Fraction.ONE);
    var success = Fraction.ZERO;
    var terminal = Fraction.ZERO;
    var illegal = Fraction.ZERO;
    var unresolved = Fraction.ZERO;
    for (int depth = 0; depth <= sequence.size(); depth++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var entry : frontier.entrySet()) {
        var mass = entry.getValue();
        if (!budget.state()) {
          unresolved = unresolved.add(mass);
          continue;
        }
        var status = evaluate.apply(entry.getKey());
        if (status == Status.MATCH) {
          success = success.add(mass);
          continue;
        }
        if (status != Status.NO_MATCH) {
          unresolved = unresolved.add(mass);
          continue;
        }
        if (depth == sequence.size()) {
          terminal = terminal.add(mass);
          continue;
        }
        var expanded = kernel.expand(entry.getKey(), sequence.get(depth), budget::edge);
        if (!expanded.available()) {
          illegal = illegal.add(mass);
          continue;
        }
        unresolved = unresolved.add(mass.multiply(expanded.unresolved()));
        for (var outcome : expanded.outcomes())
          next.merge(outcome.item(), mass.multiply(outcome.probability()), Fraction::add);
      }
      frontier = next;
    }
    var failure = terminal.add(illegal);
    if (!success.add(failure).add(unresolved).equals(Fraction.ONE))
      throw new IllegalStateException("Search mass must equal one");
    return new Comparison(
        sequence.stream().map(Enum::name).toList(),
        decimal(success),
        decimal(success.add(unresolved)),
        decimal(failure),
        decimal(unresolved),
        unresolved.equals(Fraction.ZERO),
        new ExactMass(success, failure, unresolved, terminal, illegal));
  }

  private static int compareFractions(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  private static double decimal(Fraction f) {
    return new BigDecimal(f.numerator())
        .divide(new BigDecimal(f.denominator()), MathContext.DECIMAL128)
        .doubleValue();
  }

  /** Every prefix (including Stop) is a policy; no removal, recovery, or repeat cycles. */
  public static List<List<WorkbenchCurrency>> sequences(ItemState root) {
    var result = new ArrayList<List<WorkbenchCurrency>>();
    append(result, List.of(), root.rarity(), root.explicits().size(), root.itemLevel());
    result.sort(Comparator.comparingInt(List::size));
    return List.copyOf(result);
  }

  private static void append(
      List<List<WorkbenchCurrency>> result,
      List<WorkbenchCurrency> prefix,
      ItemState.Rarity rarity,
      int count,
      int level) {
    result.add(List.copyOf(prefix));
    if (count >= 6) return;
    for (var action : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(action) || action.minimumModifierLevel() > level) continue;
      boolean legal =
          switch (action.baseAction()) {
            case TRANSMUTATION -> rarity == ItemState.Rarity.NORMAL;
            case AUGMENTATION -> rarity == ItemState.Rarity.MAGIC && count < 2;
            case REGAL -> rarity == ItemState.Rarity.MAGIC;
            case EXALTED -> rarity == ItemState.Rarity.RARE;
            default -> false;
          };
      if (!legal) continue;
      var next = new ArrayList<>(prefix);
      next.add(action);
      var upgraded =
          action.baseAction() == CraftingAction.TRANSMUTATION
              ? ItemState.Rarity.MAGIC
              : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : rarity;
      append(result, next, upgraded, count + 1, level);
    }
  }
}
