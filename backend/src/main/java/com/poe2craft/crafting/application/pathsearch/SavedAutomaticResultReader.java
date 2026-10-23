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
    boolean jointVersion = AutomaticMethodGraph.VERSION.equals(graph.version());
    boolean qualityVersion =
        AutomaticMethodGraph.VERSION.equals(graph.version())
            || "goal-directed-milestone-graph-v11-tier-laws".equals(graph.version())
            || "goal-directed-milestone-graph-v10-quality-goal".equals(graph.version());
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
          && (!(AutomaticMethodExecution.VERSION.equals(owner.execution().version())
                  || "automatic-method-execution-v5-tier-laws".equals(owner.execution().version())
                  || "automatic-method-execution-v4-prepared-renewal"
                      .equals(owner.execution().version()))
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
          var from = graph.nodes().get(owner.source());
          var to = graph.nodes().get(saved.source());
          var exits =
              new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(Exit.class);
          decoded
              .exits()
              .forEach((kind, law) -> exits.put(kind, transport(from, to, law, runtime.kernel())));
          decoded =
              new CertifiedMethodRunner.Observation<>(
                  decoded.cycles(),
                  exits,
                  transport(from, to, decoded.active(), runtime.kernel()),
                  decoded.initialUses(),
                  decoded.cycleUses(),
                  decoded.mass());
        }
        points.put(Math.toIntExact(point.cycles()), decoded);
        retained += point.outcomes().size();
      }
      var header = owner.execution().header();
      var completed = new HashSet<>(points.keySet());
      if (jointVersion)
        for (int horizon : List.of(1, 100))
          if (!points.containsKey(horizon) && horizon >= header.cycles()) {
            var point = provisional(owner.execution(), horizon);
            if (saved.owner() != null) {
              var from = graph.nodes().get(owner.source());
              var to = graph.nodes().get(saved.source());
              var exits =
                  new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(
                      Exit.class);
              point
                  .exits()
                  .forEach(
                      (kind, law) -> exits.put(kind, transport(from, to, law, runtime.kernel())));
              point =
                  new CertifiedMethodRunner.Observation<>(
                      point.cycles(),
                      exits,
                      transport(from, to, point.active(), runtime.kernel()),
                      point.initialUses(),
                      point.cycleUses(),
                      point.mass());
            }
            points.put(horizon, point);
          }
      boolean single =
          header.cycles() <= 1
              && header.atBoundary()
              && header.boundaryMass().zero()
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
      if (single) completed.add(1);
      views.add(
          new AutomaticMethodGraph.MethodView(
              saved.id(),
              saved.source(),
              template,
              template.cycle().size() == 1
                      && template.cycle().getFirst().currency()
                          == com.poe2craft.crafting.domain.WorkbenchCurrency.CHAOS
                  ? "REPEATABLE"
                  : single
                      ? "SINGLE_PASS"
                      : header.cycles() > 1
                              || (jointVersion
                                  && completed.contains(1)
                                  && !points.get(1).active().isEmpty())
                          ? "REPEATABLE"
                          : "PENDING",
              points,
              owner.execution().unresolvedReasons(),
              completed,
              qualityVersion && template.boundedChaos()
                  ? points.values().stream()
                      .flatMap(point -> point.active().keySet().stream())
                      .map(AutomaticMethodExecution.State::item)
                      .collect(java.util.stream.Collectors.toSet())
                  : Set.of(),
              owner.finished()));
    }
    var nodes = new ArrayList<AutomaticMethodGraph.NodeView>();
    var attributes =
        jointVersion
            ? AutomaticMethodGraph.savedAttributeSupports(graph.nodes(), runtime.kernel())
            : Map.<Integer, Map<String, List<AutomaticMethodGraph.AttributeOption>>>of();
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
              AutomaticMethodGraph.tierView(saved.tiers()),
              runtime.kernel().rollBands(saved.item()),
              saved.quality40(),
              quality,
              progress,
              saved.discovered(),
              saved.discovered()
                  && views.stream()
                      .filter(method -> method.source() == source)
                      .allMatch(AutomaticMethodGraph.MethodView::finished),
              jointVersion
                  ? saved.members().stream()
                      .map(
                          m ->
                              new AutomaticMethodGraph.MemberView(
                                  m.item(),
                                  AutomaticMethodGraph.tierView(m.tiers()),
                                  runtime.kernel().rollBands(m.item())))
                      .toList()
                  : List.of(),
              attributes.getOrDefault(i, Map.of()),
              jointVersion ? saved.joint() : Set.of()));
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
        List.of(),
        graph.executions().stream()
            .filter(e -> e.execution() != null)
            .flatMap(e -> e.execution().assumptions().stream())
            .map(com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption::id)
            .filter(com.poe2craft.crafting.domain.CoupledStatRollModel.ID::equals)
            .distinct()
            .sorted()
            .toList());
  }

  /** Decode saved accumulators only. Neither selector nor transition may ever execute. */
  private static CertifiedMethodRunner.Observation<
          AutomaticMethodExecution.State, com.poe2craft.crafting.domain.AutomaticCraftingAction>
      provisional(AutomaticMethodExecution.Checkpoint saved, int horizon) {
    var initial =
        saved.template().initial().stream()
            .<java.util.function.Function<
                    AutomaticMethodExecution.State,
                    com.poe2craft.crafting.domain.AutomaticCraftingAction>>
                map(a -> s -> a)
            .toList();
    var cycle =
        saved.template().cycle().stream()
            .<java.util.function.Function<
                    AutomaticMethodExecution.State,
                    com.poe2craft.crafting.domain.AutomaticCraftingAction>>
                map(a -> s -> a)
            .toList();
    var template = new ExactMethodRunner.Template<>(saved.template().id(), initial, cycle);
    try (var restore =
        CertifiedMethodRunner.restoreCursor(
            saved.header(),
            template,
            (state, action) -> {
              throw new IllegalStateException("Historical transitions cannot execute");
            },
            state -> {
              throw new IllegalStateException("Historical classifiers cannot execute");
            },
            saved.precision(),
            LinkedHashMap::new)) {
      saved.rows().forEach(restore::accept);
      var runner = restore.finish();
      if (runner.atBoundary() && runner.absorbed())
        return runner.observation().atLaterAbsorbedBoundary(horizon);
      if (runner.atBoundary() && runner.cycles() == horizon) return runner.observation();
      var point =
          runner.provisional(
              horizon,
              new AutomaticMethodExecution.State(
                  saved.root(),
                  false,
                  saved.initialQuality40(),
                  saved.initialAbstractQuality(),
                  saved.initialTiers(),
                  saved.symbolicSource()
                      ? saved.root().explicits().stream()
                          .map(com.poe2craft.item.ModifierInstance::modifierId)
                          .collect(java.util.stream.Collectors.toSet())
                      : Set.of()));
      return new CertifiedMethodRunner.Observation<>(
          horizon,
          point.exits(),
          point.active(),
          usageBounds(
              point.initialUses(),
              saved.template().initial(),
              1,
              saved.precision().fractionalBits()),
          usageBounds(
              point.cycleUses(),
              saved.template().cycle(),
              horizon,
              saved.precision().fractionalBits()),
          point.mass());
    }
  }

  private static <A> Map<A, CertifiedInterval> usageBounds(
      Map<A, CertifiedInterval> spent, List<A> actions, int repetitions, int bits) {
    var counts = new LinkedHashMap<A, Long>();
    actions.forEach(a -> counts.merge(a, (long) repetitions, Long::sum));
    var result = new LinkedHashMap<A, CertifiedInterval>();
    counts.forEach(
        (action, count) ->
            result.put(
                action,
                new CertifiedInterval(
                    bits,
                    spent.getOrDefault(action, CertifiedInterval.zero(bits)).lowerUnits(),
                    java.math.BigInteger.valueOf(count).shiftLeft(bits))));
    return result;
  }

  private static Map<String, TierCell> transportedTiers(
      AutomaticMethodGraph.NodeCursor from,
      AutomaticMethodGraph.NodeCursor to,
      AutomaticMethodExecution.State state) {
    var tiers = new HashMap<>(state.tiers());
    from.item().explicits().forEach(m -> tiers.remove(m.modifierId()));
    tiers.putAll(to.tiers());
    return Map.copyOf(tiers);
  }

  private static Map<AutomaticMethodExecution.State, CertifiedInterval> transport(
      AutomaticMethodGraph.NodeCursor from,
      AutomaticMethodGraph.NodeCursor to,
      Map<AutomaticMethodExecution.State, CertifiedInterval> law,
      com.poe2craft.crafting.domain.AutomaticCraftingKernel kernel) {
    var result = new LinkedHashMap<AutomaticMethodExecution.State, CertifiedInterval>();
    law.forEach(
        (state, mass) ->
            result.merge(
                new AutomaticMethodExecution.State(
                    TerminalAdditionClasses.transport(from.item(), to.item(), state.item()),
                    state.temporaryPreparation(),
                    state.quality40(),
                    state.abstractQuality(),
                    transportedTiers(from, to, state),
                    transportedRetention(from, to, state, kernel)),
                mass,
                CertifiedInterval::add));
    return result;
  }

  private static Set<String> transportedRetention(
      AutomaticMethodGraph.NodeCursor from,
      AutomaticMethodGraph.NodeCursor to,
      AutomaticMethodExecution.State state,
      com.poe2craft.crafting.domain.AutomaticCraftingKernel kernel) {
    var result = new HashSet<String>();
    for (var id : state.retainedFromRoot()) {
      var before =
          from.item().explicits().stream()
              .filter(m -> m.modifierId().equals(id))
              .findFirst()
              .orElseThrow();
      to.item().explicits().stream()
          .filter(m -> kernel.attributeKey(m).equals(kernel.attributeKey(before)))
          .findFirst()
          .ifPresent(m -> result.add(m.modifierId()));
    }
    return Set.copyOf(result);
  }
}
