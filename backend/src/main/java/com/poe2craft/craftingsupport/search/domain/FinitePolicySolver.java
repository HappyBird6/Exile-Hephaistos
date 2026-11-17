package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.OutcomeStrategy.*;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 작고 완전히 알려진 결정 그래프에서 정책을 정확히 평가하고 최적성 조건을 검사한다.
 *
 * <p>기존 계약: Exact policy evaluation and Bellman certification for small, fully known decision
 * graphs.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code MAX_STATES}: 상태목록; 초기값 {@code 32} ({@code int}).
 *   <li>{@code graph}: 그래프 ({@code Map<S, State<S>>}).
 *   <li>{@code policy}: 정책 ({@code Map<S, Choice<S>>}).
 *   <li>{@code transientStates}: 임시·상태목록 ({@code List<S>}).
 *   <li>{@code columns}: columns ({@code Map<S, Integer>}).
 *   <li>{@code evaluated}: evaluated ({@code Map<S, Fraction>}).
 *   <li>{@code matrix}: matrix ({@code Rational[][]}).
 *   <li>{@code row}: 행 ({@code int}).
 *   <li>{@code pivot}: pivot ({@code int}).
 *   <li>{@code solution}: 해 ({@code Solution<S>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code FinitePolicySolver(graph)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code graph}(그래프).
 *   <li>{@code supported(graph)}: 지원 처리에서 {@code graph.size}, {@code graph.values}을(를) 사용한다. 입력:
 *       {@code graph}(그래프). 반환: {@code boolean}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 반환: {@code Optional<Solution<S>>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code prepare()}: prepare 처리에서 {@code graph.forEach}, {@code state.goal}을(를) 사용한다.
 *   <li>{@code compare(a, b)}: 비교 처리에서 {@code a.numerator}, {@code b.denominator}을(를) 사용한다. 입력:
 *       {@code a}(a), {@code b}(b). 반환: {@code int}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Solution}: {@code FinitePolicySolver}에서 사용하는 해의 불변 값이다.
 *   <li>{@code Rational}: {@code FinitePolicySolver}에서 사용하는 Rational의 불변 값이다.
 * </ul>
 */
final class FinitePolicySolver<S> {
  static final int MAX_STATES = 32;

  /**
   * {@code FinitePolicySolver}에서 사용하는 해의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code Map<S, Fraction>}).
   *   <li>{@code policy}: 정책 ({@code Map<S, String>}).
   * </ul>
   */
  record Solution<S>(Map<S, Fraction> values, Map<S, String> policy) {}

  private final Map<S, State<S>> graph;
  private final Map<S, Choice<S>> policy = new LinkedHashMap<>();
  private List<S> transientStates;
  private Map<S, Integer> columns;
  private Map<S, Fraction> evaluated;
  private Rational[][] matrix;
  private int row, pivot;
  private Solution<S> solution;

  FinitePolicySolver(Map<S, State<S>> graph) {
    this.graph = graph;
    graph.forEach(
        (id, state) -> {
          if (!state.goal() && !state.choices().isEmpty())
            policy.put(id, state.choices().getFirst());
        });
  }

  static boolean supported(Map<?, ? extends State<?>> graph) {
    return graph.size() <= MAX_STATES
        && graph.values().stream()
            .allMatch(
                state ->
                    state.expansionComplete()
                        && state.choices().stream()
                            .allMatch(c -> c.unresolved().equals(Fraction.ZERO)));
  }

  /** One matrix row or pivot per call. No numerical tolerance is a completion criterion. */
  Optional<Solution<S>> step() {
    if (solution != null) return Optional.of(solution);
    if (matrix == null) {
      prepare();
      return Optional.empty();
    }
    int n = transientStates.size();
    if (row < n) {
      Arrays.fill(matrix[row], Rational.ZERO);
      matrix[row][row] = Rational.ONE;
      var choice = policy.get(transientStates.get(row));
      for (var outcome : choice.outcomes().entrySet()) {
        var probability = Rational.of(outcome.getValue());
        var column = columns.get(outcome.getKey());
        if (column != null) matrix[row][column] = matrix[row][column].subtract(probability);
        else if (graph.get(outcome.getKey()).goal())
          matrix[row][n] = matrix[row][n].add(probability);
      }
      row++;
      return Optional.empty();
    }
    if (pivot < n) {
      int selected = pivot;
      while (selected < n && matrix[selected][pivot].zero()) selected++;
      if (selected == n) throw new IllegalStateException("Transient policy system is singular");
      var swap = matrix[pivot];
      matrix[pivot] = matrix[selected];
      matrix[selected] = swap;
      var divisor = matrix[pivot][pivot];
      for (int j = pivot; j <= n; j++) matrix[pivot][j] = matrix[pivot][j].divide(divisor);
      for (int i = 0; i < n; i++) {
        if (i == pivot) continue;
        var factor = matrix[i][pivot];
        for (int j = pivot; j <= n; j++)
          matrix[i][j] = matrix[i][j].subtract(factor.multiply(matrix[pivot][j]));
      }
      pivot++;
      return Optional.empty();
    }
    for (int i = 0; i < n; i++) evaluated.put(transientStates.get(i), matrix[i][n].fraction());
    boolean improved = false;
    for (var entry : graph.entrySet()) {
      if (entry.getValue().goal()) continue;
      var best = evaluated.get(entry.getKey());
      for (var choice : entry.getValue().choices()) {
        var candidate = Fraction.ZERO;
        for (var outcome : choice.outcomes().entrySet())
          candidate = candidate.add(outcome.getValue().multiply(evaluated.get(outcome.getKey())));
        if (compare(candidate, best) > 0) {
          best = candidate;
          policy.put(entry.getKey(), choice);
          improved = true;
        }
      }
    }
    if (improved) {
      matrix = null;
      return Optional.empty();
    }
    // Evaluated policy attains this vector, and no action exceeds it. Together these prove
    // optimal reachability, including history-dependent strategies and external cycles.
    var witness = new LinkedHashMap<S, String>();
    policy.forEach((id, choice) -> witness.put(id, choice.methodId()));
    solution = new Solution<>(Map.copyOf(evaluated), Map.copyOf(witness));
    return Optional.of(solution);
  }

