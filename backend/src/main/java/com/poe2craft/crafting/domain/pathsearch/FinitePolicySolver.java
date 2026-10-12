package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy.*;
import java.math.BigInteger;
import java.util.*;

/** Exact policy evaluation and Bellman certification for small, fully known decision graphs. */
final class FinitePolicySolver<S> {
  static final int MAX_STATES = 32;

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
