package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.pathsearch.CertifiedOutcomeStrategy.*;
import java.util.*;

/** Closed communicating regions with positive-lower-bound navigation witnesses. */
final class CertifiedEndComponents<S> {
  record Component<S>(Set<S> members, Map<S, List<Choice<S>>> internal) {}

  private final Map<S, State<S>> graph;

  CertifiedEndComponents(Map<S, State<S>> graph) {
    this.graph = graph;
  }

  List<Component<S>> find() {
    var candidates = new ArrayDeque<Set<S>>();
    var initial = new LinkedHashSet<S>();
    graph.forEach(
        (id, state) -> {
          if (!state.goal() && !state.open()) initial.add(id);
        });
    candidates.add(initial);
    var result = new ArrayList<Component<S>>();
    while (!candidates.isEmpty()) {
      var region = candidates.removeFirst();
      Map<S, List<Choice<S>>> internal;
      while (true) {
        internal = new LinkedHashMap<>();
        for (var id : region) {
          var closed = graph.get(id).choices().stream().filter(c -> internal(c, region)).toList();
          if (!closed.isEmpty()) internal.put(id, closed);
        }
        if (internal.size() == region.size()) break;
        region.retainAll(internal.keySet());
      }
      if (region.isEmpty()) continue;
      var forward = new LinkedHashMap<S, Set<S>>();
      var reverse = new LinkedHashMap<S, Set<S>>();
      region.forEach(
          id -> {
            forward.put(id, new LinkedHashSet<>());
            reverse.put(id, new LinkedHashSet<>());
          });
      internal.forEach(
          (source, choices) ->
              choices.forEach(
                  c ->
                      c.outcomes()
                          .forEach(
                              (target, p) -> {
                                if (p.lowerUnits().signum() > 0) {
                                  forward.get(source).add(target);
                                  reverse.get(target).add(source);
                                }
                              })));
      var components = stronglyConnected(forward, reverse);
      if (components.size() == 1)
        result.add(new Component<>(Set.copyOf(region), Map.copyOf(internal)));
      else candidates.addAll(components);
    }
    return List.copyOf(result);
  }

  static <S> boolean internal(Choice<S> choice, Set<S> region) {
    return choice.unresolved().zero()
        && choice.outcomes().entrySet().stream()
            .allMatch(e -> e.getValue().zero() || region.contains(e.getKey()));
  }

  /** Iterative Kosaraju traversal avoids a Java stack limit on long state chains. */
  private List<Set<S>> stronglyConnected(Map<S, Set<S>> forward, Map<S, Set<S>> reverse) {
    var seen = new HashSet<S>();
    var order = new ArrayList<S>();
    var vertices = new ArrayDeque<S>();
    var iterators = new ArrayDeque<Iterator<S>>();
    for (var root : forward.keySet()) {
      if (!seen.add(root)) continue;
      vertices.push(root);
      iterators.push(forward.get(root).iterator());
      while (!vertices.isEmpty()) {
        var iterator = iterators.peek();
        if (!iterator.hasNext()) {
          order.add(vertices.pop());
          iterators.pop();
          continue;
        }
        var next = iterator.next();
        if (seen.add(next)) {
          vertices.push(next);
          iterators.push(forward.get(next).iterator());
        }
      }
    }
    seen.clear();
    var result = new ArrayList<Set<S>>();
    for (int i = order.size() - 1; i >= 0; i--) {
      var root = order.get(i);
      if (!seen.add(root)) continue;
      var component = new LinkedHashSet<S>();
      vertices.push(root);
      while (!vertices.isEmpty()) {
        var current = vertices.pop();
        component.add(current);
        for (var next : reverse.get(current)) if (seen.add(next)) vertices.push(next);
      }
      result.add(component);
    }
    return result;
  }
}
