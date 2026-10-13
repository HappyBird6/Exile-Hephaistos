package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;

/** Conservative adaptive reachability bounds; residual iteration width is not arithmetic error. */
public final class CertifiedOutcomeStrategy<S> {
  public static final String VERSION = "certified-adaptive-reachability-v1";

  public record Choice<S>(
      String id, Map<S, CertifiedInterval> outcomes, CertifiedInterval unresolved) {
    public Choice {
      outcomes = Map.copyOf(outcomes);
      if (!unresolved.equals(unresolved.probability()))
        throw new IllegalArgumentException("Probability required");
      var mass = unresolved;
      for (var probability : outcomes.values()) {
        if (!probability.equals(probability.probability()))
          throw new IllegalArgumentException("Probability required");
        mass = mass.add(probability);
      }
      if (!mass.contains(Fraction.ONE))
        throw new IllegalArgumentException("Enclosing transition law required");
    }
  }

  public record State<S>(boolean goal, boolean open, List<Choice<S>> choices) {
    public State {
      choices = List.copyOf(choices);
    }
  }

  public record Result(
      CertifiedInterval value,
      String status,
      List<String> methodIds,
      boolean reachableOpen,
      boolean reachableUnresolved,
      boolean transitionArithmetic,
      boolean fixedPointPending) {}

  private final Map<S, State<S>> graph;
  private final Map<S, CertifiedInterval> values = new LinkedHashMap<>();
  private final Map<S, String> almostSure = new HashMap<>();
  private final int bits;
  private final Set<S> possible, open, unresolved, arithmetic;
  private long iterations;

  public CertifiedOutcomeStrategy(Map<S, State<S>> graph, int bits) {
    new CertifiedInterval.Precision(bits, 0);
    this.graph = Map.copyOf(graph);
    this.bits = bits;
    var goals = new HashSet<S>();
    var openSeeds = new HashSet<S>();
    var unresolvedSeeds = new HashSet<S>();
    var arithmeticSeeds = new HashSet<S>();
    graph.forEach(
        (id, state) -> {
          if (state.goal()) goals.add(id);
          if (state.open()) openSeeds.add(id);
          var ids = new HashSet<String>();
          for (var choice : state.choices()) {
            if (!ids.add(choice.id()) || choice.unresolved().fractionalBits() != bits)
              throw new IllegalArgumentException("Invalid choice identity or precision");
            if (!choice.unresolved().zero()) unresolvedSeeds.add(id);
            if (!choice.unresolved().exact()) arithmeticSeeds.add(id);
            choice
                .outcomes()
                .forEach(
                    (target, probability) -> {
                      if (!graph.containsKey(target) || probability.fractionalBits() != bits)
                        throw new IllegalArgumentException("Missing target or precision mismatch");
                      if (!probability.exact()) arithmeticSeeds.add(id);
                    });
          }
        });
    open = predecessors(openSeeds);
    unresolved = predecessors(unresolvedSeeds);
    arithmetic = predecessors(arithmeticSeeds);
    var potential = new HashSet<>(goals);
    potential.addAll(openSeeds);
    potential.addAll(unresolvedSeeds);
    possible = predecessors(potential);
    proveAlmostSure(goals);
    graph.forEach(
        (id, state) ->
            values.put(
                id,
                state.goal() || almostSure.containsKey(id)
                    ? CertifiedInterval.one(bits)
                    : !possible.contains(id)
                        ? CertifiedInterval.zero(bits)
                        : new CertifiedInterval(
                            bits,
                            java.math.BigInteger.ZERO,
                            java.math.BigInteger.ONE.shiftLeft(bits))));
  }

  private Set<S> predecessors(Set<S> seeds) {
    var result = new HashSet<>(seeds);
    boolean changed;
    do {
      changed = false;
      for (var entry : graph.entrySet()) {
        if (entry.getValue().goal() || result.contains(entry.getKey())) continue;
        if (entry.getValue().choices().stream()
            .anyMatch(
                c ->
                    c.outcomes().entrySet().stream()
                        .anyMatch(e -> !e.getValue().zero() && result.contains(e.getKey()))))
          changed |= result.add(entry.getKey());
      }
    } while (changed);
    return result;
  }