  private void prepare() {
    var reachable = new LinkedHashSet<S>();
    graph.forEach(
        (id, state) -> {
          if (state.goal()) reachable.add(id);
        });
    boolean changed;
    do {
      changed = false;
      for (var entry : policy.entrySet())
        if (entry.getValue().outcomes().entrySet().stream()
            .anyMatch(e -> !e.getValue().equals(Fraction.ZERO) && reachable.contains(e.getKey())))
          changed |= reachable.add(entry.getKey());
    } while (changed);
    // Every policy class with no path to a goal has least reachability zero. Removing it
    // makes I-Q nonsingular, including policies containing absorbing failure/self loops.
    evaluated = new LinkedHashMap<>();
    graph.forEach((id, state) -> evaluated.put(id, state.goal() ? Fraction.ONE : Fraction.ZERO));
    transientStates = reachable.stream().filter(id -> !graph.get(id).goal()).toList();
    columns = new LinkedHashMap<>();
    for (int i = 0; i < transientStates.size(); i++) columns.put(transientStates.get(i), i);
    matrix = new Rational[transientStates.size()][transientStates.size() + 1];
    row = 0;
    pivot = 0;
  }

  private static int compare(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  /**
   * {@code FinitePolicySolver}에서 사용하는 Rational의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code BigInteger}).
   *   <li>{@code denominator}: 분모 ({@code BigInteger}).
   *   <li>{@code ZERO}: 영 ({@code Rational}).
   *   <li>{@code ONE}: 일 ({@code Rational}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Rational(numerator, denominator)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code numerator}(분자),
   *       {@code denominator}(분모). 사용하는 예외: {@code ArithmeticException}.
   *   <li>{@code of(f)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code f}(f). 반환: {@code Rational}.
   *   <li>{@code zero()}: 영에 해당하는 값을 제공한다. 반환: {@code boolean}.
   *   <li>{@code add(r)}: 값 또는 항목을 더한다. 입력: {@code r}(r). 반환: {@code Rational}.
   *   <li>{@code subtract(r)}: 두 값의 차를 계산한다. 입력: {@code r}(r). 반환: {@code Rational}.
   *   <li>{@code multiply(r)}: 두 값의 곱을 계산한다. 입력: {@code r}(r). 반환: {@code Rational}.
   *   <li>{@code divide(r)}: 두 값의 비를 계산한다. 입력: {@code r}(r). 반환: {@code Rational}.
   *   <li>{@code fraction()}: 유리수 값을 조회·구성한다. 반환: {@code Fraction}.
   * </ul>
   */
  private record Rational(BigInteger numerator, BigInteger denominator) {
    static final Rational ZERO = new Rational(BigInteger.ZERO, BigInteger.ONE);
    static final Rational ONE = new Rational(BigInteger.ONE, BigInteger.ONE);

    Rational {
      if (denominator.signum() == 0) throw new ArithmeticException("Zero denominator");
      if (denominator.signum() < 0) {
        numerator = numerator.negate();
        denominator = denominator.negate();
      }
      var gcd = numerator.gcd(denominator);
      numerator = numerator.divide(gcd);
      denominator = denominator.divide(gcd);
    }

    static Rational of(Fraction f) {
      return new Rational(f.numerator(), f.denominator());
    }

    boolean zero() {
      return numerator.signum() == 0;
    }

    Rational add(Rational r) {
      return new Rational(
          numerator.multiply(r.denominator).add(r.numerator.multiply(denominator)),
          denominator.multiply(r.denominator));
    }

    Rational subtract(Rational r) {
      return add(new Rational(r.numerator.negate(), r.denominator));
    }

    Rational multiply(Rational r) {
      return new Rational(numerator.multiply(r.numerator), denominator.multiply(r.denominator));
    }

    Rational divide(Rational r) {
      return new Rational(numerator.multiply(r.denominator), denominator.multiply(r.numerator));
    }

    Fraction fraction() {
      return new Fraction(numerator, denominator);
    }
  }
}
