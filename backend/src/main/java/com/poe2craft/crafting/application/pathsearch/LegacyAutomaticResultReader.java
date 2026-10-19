package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import java.util.*;

/**
 * Reads immutable v8 observations. Never restores an execution, advances a cursor, or writes a job.
 */
final class LegacyAutomaticResultReader {
  static final String VERSION = "goal-directed-milestone-graph-v8-prepared-renewal";

  static void require(boolean valid) {
    if (!valid) throw new IllegalArgumentException("Invalid legacy result");
  }

  static AutomaticSearchService.View read(
      AutomaticSearchService.Disk disk, AutomaticSearchService.Runtime runtime) {
    var graph = disk.graph();
    require(
        AutomaticSearchService.VERSION.equals(disk.version()) && VERSION.equals(graph.version()));
    require(
        graph.identity().equals(runtime.kernel().identity())
            && graph.objective().equals(runtime.objective()));
    require(
        disk.revision() > 0
            && disk.generation() > 0
            && graph.operations() >= 0
            && graph.revision() >= 0);
    require(Set.of("READY", "PAUSED", "CANCELLED", "PARTIAL", "COMPLETE").contains(disk.status()));
    require(
        graph.states() > 0
            && graph.states() <= 128
            && graph.methods() > 0
            && graph.methods() <= 256
            && graph.retained() > 0
            && graph.retained() <= 50000
            && graph.primitive() > 0
            && graph.primitive() <= 10000);
    require(
        !graph.nodes().isEmpty()
            && graph.nodes().size() <= 50000
            && graph.executions().size() <= 256);
    require(graph.precision().fractionalBits() <= 128);
    require(graph.nodes().getFirst().item().equals(disk.request().start().item()));
    var methods = new ArrayList<AutomaticMethodGraph.MethodView>();
    var ids = new HashSet<String>();
    long records = graph.nodes().size() + graph.executions().size();
    for (var saved : graph.executions()) {
      require(saved.owner() == null && ids.add(saved.id()) && saved.id().length() <= 16384);
      require(saved.source() >= 0 && saved.source() < graph.nodes().size());
      var execution = Objects.requireNonNull(saved.execution());
      var header = Objects.requireNonNull(execution.header());
      require(
          AutomaticMethodExecution.VERSION.equals(execution.version())
              && CertifiedMethodRunner.CURSOR_VERSION.equals(header.version()));
      require(
          execution.objectiveIdentity().equals(graph.objective())
              && (execution.identity() == null || execution.identity().equals(graph.identity())));
      require(
          execution.root().equals(graph.nodes().get(saved.source()).item())
              && execution.initialQuality40() == graph.nodes().get(saved.source()).quality40());
      var template = execution.template();
      require(template.id().equals(saved.id()) && header.templateId().equals(saved.id()));
      require(
          !template.cycle().isEmpty()
              && template.cycle().size() <= 16
              && template.initial().size() <= 16);
      require(
          header.cycles() >= 0
              && header.cycles() <= 500
              && header.bits() == graph.precision().fractionalBits());
      require(
          header.initialPhases() == template.initial().size()
              && header.cyclePhases() == template.cycle().size());
      require(saved.points().size() <= 4 && execution.rows().size() <= 50000);
      var points =
          new LinkedHashMap<
              Integer,
              CertifiedMethodRunner.Observation<
                  AutomaticMethodExecution.State,
                  com.poe2craft.crafting.domain.AutomaticCraftingAction>>();
      for (var point : saved.points()) {
        require(
            List.of(1L, 100L, 300L, 500L).contains(point.cycles())
                && !points.containsKey((int) point.cycles()));
        require(point.outcomes().size() <= 100000 && point.uses().size() <= 256);
        var exits =
            new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(Exit.class);
        for (var kind : Exit.values()) exits.put(kind, new LinkedHashMap<>());
        var active = new LinkedHashMap<AutomaticMethodExecution.State, CertifiedInterval>();
        var total = CertifiedInterval.zero(header.bits());
        for (var mass : point.outcomes()) {
          require(
              mass.state() != null
                  && mass.state().item() != null
                  && mass.probability().fractionalBits() == header.bits());
          require(
              mass.probability()
                      .lowerUnits()
                      .compareTo(java.math.BigInteger.ONE.shiftLeft(header.bits()))
                  <= 0);
          var target = mass.state().item();
          require(
              target.snapshotId().equals(execution.root().snapshotId())
                  && target.baseItemId().equals(execution.root().baseItemId()));
          var law = mass.exit() == null ? active : exits.get(mass.exit());
          require(law.put(mass.state(), mass.probability()) == null);
          total = total.add(mass.probability());
        }
        require(total.contains(Fraction.ONE) && point.mass().contains(Fraction.ONE));
        var initial =
            new LinkedHashMap<
                com.poe2craft.crafting.domain.AutomaticCraftingAction, CertifiedInterval>();
        var cycle =
            new LinkedHashMap<
                com.poe2craft.crafting.domain.AutomaticCraftingAction, CertifiedInterval>();
        for (var use : point.uses()) {
          require(use.action() != null && use.quantity().fractionalBits() == header.bits());
          require((use.initial() ? initial : cycle).put(use.action(), use.quantity()) == null);
        }
        points.put(
            (int) point.cycles(),
            new CertifiedMethodRunner.Observation<>(
                point.cycles(), exits, active, initial, cycle, point.mass()));
        records += point.outcomes().size() + point.uses().size();
      }
      boolean single =
          header.cycles() <= 1
              && header.atBoundary()
              && header.flight() == null
              && !points.isEmpty()
              && points.values().stream().allMatch(p -> p.active().isEmpty());
      if (single && !points.containsKey(1)) {
        var p = points.values().iterator().next();
        points.put(
            1,
            new CertifiedMethodRunner.Observation<>(
                1, p.exits(), p.active(), p.initialUses(), p.cycleUses(), p.mass()));
      }
      var mode = single ? "SINGLE_PASS" : header.cycles() > 1 ? "REPEATABLE" : "PENDING";
      // v8 did not persist its computed retry-anchor metadata. Keep its exact saved mass;
      // do not invent a retry classification or re-run preparation to recover it.
      methods.add(
          new AutomaticMethodGraph.MethodView(
              saved.id(),
              saved.source(),
              template,
              mode,
              points,
              execution.unresolvedReasons(),
              Set.copyOf(points.keySet()),
              Set.of(),
              saved.finished()));
    }
    require(ids.containsAll(graph.ready()));
    boolean complete = disk.status().equals("COMPLETE");
    if (complete)
      require(
          !graph.limited()
              && !graph.discoveryLimited()
              && graph.ready().isEmpty()
              && graph.nodes().stream().allMatch(AutomaticMethodGraph.NodeCursor::discovered)
              && graph.executions().stream().allMatch(m -> m.finished() && !m.points().isEmpty()));
    var nodes = new ArrayList<AutomaticMethodGraph.NodeView>();
    var identities = new HashSet<AutomaticMethodGraph.Identity>();
    for (int id = 0; id < graph.nodes().size(); id++) {
      var saved = graph.nodes().get(id);
      require(
          saved.depth() >= 0
              && saved.scan() >= 0
              && saved.pairs() >= 0
              && saved.item().explicits().size() <= 4
              && identities.add(
                  new AutomaticMethodGraph.Identity(saved.item(), saved.quality40())));
      require(
          saved.item().snapshotId().equals(disk.request().start().item().snapshotId())
              && saved.item().baseItemId().equals(disk.request().start().item().baseItemId()));
      var raw = runtime.evaluate().apply(saved.item());
      var progress =
          !saved.quality40()
                  && com.poe2craft.crafting.domain.AutomaticCraftingKernel.hasQualityCap(
                      saved.item())
                  && raw.goal() == Status.MATCH
              ? new AutomaticMethodGraph.Progress(Status.NO_MATCH, raw.entries())
              : raw;
      final int source = id;
      nodes.add(
          new AutomaticMethodGraph.NodeView(
              id,
              saved.depth(),
              saved.item(),
              runtime.kernel().rollBands(saved.item()),
              saved.quality40(),
              progress,
              saved.discovered(),
              saved.discovered()
                  && methods.stream()
                      .filter(m -> m.source() == source)
                      .allMatch(AutomaticMethodGraph.MethodView::finished)));
    }
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
        records,
        null,
        List.copyOf(nodes),
        AutomaticSearchService.arrows(nodes, methods),
        List.of());
  }
}
