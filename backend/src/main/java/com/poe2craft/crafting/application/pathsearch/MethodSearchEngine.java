package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;

/** Immutable graph views use state identities, independently of the probability representation. */
interface MethodSearchEngine {
  record StateView(
      String id,
      DeferredItem representative,
      int depth,
      MethodSearchCalculation.Progress progress,
      boolean expanded,
      boolean complete,
      Optional<DeferredItem> distributionCell) {
    static StateView cell(MethodSearchCalculation.StateView state) {
      return new StateView(
          state.id(),
          state.state(),
          state.depth(),
          state.progress(),
          state.expanded(),
          state.complete(),
          Optional.of(state.state()));
    }
  }

  record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, Observation<String, WorkbenchCurrency>> points) {}

  void step();

  boolean complete();

  boolean memoryLimited();

  void increaseMemoryBudget(int limit);

  long operations();

  String rootId();

  String calculationVersion();

  String stateVersion();

  String templatesVersion();

  List<WorkbenchSimulator.Assumption> assumptions();

  List<StateView> states();

  List<MethodView> methods();

  default OutcomeStrategy<String> strategy(int cycles) {
    var states = states();
    var bySource = new HashMap<String, List<MethodView>>();
    methods()
        .forEach(m -> bySource.computeIfAbsent(m.source(), ignored -> new ArrayList<>()).add(m));
    var graph = new LinkedHashMap<String, OutcomeStrategy.State<String>>();
    for (var state : states) {
      var choices = new ArrayList<OutcomeStrategy.Choice<String>>();
      boolean closed = state.expanded();
      for (var method : bySource.getOrDefault(state.id(), List.of())) {
        var point = method.points().get(cycles);
        if (point == null) {
          closed = false;
          continue;
        }
        var outcomes = new LinkedHashMap<>(point.active());
        var unresolved = Fraction.ZERO;
        for (var exit : point.exits().entrySet())
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
          else
            exit.getValue().forEach((target, mass) -> outcomes.merge(target, mass, Fraction::add));
        choices.add(new OutcomeStrategy.Choice<>(method.id(), outcomes, unresolved));
      }
      graph.put(
          state.id(),
          new OutcomeStrategy.State<>(state.progress().goal() == Status.MATCH, closed, choices));
    }
    return new OutcomeStrategy<>(graph);
  }
}
