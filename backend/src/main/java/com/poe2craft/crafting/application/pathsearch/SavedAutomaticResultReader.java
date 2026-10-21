package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import java.util.*;

/** Historical observations are immutable; never restore a cursor under a new policy. */
final class SavedAutomaticResultReader {
  static AutomaticSearchService.View read(
      AutomaticSearchService.Disk disk, AutomaticSearchService.Runtime runtime) {
    var graph = Objects.requireNonNull(disk.graph());
    boolean qualityVersion = AutomaticMethodGraph.VERSION.equals(graph.version());
    if (!graph.identity().catalogDigest().equals(runtime.kernel().identity().catalogDigest())
        || !graph.identity().model().equals(runtime.kernel().identity().model())
        || !LegacyAutomaticResultReader.sameGoal(graph.objective(), runtime.objective()))
      throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
    if (!AutomaticSearchService.VERSION.equals(disk.version())
        || graph.nodes().isEmpty()
        || graph.nodes().size() > 50000
        || graph.executions().size() > 50000)
      throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
    var views = new ArrayList<AutomaticMethodGraph.MethodView>();
    var byId = new LinkedHashMap<String, AutomaticMethodGraph.MethodCursor>();
    long retained = graph.nodes().size() + graph.executions().size();
    for (var saved : graph.executions()) {
      if (saved.source() < 0
          || saved.source() >= graph.nodes().size()
          || byId.putIfAbsent(saved.id(), saved) != null)
        throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
      var owner = saved.owner() == null ? saved : Objects.requireNonNull(byId.get(saved.owner()));
      var template = Objects.requireNonNull(owner.execution()).template();
      if (qualityVersion
          && (!AutomaticMethodExecution.VERSION.equals(owner.execution().version())
              || !CertifiedMethodRunner.CURSOR_VERSION.equals(
                  owner.execution().header().version())))
        throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
      var points =
          new LinkedHashMap<
              Integer,
              CertifiedMethodRunner.Observation<
                  AutomaticMethodExecution.State,
                  com.poe2craft.crafting.domain.AutomaticCraftingAction>>();
      for (var point : owner.points()) {
        if (!List.of(1L, 100L, 300L, 500L).contains(point.cycles())
            || point.outcomes().size() > 100000)
          throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
        var decoded = AutomaticMethodGraph.decode(point);
        if (!decoded
            .mass()
            .contains(
                com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction.ONE))
          throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
        if (saved.owner() != null) {
          var from = graph.nodes().get(owner.source()).item();
          var to = graph.nodes().get(saved.source()).item();
          var exits =
              new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(Exit.class);
          decoded.exits().forEach((kind, law) -> exits.put(kind, transport(from, to, law)));
          decoded =
              new CertifiedMethodRunner.Observation<>(
                  decoded.cycles(),
                  exits,
                  transport(from, to, decoded.active()),
                  decoded.initialUses(),
                  decoded.cycleUses(),
                  decoded.mass());
        }
        points.put(Math.toIntExact(point.cycles()), decoded);
        retained += point.outcomes().size();
      }
      var header = owner.execution().header();
      boolean single =
          header.cycles() <= 1
              && header.atBoundary()
              && header.flight() == null
              && !points.isEmpty()
              && points.values().stream().allMatch(point -> point.active().isEmpty());
      if (single && !points.containsKey(1)) {
        var first = points.values().iterator().next();
        points.put(
            1,
            new CertifiedMethodRunner.Observation<>(
                1,
                first.exits(),
                first.active(),
                first.initialUses(),
                first.cycleUses(),
                first.mass()));
      }
      views.add(
          new AutomaticMethodGraph.MethodView(
              saved.id(),
              saved.source(),
              template,
              single ? "SINGLE_PASS" : header.cycles() > 1 ? "REPEATABLE" : "PENDING",
              points,
              owner.execution().unresolvedReasons(),
              points.keySet(),
              qualityVersion && template.boundedChaos()
                  ? points.values().stream()
                      .flatMap(point -> point.active().keySet().stream())
                      .map(AutomaticMethodExecution.State::item)
                      .collect(java.util.stream.Collectors.toSet())
                  : Set.of(),
              owner.finished()));
    }
    var nodes = new ArrayList<AutomaticMethodGraph.NodeView>();
    for (int i = 0; i < graph.nodes().size(); i++) {
      var saved = graph.nodes().get(i);
      int quality = saved.quality40() ? 40 : saved.abstractQuality();
      if (quality < 0 || quality > 40)
        throw new PathSearchProtocol.Rejected(409, "RECALCULATION_REQUIRED");
      var raw = runtime.evaluate().apply(saved.item());
      if (qualityVersion)
        raw = AutomaticMethodGraph.qualityProgress(runtime.kernel(), saved.item(), quality, raw);
      var progress =
          (qualityVersion
                      ? runtime.kernel().workedQuality(saved.item(), quality) < 40
                      : !saved.quality40())
                  && com.poe2craft.crafting.domain.AutomaticCraftingKernel.hasQualityCap(
                      saved.item())
                  && raw.goal() == Status.MATCH
              ? new AutomaticMethodGraph.Progress(Status.NO_MATCH, raw.entries())
              : raw;
      final int source = i;
      nodes.add(
          new AutomaticMethodGraph.NodeView(
              i,
              saved.depth(),
              saved.item(),
              runtime.kernel().rollBands(saved.item()),
              saved.quality40(),
              quality,
              progress,
              saved.discovered(),
              saved.discovered()
                  && views.stream()
                      .filter(method -> method.source() == source)
                      .allMatch(AutomaticMethodGraph.MethodView::finished)));
    }
    boolean complete = disk.status().equals("COMPLETE");
    return new AutomaticSearchService.View(
        AutomaticSearchService.VERSION,
        disk.id(),
        disk.revision(),
        disk.generation(),
        complete ? "COMPLETE" : "PARTIAL",
        "LEGACY_READ_ONLY",
        complete,
        false,
        graph.operations(),
        retained,
        null,
        List.copyOf(nodes),
        AutomaticSearchService.arrows(nodes, views),
        List.of());
  }

  private static Map<AutomaticMethodExecution.State, CertifiedInterval> transport(
      com.poe2craft.item.ItemState from,
      com.poe2craft.item.ItemState to,
      Map<AutomaticMethodExecution.State, CertifiedInterval> law) {
    var result = new LinkedHashMap<AutomaticMethodExecution.State, CertifiedInterval>();
    law.forEach(
        (state, mass) ->
            result.merge(
                new AutomaticMethodExecution.State(
                    TerminalAdditionClasses.transport(from, to, state.item()),
                    state.temporaryPreparation(),
                    state.quality40(),
                    state.abstractQuality()),
                mass,
                CertifiedInterval::add));
    return result;
  }
}
