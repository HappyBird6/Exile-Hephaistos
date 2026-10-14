package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Probability;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.pathsearch.*;
import java.util.*;

/** Additive wire values: an arithmetic enclosure is never a statistical confidence interval. */
public final class CertifiedMethodProtocol {
  public static final String VERSION = "certified-method-wire-v7-strategy-job";

  private CertifiedMethodProtocol() {}

  public record Create(
      Integer version,
      MethodSearchProtocol.Create request,
      CertifiedInterval.Precision precision) {}

  public record State(
      String id,
      int depth,
      String goal,
      List<String> entries,
      boolean directComplete,
      com.poe2craft.item.ItemState representative) {}

  public record Snapshot(
      String version,
      String jobId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean enumerationComplete,
      long operations,
      String rootStateId,
      CertifiedInterval.Precision precision,
      String arithmeticVersion,
      String calculationVersion,
      String templateVersion,
      String conditionalVersion,
      com.poe2craft.crafting.domain.BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      List<com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption> assumptions,
      String strategyStatus,
      String strategyVersion,
      int stateCount,
      int methodCount,
      Storage storage,
      List<Local> localRecommendations,
      List<Strategy> strategies) {}

  public record Strategy(
      String stateId,
      int cycles,
      String status,
      Probability lower,
      Probability upper,
      List<String> methodIds,
      boolean reachableOpen,
      boolean reachableUnresolved,
      boolean transitionArithmetic,
      boolean fixedPointPending) {}

  public record Storage(
      String version,
      long residentEntries,
      long storedClassRows,
      long packedBytes,
      long residentPackedByteLimit,
      long transientFreezeBytes,
      long kernelPackedBytes,
      long kernelPackedByteLimit,
      int materializedRows,
      int uniquePages,
      String workingVersion,
      long workingClassRows,
      long workingPackedBytes,
      int workingMaterializedRows,
      long residentPackedBytes,
      long diskBytes) {}

  public record Graph(
      String jobId,
      long revision,
      List<State> states,
      List<Method> methods,
      List<Local> localRecommendations,
      Integer nextStateOffset,
      Integer nextMethodOffset) {}

  public record Outcomes(
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<Outcome> outcomes,
      Integer nextOffset) {}

  public record Entry(
      String id,
      String stateId,
      String parentEntryId,
      String status,
      String reason,
      Bounds conditionalProbability,
      long generation,
      List<MethodSearchProtocol.ConditionalState> states,
      Integer nextOffset) {}

  public record Started(Snapshot search, Entry rootEntry) {}

  public record Bounds(
      Probability lower,
      Probability upper,
      Probability arithmeticWidth,
      boolean exact,
      String confirmedPercent) {
    public static Bounds probability(CertifiedInterval value, int decimals) {
      var probability = value.probability();
      return of(probability, probability.confirmedPercent(decimals).orElse(null));
    }

    public static Bounds quantity(CertifiedInterval value) {
      return of(value, null);
    }

    private static Bounds of(CertifiedInterval value, String confirmed) {
      var width =
          new com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction(
              value.upperUnits().subtract(value.lowerUnits()),
              java.math.BigInteger.ONE.shiftLeft(value.fractionalBits()));
      return new Bounds(
          Probability.of(value.lower()),
          Probability.of(value.upper()),
          Probability.of(width),
          value.exact(),
          confirmed);
    }
  }

  public record Outcome(String kind, String stateId, Bounds probability) {}

  public record Point(
      int cycles,
      Map<String, Bounds> exits,
      Bounds active,
      Map<String, Bounds> initialUses,
      Map<String, Bounds> cycleUses,
      List<Checkpoint> checkpoints) {}

  public record Checkpoint(Target target, Bounds probability) {}

  public record Method(
      String id,
      String sourceStateId,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      List<Point> points) {}

  public record Target(String goal, List<String> entries) {}

  /** A null target ranks the whole goal; a target ranks one observed checkpoint pattern. */
  public record Local(
      String stateId, int cycles, Target target, String status, List<String> methodIds) {}

