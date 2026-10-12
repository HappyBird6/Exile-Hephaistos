package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy.*;
import java.util.*;

/** Qualitative reachability with a proper policy witness, without enumerating item histories. */
final class AlmostSureReachability<S> {
  record Solution<S>(Set<S> winning, Map<S, String> policy) {}

  private record Arc<S>(S source, String method) {}

  private final Map<S, State<S>> graph;
  private Set<S> candidate;
  private Set<S> reachable;
  private Map<S, List<Arc<S>>> reverse;
  private Map<S, String> policy;
  private ArrayDeque<S> queue;
  private Iterator<Map.Entry<S, State<S>>> states;
  private Iterator<Choice<S>> choices = Collections.emptyIterator();
  private Iterator<Map.Entry<S, Fraction>> targets = Collections.emptyIterator();
  private Iterator<Arc<S>> predecessors = Collections.emptyIterator();
  private S source;
  private Choice<S> choice;
  private boolean safe, adding, traversing, unsupported;
  private Solution<S> solution;

  AlmostSureReachability(Map<S, State<S>> graph) {
    this.graph = graph;
    candidate = new HashSet<>(graph.keySet());
    beginPass();
  }

  /** At most 128 state/choice/support-edge operations; no numeric convergence tolerance. */
  Optional<Solution<S>> step() {
    for (int n = 0; n < 128 && solution == null && !unsupported; n++) advance();
    return Optional.ofNullable(solution);
  }

  private void beginPass() {
    reachable = new HashSet<>();
    reverse = new HashMap<>();
    policy = new HashMap<>();
    queue = new ArrayDeque<>();
    states = graph.entrySet().iterator();
    choices = Collections.emptyIterator();
    targets = Collections.emptyIterator();
    predecessors = Collections.emptyIterator();
    choice = null;
    traversing = false;
  }

  private void advance() {
    if (traversing) {
      if (predecessors.hasNext()) {
        var arc = predecessors.next();
        if (reachable.add(arc.source())) {
          queue.add(arc.source());
          policy.put(arc.source(), arc.method());
        }
      } else if (!queue.isEmpty()) {
        predecessors = reverse.getOrDefault(queue.remove(), List.of()).iterator();
      } else if (reachable.equals(candidate)) {
        // Each witness stays inside this set and has a positive edge to a smaller BFS rank.
        // A finite closed non-goal recurrent class is therefore impossible under this policy.
        solution = new Solution<>(Set.copyOf(reachable), Map.copyOf(policy));
      } else {
        candidate = reachable;
        beginPass();
      }
      return;
    }
    if (targets.hasNext()) {
      var target = targets.next();
      if (!target.getValue().equals(Fraction.ZERO)) {
        if (adding)
          reverse
              .computeIfAbsent(target.getKey(), ignored -> new ArrayList<>())
              .add(new Arc<>(source, choice.methodId()));
        else if (!candidate.contains(target.getKey())) safe = false;
      }
      return;
    }
    if (choice != null) {
      if (!adding && safe) {
        adding = true;
        targets = choice.outcomes().entrySet().iterator();
      } else choice = null;
      return;
    }
    if (choices.hasNext()) {
      choice = choices.next();
      if (!choice.unresolved().equals(Fraction.ZERO)) {
        unsupported = true;
        return;
      }
      safe = candidate.contains(source);
      adding = false;
      targets = choice.outcomes().entrySet().iterator();
      return;
    }
    if (states.hasNext()) {
      var state = states.next();
      if (!state.getValue().expansionComplete()) {
        unsupported = true;
        return;
      }
      source = state.getKey();
      if (state.getValue().goal()) {
        reachable.add(source);
        queue.add(source);
      }
      choices = state.getValue().choices().iterator();
      return;
    }
    traversing = true;
  }
}
