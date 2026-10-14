package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.pathsearch.CertifiedOutcomeStrategy;
import java.util.*;
import java.util.function.IntFunction;

/** Resumable horizon sequencing for the explicitly bounded finite strategy adapter. */
final class CertifiedStrategyWork {
  record Row(String stateId, int cycles, CertifiedOutcomeStrategy.Result result) {}

  private final List<String> states;
  private final IntFunction<CertifiedOutcomeStrategy<String>> factory;
  private final int decimals;
  private final List<Row> completed = new ArrayList<>();
  private List<Row> current = List.of();
  private CertifiedOutcomeStrategy<String> solver;
  private int horizon;
  private long revision;
  private String pauseReason;

  CertifiedStrategyWork(
      List<String> states, int decimals, IntFunction<CertifiedOutcomeStrategy<String>> factory) {
    this.states = List.copyOf(states);
    this.decimals = decimals;
    this.factory = factory;
  }

  long revision() {
    return revision;
  }

  boolean complete() {
    return horizon == MethodSearchCalculation.OBSERVATIONS.size();
  }

  String pauseReason() {
    return pauseReason;
  }

  String status() {
    return complete() ? "COMPLETE" : pauseReason != null ? "PAUSED" : "RUNNING";
  }

  List<Row> rows() {
    var result = new ArrayList<>(completed);
    result.addAll(current);
    return List.copyOf(result);
  }

  void step() {
    if (complete() || pauseReason != null) return;
    int cycles = MethodSearchCalculation.OBSERVATIONS.get(horizon);
    if (solver == null) {
      solver = factory.apply(cycles);
      revision++;
      return;
    }
    solver.refine();
    var next = states.stream().map(id -> new Row(id, cycles, solver.result(id))).toList();
    boolean certified =
        next.stream()
            .allMatch(
                row -> {
                  var result = row.result();
                  return result.value().confirmedPercent(decimals).isPresent()
                      && (!result.methodIds().isEmpty()
                          || Set.of("GOAL", "PROVEN_ZERO").contains(result.status()));
                });
    if (certified) {
      completed.addAll(next);
      current = List.of();
      solver = null;
      horizon++;
    } else {
      if (next.equals(current))
        pauseReason =
            next.stream().anyMatch(r -> r.result().reachableUnresolved())
                ? "STRATEGY_UNRESOLVED"
                : "STRATEGY_PRECISION";
      current = next;
    }
    revision++;
  }
}
