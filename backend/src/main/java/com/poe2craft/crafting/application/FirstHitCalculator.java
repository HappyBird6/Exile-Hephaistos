package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;
import java.util.function.*;

/** State propagation with absorbing hits. Budgets describe computation, never crafting limits. */
public final class FirstHitCalculator<S> {
  public enum Match {
    HIT,
    MISS,
    UNKNOWN
  }

  public record Edge<S>(S state, Fraction mass) {}

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

  public record Budget(long evaluations, int frontierStates, int fractionBits) {
    public Budget {
      if (evaluations < 1 || frontierStates < 1 || fractionBits < 64)
        throw new IllegalArgumentException("Positive computation budget required");
    }
  }

  public record Point(
      long attempts,
      Fraction lower,
      Fraction upper,
      Fraction active,
      Fraction dead,
      Fraction unresolved,
      String status) {}

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
    Objects.requireNonNull(root);
    Objects.requireNonNull(goal);
    Objects.requireNonNull(transition);
    Objects.requireNonNull(budget);
    Objects.requireNonNull(cancelled);
    if (observations == null || observations.isEmpty() || observations.size() > 32)
      throw new IllegalArgumentException("One to 32 observation points required");
    if (observations.stream().anyMatch(Objects::isNull))
      throw new IllegalArgumentException("Non-null observation points required");
    var sorted = observations.stream().distinct().sorted().toList();
    if (sorted.size() != observations.size() || sorted.getFirst() < 0)
      throw new IllegalArgumentException("Distinct nonnegative observation points required");
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
      var active = Fraction.ZERO;
      for (var mass : frontier.values()) active = active.add(mass);
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
}
