package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;

/**
 * Exact labelled probabilistic bisimulation for closed finite fixtures. This proof oracle requires
 * the original graph; it is not an on-demand production state-space reduction. Observation keys
 * must be immutable and include every goal/checkpoint judgement relevant to the caller.
 */
public final class FiniteQuotientProof<S> {
  public record Action<S>(String label, Map<S, Fraction> outcomes, Map<String, Fraction> costs) {
    public Action {
      if (label == null || label.isBlank())
        throw new IllegalArgumentException("Action label required");
      outcomes = Map.copyOf(outcomes);
      costs = Map.copyOf(costs);
      if (!sum(outcomes).equals(Fraction.ONE))
        throw new IllegalArgumentException("Only complete unit-mass transition laws can be proved");
    }
  }

  public record Node<S>(Object observation, boolean complete, List<Action<S>> actions) {
    public Node {
      Objects.requireNonNull(observation);
      actions = List.copyOf(actions);
      if (!complete || actions.stream().map(Action::label).distinct().count() != actions.size())
        throw new IllegalArgumentException("Complete states with unique action labels required");
    }
  }

  public record Conditional<S>(Fraction mass, Map<S, Fraction> law) {}

  private record Law(Map<Integer, Fraction> blocks, Map<String, Fraction> costs) {}

  private record Signature(int previousBlock, Map<String, Law> actions) {}

  private final Map<S, Node<S>> original;
  private final Map<S, Integer> blocks;
  private final Map<Integer, Node<Integer>> quotient;

  public FiniteQuotientProof(Map<S, Node<S>> graph) {
    original = Map.copyOf(graph);
    for (var node : graph.values())
      for (var action : node.actions())
        for (var state : action.outcomes().keySet())
          if (!graph.containsKey(state)) throw new IllegalArgumentException("Graph must be closed");
    var observations = new HashMap<Object, Integer>();
    var partition = new LinkedHashMap<S, Integer>();
    graph.forEach(
        (s, n) ->
            partition.put(
                s, observations.computeIfAbsent(n.observation(), ignored -> observations.size())));
    while (true) {
      var keys = new HashMap<Signature, Integer>();
      var refined = new LinkedHashMap<S, Integer>();
      for (var entry : graph.entrySet()) {
        var actions = new HashMap<String, Law>();
        for (var action : entry.getValue().actions())
          actions.put(
              action.label(), new Law(aggregate(action.outcomes(), partition), action.costs()));
        var key = new Signature(partition.get(entry.getKey()), Map.copyOf(actions));
        refined.put(entry.getKey(), keys.computeIfAbsent(key, ignored -> keys.size()));
      }
      // Refinement never merges previous classes; an unchanged class count proves stability.
      boolean stable = new HashSet<>(partition.values()).size() == keys.size();
      partition.clear();
      partition.putAll(refined);
      if (stable) break;
    }
    blocks = Map.copyOf(partition);
    var reduced = new LinkedHashMap<Integer, Node<Integer>>();
    graph.forEach(
        (state, node) ->
            reduced.computeIfAbsent(
                blocks.get(state),
                ignored ->
                    new Node<>(
                        node.observation(),
                        true,
                        node.actions().stream()
                            .map(
                                action ->
                                    new Action<>(
                                        action.label(),
                                        aggregate(action.outcomes(), blocks),
                                        action.costs()))
                            .toList())));
    quotient = Map.copyOf(reduced);
  }

  public Map<S, Integer> blocks() {
    return blocks;
  }

  public Map<Integer, Node<Integer>> graph() {
    return quotient;
  }

  /**
   * Push one specified entry law through one action, then condition on its selected result block.
   */
  public Conditional<S> condition(Map<S, Fraction> entry, String action, int targetBlock) {
    if (!sum(entry).equals(Fraction.ONE))
      throw new IllegalArgumentException("Normalized entry law required");
    var sourceBlocks = new HashSet<Integer>();
    var reached = new LinkedHashMap<S, Fraction>();
    for (var source : entry.entrySet()) {
      var node = original.get(source.getKey());
      if (node == null) throw new IllegalArgumentException("Unknown entry state");
      if (source.getValue().equals(Fraction.ZERO)) continue;
      sourceBlocks.add(blocks.get(source.getKey()));
      var selected =
          node.actions().stream().filter(a -> a.label().equals(action)).findFirst().orElseThrow();
      selected
          .outcomes()
          .forEach(
              (target, p) -> {
                if (blocks.get(target) == targetBlock && !p.equals(Fraction.ZERO))
                  reached.merge(target, source.getValue().multiply(p), Fraction::add);
              });
    }
    if (sourceBlocks.size() != 1)
      throw new IllegalArgumentException("Entry law must belong to one proved block");
    var mass = sum(reached);
    var conditional = new LinkedHashMap<S, Fraction>();
    if (!mass.equals(Fraction.ZERO))
      reached.forEach(
          (state, p) ->
              conditional.put(
                  state,
                  new Fraction(
                      p.numerator().multiply(mass.denominator()),
                      p.denominator().multiply(mass.numerator()))));
    return new Conditional<>(mass, Map.copyOf(conditional));
  }

  public Map<Integer, Fraction> project(Map<S, Fraction> law) {
    return aggregate(law, blocks);
  }

  private static <S> Map<Integer, Fraction> aggregate(
      Map<S, Fraction> law, Map<S, Integer> blocks) {
    var result = new HashMap<Integer, Fraction>();
    law.forEach(
        (state, mass) -> {
          var block = Objects.requireNonNull(blocks.get(state), "Unknown state");
          if (!mass.equals(Fraction.ZERO)) result.merge(block, mass, Fraction::add);
        });
    return Map.copyOf(result);
  }

  private static Fraction sum(Map<?, Fraction> law) {
    return law.values().stream().reduce(Fraction.ZERO, Fraction::add);
  }
}
