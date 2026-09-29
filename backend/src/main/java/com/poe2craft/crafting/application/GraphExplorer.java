package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.CraftingAction;
import com.poe2craft.crafting.domain.StateBucket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Layered probability propagation for a fixed action plan. Shared/cyclic graph nodes remain shared.
 */
public final class GraphExplorer {
  private final TransitionCache transitions;

  public GraphExplorer(TransitionCache transitions) {
    this.transitions = transitions;
  }

  public Result explore(
      StateBucket root,
      List<CraftingAction> plan,
      int maxNodes,
      int maxEdges,
      BooleanSupplier stop) {
    if (plan == null
        || plan.isEmpty()
        || plan.size() > 4
        || plan.stream().anyMatch(java.util.Objects::isNull)
        || maxNodes < 1
        || maxNodes > 2000
        || maxEdges < 1
        || maxEdges > 10000) {
      throw new IllegalArgumentException("Invalid exploration limits or action plan");
    }
    Map<String, StateBucket> nodes = new LinkedHashMap<>();
    nodes.put(root.id(), root);
    Map<StateBucket, Double> frontier = new LinkedHashMap<>();
    frontier.put(root, 1.0);
    var edges = new ArrayList<Edge>();
    var terminals = new ArrayList<Terminal>();
    double blocked = 0, deferred = 0;
    for (int step = 0; step < plan.size(); step++) {
      Map<StateBucket, Double> next = new LinkedHashMap<>();
      for (var entry : frontier.entrySet()) {
        var state = entry.getKey();
        double mass = entry.getValue();
        if (stop.getAsBoolean()) {
          terminals.add(new Terminal(state.id(), step, "DEFERRED", mass));
          deferred += mass;
          continue;
        }
        var result = transitions.get(state, plan.get(step));
        if (!result.available()) {
          terminals.add(new Terminal(state.id(), step, "BLOCKED", mass));
          blocked += mass;
          continue;
        }
        long added = result.outcomes().stream().filter(o -> !nodes.containsKey(o.id())).count();
        if (nodes.size() + added > maxNodes
            || edges.size() + result.outcomes().size() > maxEdges
            || stop.getAsBoolean()) {
          terminals.add(new Terminal(state.id(), step, "DEFERRED", mass));
          deferred += mass;
          continue;
        }
        for (var outcome : result.outcomes()) {
          nodes.putIfAbsent(outcome.id(), outcome.state());
          edges.add(
              new Edge(state.id(), outcome.id(), step, plan.get(step), outcome.probability()));
          next.merge(outcome.state(), mass * outcome.probability(), Double::sum);
        }
      }
      frontier = next;
    }
    double completed = 0;
    for (var entry : frontier.entrySet()) {
      terminals.add(new Terminal(entry.getKey().id(), plan.size(), "COMPLETE", entry.getValue()));
      completed += entry.getValue();
    }
    terminals.sort(
        java.util.Comparator.comparingDouble(Terminal::probability)
            .reversed()
            .thenComparing(Terminal::id));
    return new Result(
        Map.copyOf(nodes),
        List.copyOf(edges),
        List.copyOf(terminals),
        completed,
        blocked,
        deferred,
        deferred == 0);
  }

  public record Edge(
      String fromId, String toId, int step, CraftingAction action, double probability) {}

  public record Terminal(String id, int step, String status, double probability) {}

  public record Result(
      Map<String, StateBucket> nodes,
      List<Edge> edges,
      List<Terminal> terminals,
      double completedProbability,
      double blockedProbability,
      double unexploredProbability,
      boolean complete) {}
}