  /** Nested support fixed point with a decreasing-rank proper-policy witness. */
  private void proveAlmostSure(Set<S> goals) {
    var candidate = new HashSet<>(graph.keySet());
    while (true) {
      var region = candidate;
      var reached = new HashSet<>(goals);
      var witness = new HashMap<S, String>();
      boolean changed;
      do {
        changed = false;
        for (var id : candidate) {
          var state = graph.get(id);
          if (state.goal() || reached.contains(id)) continue;
          for (var choice : state.choices()) {
            if (!choice.unresolved().zero()) continue;
            var support =
                choice.outcomes().entrySet().stream().filter(e -> !e.getValue().zero()).toList();
            // A zero lower bound does not prove a positive transition. Such edges cannot
            // justify the decreasing rank, although they remain in the closure check.
            if (!support.isEmpty()
                && support.stream().allMatch(e -> region.contains(e.getKey()))
                && support.stream()
                    .anyMatch(
                        e ->
                            e.getValue().lowerUnits().signum() > 0
                                && reached.contains(e.getKey()))) {
              reached.add(id);
              witness.put(id, choice.id());
              changed = true;
              break;
            }
          }
        }
      } while (changed);
      if (reached.equals(candidate)) {
        almostSure.putAll(witness);
        return;
      }
      candidate = reached;
    }
  }

  public long iterations() {
    return iterations;
  }

  public void refine() {
    var next = new LinkedHashMap<S, CertifiedInterval>();
    graph.forEach(
        (id, state) -> {
          var old = values.get(id);
          if (state.goal() || almostSure.containsKey(id) || !possible.contains(id)) {
            next.put(id, old);
            return;
          }
          var lower = java.math.BigInteger.ZERO;
          var upper =
              state.open() ? java.math.BigInteger.ONE.shiftLeft(bits) : java.math.BigInteger.ZERO;
          for (var choice : state.choices()) {
            var q = action(choice);
            lower = lower.max(q.lowerUnits());
            upper = upper.max(q.upperUnits());
          }
          next.put(
              id,
              new CertifiedInterval(
                  bits, old.lowerUnits().max(lower), old.upperUnits().min(upper)));
        });
    values.clear();
    values.putAll(next);
    iterations++;
  }

  private CertifiedInterval action(Choice<S> choice) {
    var result =
        new CertifiedInterval(bits, java.math.BigInteger.ZERO, choice.unresolved().upperUnits());
    for (var outcome : choice.outcomes().entrySet())
      result = result.add(outcome.getValue().multiply(values.get(outcome.getKey())));
    return result.probability();
  }

  public Result result(S id) {
    var state = graph.get(id);
    if (state == null) throw new IllegalArgumentException("Unknown strategy state");
    var value = values.get(id);
    if (almostSure.containsKey(id))
      return new Result(
          value,
          "PROVEN_ONE",
          List.of(almostSure.get(id)),
          open.contains(id),
          unresolved.contains(id),
          arithmetic.contains(id),
          false);
    if (state.goal() || !possible.contains(id))
      return new Result(
          value,
          state.goal() ? "GOAL" : "PROVEN_ZERO",
          List.of(),
          open.contains(id),
          unresolved.contains(id),
          arithmetic.contains(id),
          false);
    var actions =
        state.choices().stream()
            .map(
                c ->
                    new CertifiedRecommendations.Choice(
                        c.id(), action(c), CertifiedInterval.zero(bits)))
            .toList();
    var ranked = CertifiedRecommendations.local(actions, !state.open());
    boolean modelSupported =
        state.choices().stream()
            .filter(c -> ranked.methodIds().contains(c.id()))
            .allMatch(c -> c.unresolved().zero());
    // Action separation is a recommendation proof, not convergence of its eventual value.
    return new Result(
        value,
        ranked.status().equals("CERTIFIED") && modelSupported ? "ACTION_CERTIFIED" : "PENDING",
        modelSupported ? ranked.methodIds() : List.of(),
        open.contains(id),
        unresolved.contains(id),
        arithmetic.contains(id),
        !value.exact());
  }
}