  static Method method(
      CertifiedMethodSearchEngine.MethodView method, CertifiedInterval.Precision precision) {
    var points = new ArrayList<Point>();
    method.points().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              var point = entry.getValue();
              var exits = new LinkedHashMap<String, Bounds>();
              point
                  .exits()
                  .forEach(
                      (kind, law) ->
                          exits.put(
                              kind.name(),
                              Bounds.probability(
                                  sum(law, precision), precision.percentDecimals())));
              points.add(
                  new Point(
                      entry.getKey(),
                      Map.copyOf(exits),
                      Bounds.probability(
                          sum(point.active(), precision), precision.percentDecimals()),
                      quantities(point.initialUses()),
                      quantities(point.cycleUses()),
                      method
                          .checkpoints()
                          .getOrDefault(entry.getKey(), Map.of())
                          .entrySet()
                          .stream()
                          .sorted(Comparator.comparing(e -> e.getKey().toString()))
                          .map(
                              e ->
                                  new Checkpoint(
                                      target(e.getKey()),
                                      Bounds.probability(
                                          e.getValue(), precision.percentDecimals())))
                          .toList()));
            });
    return new Method(
        method.id(),
        method.source(),
        method.initialActions().stream().map(Enum::name).toList(),
        method.actions().stream().map(Enum::name).toList(),
        method.status(),
        List.copyOf(points));
  }

  static List<Local> locals(
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods,
      CertifiedInterval.Precision precision) {
    var bySource = new HashMap<String, List<CertifiedMethodSearchEngine.MethodView>>();
    methods.forEach(m -> bySource.computeIfAbsent(m.source(), ignored -> new ArrayList<>()).add(m));
    var result = new ArrayList<Local>();
    for (var state : states) {
      var direct = bySource.getOrDefault(state.id(), List.of());
      for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
        boolean complete = state.expanded();
        var choices = new ArrayList<CertifiedRecommendations.Choice>();
        for (var method : direct) {
          var point = method.points().get(cycles);
          if (point == null) {
            complete = false;
            continue;
          }
          choices.add(
              new CertifiedRecommendations.Choice(
                  method.id(),
                  sum(point.exits().get(ExactMethodRunner.Exit.GOAL), precision),
                  sum(point.exits().get(ExactMethodRunner.Exit.UNRESOLVED), precision)));
        }
        var ranked = CertifiedRecommendations.local(choices, complete);
        result.add(new Local(state.id(), cycles, null, ranked.status(), ranked.methodIds()));
        var targets = new LinkedHashSet<MethodSearchCalculation.Progress>();
        for (var method : direct)
          targets.addAll(method.checkpoints().getOrDefault(cycles, Map.of()).keySet());
        targets.remove(state.progress());
        targets.removeIf(
            target ->
                unresolved(target.goal())
                    || target.entries().stream().anyMatch(CertifiedMethodProtocol::unresolved));
        for (var target : targets) {
          var targetChoices = new ArrayList<CertifiedRecommendations.Choice>();
          for (var method : direct) {
            var point = method.points().get(cycles);
            if (point == null) continue;
            targetChoices.add(
                new CertifiedRecommendations.Choice(
                    method.id(),
                    method
                        .checkpoints()
                        .getOrDefault(cycles, Map.of())
                        .getOrDefault(target, CertifiedInterval.zero(precision.fractionalBits())),
                    sum(point.exits().get(ExactMethodRunner.Exit.UNRESOLVED), precision)));
          }
          var targetRanked = CertifiedRecommendations.local(targetChoices, complete);
          result.add(
              new Local(
                  state.id(),
                  cycles,
                  target(target),
                  targetRanked.status(),
                  targetRanked.methodIds()));
        }
      }
    }
    return List.copyOf(result);
  }

  private static boolean unresolved(
      com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status status) {
    return status == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.UNKNOWN
        || status == com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status.UNSUPPORTED;
  }

  private static Target target(MethodSearchCalculation.Progress progress) {
    return new Target(progress.goal().name(), progress.entries().stream().map(Enum::name).toList());
  }

  private static CertifiedInterval sum(
      Map<?, CertifiedInterval> values, CertifiedInterval.Precision precision) {
    return CertifiedLawStore.total(values, precision.fractionalBits());
  }

  private static Map<String, Bounds> quantities(Map<WorkbenchCurrency, CertifiedInterval> values) {
    var result = new LinkedHashMap<String, Bounds>();
    values.forEach((action, value) -> result.put(action.name(), Bounds.quantity(value)));
    return Map.copyOf(result);
  }
}
