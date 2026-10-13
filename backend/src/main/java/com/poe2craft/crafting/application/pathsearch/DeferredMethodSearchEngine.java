package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.DeferredItem;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;

/** Adapter preserves the existing deferred-cell calculation and its exact distribution pages. */
final class DeferredMethodSearchEngine implements MethodSearchEngine {
  private final MethodSearchCalculation calculation;
  private final String rootId;
  private final Map<DeferredItem, String> ids = new HashMap<>();
  private final Map<String, DeferredItem> identities = new HashMap<>();
  private final IdentityHashMap<
          Observation<DeferredItem, WorkbenchCurrency>, Observation<String, WorkbenchCurrency>>
      points = new IdentityHashMap<>();
  private long snapshotOperation = Long.MIN_VALUE;
  private List<StateView> states = List.of();
  private List<MethodView> methods = List.of();

  DeferredMethodSearchEngine(MethodSearchCalculation calculation) {
    this.calculation = calculation;
    rootId = calculation.states().getFirst().id();
  }

  public void step() {
    calculation.step();
  }

  public boolean complete() {
    return calculation.complete();
  }

  public boolean memoryLimited() {
    return calculation.memoryLimited();
  }

  public void increaseMemoryBudget(int limit) {
    calculation.increaseMemoryBudget(limit);
  }

  public long operations() {
    return calculation.operations();
  }

  public String rootId() {
    return rootId;
  }

  public String calculationVersion() {
    return MethodSearchCalculation.VERSION;
  }

  public String stateVersion() {
    return DeferredItem.VERSION;
  }

  public String templatesVersion() {
    return MethodSearchCalculation.TEMPLATES;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return calculation.assumptions();
  }

  public List<StateView> states() {
    refresh();
    return states;
  }

  public List<MethodView> methods() {
    refresh();
    return methods;
  }

  private String id(DeferredItem item) {
    return ids.computeIfAbsent(
        item,
        value -> {
          var key = value.canonicalKey();
          var previous = identities.putIfAbsent(key, value);
          if (previous != null && !previous.equals(value))
            throw new IllegalStateException("State digest collision");
          return key;
        });
  }

  private Map<String, Fraction> law(Map<DeferredItem, Fraction> input) {
    var result = new LinkedHashMap<String, Fraction>();
    input.forEach((state, mass) -> result.merge(id(state), mass, Fraction::add));
    return Collections.unmodifiableMap(result);
  }

  private Observation<String, WorkbenchCurrency> point(
      Observation<DeferredItem, WorkbenchCurrency> input) {
    return points.computeIfAbsent(
        input,
        value -> {
          var exits = new LinkedHashMap<Exit, Map<String, Fraction>>();
          value.exits().forEach((exit, distribution) -> exits.put(exit, law(distribution)));
          return new Observation<>(
              value.cycles(),
              Collections.unmodifiableMap(exits),
              law(value.active()),
              value.initialUses(),
              value.cycleUses());
        });
  }

  private void refresh() {
    if (snapshotOperation == calculation.operations()) return;
    states =
        calculation.states().stream()
            .map(
                state -> {
                  if (!id(state.state()).equals(state.id()))
                    throw new IllegalStateException("Inconsistent state identity");
                  return StateView.cell(state);
                })
            .toList();
    methods =
        calculation.methods().stream()
            .map(
                method -> {
                  var observations =
                      new LinkedHashMap<Integer, Observation<String, WorkbenchCurrency>>();
                  method
                      .points()
                      .forEach((cycles, value) -> observations.put(cycles, point(value)));
                  return new MethodView(
                      method.id(),
                      method.source(),
                      method.initialActions(),
                      method.actions(),
                      method.status(),
                      method.reason(),
                      Collections.unmodifiableMap(observations));
                })
            .toList();
    snapshotOperation = calculation.operations();
  }
}
