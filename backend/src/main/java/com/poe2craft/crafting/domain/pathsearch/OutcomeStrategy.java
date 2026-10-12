package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;

/** Reachability bounds for a finite discovered decision graph, including external cycles. */
public final class OutcomeStrategy<S> {
  public static final String VERSION = "outcome-responsive-reachability-v2";

  public record Choice<S>(String methodId, Map<S, Fraction> outcomes, Fraction unresolved) {
    public Choice {
      if (methodId == null || methodId.isBlank())
        throw new IllegalArgumentException("Method id required");
      outcomes = Map.copyOf(outcomes);
      var sum = unresolved;
      for (var probability : outcomes.values()) sum = sum.add(probability);
      if (!sum.equals(Fraction.ONE))
        throw new IllegalArgumentException("Choice mass must equal one");
    }
  }

  public record State<S>(boolean goal, boolean expansionComplete, List<Choice<S>> choices) {
    public State {
      choices = List.copyOf(choices);
      if (choices.stream().map(Choice::methodId).distinct().count() != choices.size())
        throw new IllegalArgumentException("Duplicate method identity at state");
    }
  }

  public record Bounds(Fraction lower, Fraction upper) {
    public boolean exact() {
      return lower.equals(upper);
    }
  }

  public record Recommendation(String methodId, Bounds value, boolean certified) {}

  private final Map<S, State<S>> graph;
  private Map<S, Bounds> values;
  private long iterations;
  private final Map<S, String> improvingChoices = new HashMap<>();

  public OutcomeStrategy(Map<S, State<S>> graph) {
    this.graph = Map.copyOf(graph);
    for (var state : graph.values())
      for (var choice : state.choices())
        for (var target : choice.outcomes().keySet())
          if (!graph.containsKey(target))
            throw new IllegalArgumentException("Missing outcome state");
    // States unable to reach any goal or unknown boundary have exact zero, including closed loops.
    var reachable = new HashSet<S>();
    graph.forEach(
        (id, state) -> {
          if (state.goal()
              || !state.expansionComplete()
              || state.choices().stream().anyMatch(c -> !c.unresolved().equals(Fraction.ZERO)))
            reachable.add(id);
        });
    boolean changed;
    do {
      changed = false;
      for (var entry : graph.entrySet()) {
        if (reachable.contains(entry.getKey())) continue;
        if (entry.getValue().choices().stream()
            .anyMatch(
                c ->
                    c.outcomes().entrySet().stream()
                        .anyMatch(
                            e ->
                                !e.getValue().equals(Fraction.ZERO)
                                    && reachable.contains(e.getKey()))))
          changed |= reachable.add(entry.getKey());
      }
    } while (changed);
    values = new LinkedHashMap<>();
    graph.forEach(
        (id, state) ->
            values.put(
                id,
                new Bounds(
                    state.goal() ? Fraction.ONE : Fraction.ZERO,
                    reachable.contains(id) ? Fraction.ONE : Fraction.ZERO)));
  }

  /** One Bellman sweep. Work limits leave bounds, never a fabricated completed optimum. */
  public void refine() {
    var next = new LinkedHashMap<S, Bounds>();
    for (var entry : graph.entrySet()) {
      var state = entry.getValue();
      if (state.goal()) {
        next.put(entry.getKey(), new Bounds(Fraction.ONE, Fraction.ONE));
        continue;
      }
      var lower = Fraction.ZERO;
      var upper = state.expansionComplete() ? Fraction.ZERO : Fraction.ONE;
      String witness = null;
      for (var choice : state.choices()) {
        var bound = evaluate(choice);
        if (compare(bound.lower(), lower) > 0) witness = choice.methodId();
        lower = maximum(lower, bound.lower());
        upper = maximum(upper, bound.upper());
      }
      if (witness != null && compare(lower, values.get(entry.getKey()).lower()) > 0)
        improvingChoices.put(entry.getKey(), witness);
      next.put(entry.getKey(), new Bounds(lower, upper));
    }
    values = next;
    iterations++;
  }

  public long iterations() {
    return iterations;
  }

  public Map<S, Bounds> bounds() {
    return Map.copyOf(values);
  }

  /** A lower bound winner is provisional until it dominates every alternative's upper bound. */
  public Optional<Recommendation> recommendation(S source) {
    var state = Objects.requireNonNull(graph.get(source), "Unknown source");
    Choice<S> best = null;
    Bounds bestValue = null;
    for (var choice : state.choices()) {
      var value = evaluate(choice);
      if (best == null
          || compare(value.lower(), bestValue.lower()) > 0
          || (value.lower().equals(bestValue.lower())
              && choice.methodId().equals(improvingChoices.get(source)))) {
        best = choice;
        bestValue = value;
      }
    }
    if (best == null || state.goal()) return Optional.empty();
    boolean certified = state.expansionComplete();
    for (var choice : state.choices())
      if (choice != best && compare(bestValue.lower(), evaluate(choice).upper()) < 0)
        certified = false;
    return Optional.of(new Recommendation(best.methodId(), bestValue, certified));
  }

  private Bounds evaluate(Choice<S> choice) {
    var lower = Fraction.ZERO;
    var upper = choice.unresolved();
    for (var outcome : choice.outcomes().entrySet()) {
      var value = values.get(outcome.getKey());
      lower = lower.add(outcome.getValue().multiply(value.lower()));
      upper = upper.add(outcome.getValue().multiply(value.upper()));
    }
    return new Bounds(lower, upper);
  }

  private static Fraction maximum(Fraction a, Fraction b) {
    return compare(a, b) >= 0 ? a : b;
  }

  private static int compare(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }
}
