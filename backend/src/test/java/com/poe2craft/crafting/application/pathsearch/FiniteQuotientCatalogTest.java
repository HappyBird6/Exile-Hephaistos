package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.FiniteQuotientProof;
import com.poe2craft.crafting.domain.pathsearch.FiniteQuotientProof.*;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FiniteQuotientCatalogTest {
  private final MethodSearchCalculationTest fixture = new MethodSearchCalculationTest();

  @Test
  void futureTransitionDifferencesAndCostsForcePartitionRefinement() {
    var graph =
        Map.of(
            "a",
            node("start", "c", 1),
            "b",
            node("start", "d", 1),
            "c",
            node("middle", "goal", 1),
            "d",
            node("middle", "d", 1),
            "goal",
            new Node<String>("goal", true, List.of()));
    assertThat(new FiniteQuotientProof<>(graph).graph()).hasSize(5);
    var costs =
        Map.of(
            "a",
            node("same", "goal", 1),
            "b",
            node("same", "goal", 2),
            "goal",
            new Node<String>("goal", true, List.of()));
    assertThat(new FiniteQuotientProof<>(costs).graph()).hasSize(3);
    assertThatThrownBy(() -> new FiniteQuotientProof<>(Map.of("a", node("same", "missing", 1))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Node<String> node(String observation, String target, int cost) {
    return new Node<>(
        observation,
        true,
        List.of(
            new Action<>(
                "act", Map.of(target, Fraction.ONE), Map.of("currency", Fraction.of(cost, 1)))));
  }

  private Map<BasicCurrencyState, Node<BasicCurrencyState>> closedGraph() {
    var graph = new LinkedHashMap<BasicCurrencyState, Node<BasicCurrencyState>>();
    var queue = new ArrayDeque<BasicCurrencyState>();
    queue.add(fixture.root());
    while (!queue.isEmpty()) {
      var state = queue.remove();
      if (graph.containsKey(state)) continue;
      var actions = new ArrayList<Action<BasicCurrencyState>>();
      for (var currency : BasicCurrencyTransitions.supportedActions()) {
        var result = fixture.transitions.expand(state, currency, Set.of(), 1000);
        if (result.status() == BasicCurrencyTransitions.Status.UNAVAILABLE) continue;
        assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
        var law = new LinkedHashMap<BasicCurrencyState, Fraction>();
        result
            .outcomes()
            .forEach(
                outcome -> {
                  law.merge(outcome.state(), outcome.probability(), Fraction::add);
                  if (!graph.containsKey(outcome.state())) queue.add(outcome.state());
                });
        actions.add(new Action<>(currency.name(), law, Map.of(currency.name(), Fraction.ONE)));
      }
      graph.put(state, new Node<>(fixture.progress(state.item()), true, actions));
    }
    return graph;
  }

  @Test
  void tinyCatalogPreservesAllActionLawsFirstHitObservationsAndDecisionRankings() {
    var full = closedGraph();
    var proof = new FiniteQuotientProof<>(full);
    assertThat(full).hasSize(6);
    assertThat(proof.graph()).hasSize(4);
    // Independent sparse finite-horizon Bellman oracle, including all available actions.
    var fullValues = initial(full);
    var reducedValues = initial(proof.graph());
    for (int horizon = 0; horizon < 12; horizon++) {
      for (var state : full.keySet()) {
        var block = proof.blocks().get(state);
        assertThat(fullValues.get(state)).isEqualTo(reducedValues.get(block));
        assertThat(ranking(full.get(state), fullValues))
            .isEqualTo(ranking(proof.graph().get(block), reducedValues));
      }
      fullValues = bellman(full, fullValues);
      reducedValues = bellman(proof.graph(), reducedValues);
    }
    for (var source : full.keySet())
      for (var cycle : List.of(List.of("EXALTED", "ANNULMENT"), List.of("CHAOS"))) {
        var fullLaw = Map.of(source, Fraction.ONE);
        var reducedLaw = Map.of(proof.blocks().get(source), Fraction.ONE);
        for (int n = 1; n <= 500; n++) {
          for (var action : cycle) {
            fullLaw = firstHit(full, fullLaw, action);
            reducedLaw = firstHit(proof.graph(), reducedLaw, action);
          }
          if (Set.of(100, 300, 500).contains(n))
            assertThat(proof.project(fullLaw)).isEqualTo(reducedLaw);
        }
      }
  }

  @Test
  void distinctEntryLawsRemainDistinctThroughACommonBlockAndGoalMerge() {
    var full = closedGraph();
    var proof = new FiniteQuotientProof<>(full);
    var twins =
        full.keySet().stream()
            .filter(
                s ->
                    s.item().explicits().size() == 1
                        && s.item().explicits().getFirst().modifierId().contains("adept-s"))
            .toList();
    assertThat(proof.blocks().get(twins.get(0))).isEqualTo(proof.blocks().get(twins.get(1)));
    var goal =
        proof.graph().entrySet().stream()
            .filter(e -> goal(e.getValue()))
            .findFirst()
            .orElseThrow()
            .getKey();
    var first =
        proof.condition(
            Map.of(twins.get(0), Fraction.of(1, 4), twins.get(1), Fraction.of(3, 4)),
            "EXALTED",
            goal);
    var second =
        proof.condition(
            Map.of(twins.get(0), Fraction.of(3, 4), twins.get(1), Fraction.of(1, 4)),
            "EXALTED",
            goal);
    assertThat(first.mass()).isEqualTo(Fraction.ONE);
    assertThat(second.mass()).isEqualTo(Fraction.ONE);
    assertThat(first.law()).isNotEqualTo(second.law());
    assertThat(proof.project(first.law())).isEqualTo(proof.project(second.law()));
  }

  @Test
  void numericGoalDifferencesAndUnprovedBoundariesCannotBeCollapsed() {
    var full = closedGraph();
    var numeric = new LinkedHashMap<BasicCurrencyState, Node<BasicCurrencyState>>();
    full.forEach(
        (s, n) ->
            numeric.put(
                s,
                new Node<>(
                    List.of(
                        n.observation(),
                        s.item().explicits().stream()
                            .flatMap(m -> m.values().values().stream())
                            .toList()),
                    true,
                    n.actions())));
    assertThat(new FiniteQuotientProof<>(numeric).graph()).hasSize(6);
    assertThatThrownBy(() -> new Node<>("unknown", false, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Action<>("partial", Map.of("x", Fraction.of(1, 2)), Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static boolean goal(Node<?> node) {
    return ((MethodSearchCalculation.Progress) node.observation()).goal()
        == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.MATCH;
  }

  private static <S> Map<S, Fraction> initial(Map<S, Node<S>> graph) {
    var result = new LinkedHashMap<S, Fraction>();
    graph.forEach((s, n) -> result.put(s, goal(n) ? Fraction.ONE : Fraction.ZERO));
    return result;
  }

  private static <S> Map<String, Fraction> ranking(Node<S> node, Map<S, Fraction> values) {
    var result = new TreeMap<String, Fraction>();
    for (var action : node.actions()) {
      var value = Fraction.ZERO;
      for (var outcome : action.outcomes().entrySet())
        value = value.add(outcome.getValue().multiply(values.get(outcome.getKey())));
      result.put(action.label(), value);
    }
    return result;
  }

  private static <S> Map<S, Fraction> bellman(Map<S, Node<S>> graph, Map<S, Fraction> previous) {
    var result = new LinkedHashMap<S, Fraction>();
    graph.forEach(
        (s, n) ->
            result.put(
                s,
                goal(n)
                    ? Fraction.ONE
                    : ranking(n, previous).values().stream()
                        .max(
                            (a, b) ->
                                a.numerator()
                                    .multiply(b.denominator())
                                    .compareTo(b.numerator().multiply(a.denominator())))
                        .orElse(Fraction.ZERO)));
    return result;
  }

  private static <S> Map<S, Fraction> firstHit(
      Map<S, Node<S>> graph, Map<S, Fraction> law, String action) {
    var result = new LinkedHashMap<S, Fraction>();
    law.forEach(
        (s, mass) -> {
          var selected =
              graph.get(s).actions().stream().filter(a -> a.label().equals(action)).findFirst();
          if (goal(graph.get(s)) || selected.isEmpty()) result.merge(s, mass, Fraction::add);
          else
            selected
                .orElseThrow()
                .outcomes()
                .forEach((target, p) -> result.merge(target, mass.multiply(p), Fraction::add));
        });
    return result;
  }
}
