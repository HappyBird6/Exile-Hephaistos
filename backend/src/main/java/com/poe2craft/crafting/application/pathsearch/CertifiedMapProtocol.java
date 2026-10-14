package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.MethodSearchService.*;

import com.poe2craft.crafting.application.pathsearch.CertifiedMethodProtocol.Bounds;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.item.ItemState;
import java.util.*;

/** Display grouping only: source class identities and alternative methods remain independent. */
public final class CertifiedMapProtocol {
  public static final String VERSION = "certified-bucket-map-v1";

  private CertifiedMapProtocol() {}

  public record Exit(String bucketId, String kind, Bounds probability) {}

  public record Point(
      int cycles,
      List<Exit> exits,
      Bounds unresolved,
      Map<String, Bounds> initialUses,
      Map<String, Bounds> cycleUses) {}

  public record Arrow(
      String id,
      String sourceStateId,
      String sourceBucketId,
      ItemState sourceItem,
      Map<String, DeferredItem.Band> sourceRolls,
      Map<String, List<MethodSearchProtocol.TierVariant>> sourceTiers,
      Map<String, MethodSearchProtocol.OrbitView> sourceOrbits,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      List<Point> points) {}

  public record Graph(
      String version,
      String jobId,
      long revision,
      List<MethodSearchProtocol.Bucket> buckets,
      List<Arrow> methods,
      List<CertifiedMethodProtocol.Local> localRecommendations,
      int methodCount,
      Integer nextOffset) {}

  public record Header(
      String version,
      String jobId,
      String clientRequestId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean resumable,
      boolean enumerationComplete,
      String rootStateId,
      String strategyStatus,
      List<CertifiedMethodProtocol.Local> localRecommendations,
      List<MethodSearchProtocol.Strategy> strategies) {}

  public record View(Header search, Graph graph) {}

  public record ClassItem(
      String stateId,
      ItemState item,
      Map<String, DeferredItem.Band> rolls,
      Map<String, List<MethodSearchProtocol.TierVariant>> tiers,
      Map<String, MethodSearchProtocol.OrbitView> orbits,
      String kind,
      Bounds probability) {}

  public record Distribution(
      String version,
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<ClassItem> states,
      Integer nextOffset) {}

  private static final class Group {
    MethodSearchEngine.StateView first;
    int size, depth = Integer.MAX_VALUE;
    boolean complete = true;

    void add(MethodSearchEngine.StateView state) {
      if (first == null || state.depth() < depth) first = state;
      depth = Math.min(depth, state.depth());
      size++;
      complete &= state.complete();
    }

    MethodSearchProtocol.Bucket view(String id) {
      var item = first.representative();
      return new MethodSearchProtocol.Bucket(
          id,
          depth,
          first.progress().goal().name(),
          first.progress().entries().stream().map(Enum::name).toList(),
          size,
          item.item(),
          item.rolls(),
          tierView(item),
          orbitView(item),
          complete);
    }
  }

  static Graph graph(
      String jobId,
      long revision,
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods,
      int offset,
      CertifiedInterval.Precision precision) {
    if (offset < 0 || offset > methods.size())
      throw new PathSearchProtocol.Rejected(422, "INVALID_OFFSET");
    int end = Math.min(offset + 12, methods.size());
    var selected = methods.subList(offset, end);
    var sourceIds = new HashSet<String>();
    selected.forEach(m -> sourceIds.add(m.source()));
    var sources = new LinkedHashMap<String, MethodSearchEngine.StateView>();
    var groups = new LinkedHashMap<String, Group>();
    for (var state : states) {
      groups.computeIfAbsent(bucket(state.progress()), ignored -> new Group()).add(state);
      if (sourceIds.contains(state.id())) sources.put(state.id(), state);
    }
    var arrows =
        selected.stream()
            .map(method -> arrow(method, sources.get(method.source()), precision))
            .toList();
    return new Graph(
        VERSION,
        jobId,
        revision,
        groups.entrySet().stream().map(e -> e.getValue().view(e.getKey())).toList(),
        arrows,
        CertifiedMethodProtocol.locals(List.copyOf(sources.values()), methods, precision),
        methods.size(),
        end < methods.size() ? end : null);
  }

  private static Arrow arrow(
      CertifiedMethodSearchEngine.MethodView method,
      MethodSearchEngine.StateView source,
      CertifiedInterval.Precision precision) {
    if (source == null) throw new IllegalStateException("Missing source class");
    var base = CertifiedMethodProtocol.method(method, precision);
    var points = new ArrayList<Point>();
    for (var point : base.points()) {
      var exits = new ArrayList<Exit>();
      method
          .branches()
          .get(point.cycles())
          .forEach(
              (kind, targets) -> {
                if (kind.equals("UNRESOLVED")) return;
                targets.forEach(
                    (target, mass) ->
                        exits.add(
                            new Exit(
                                bucket(target),
                                kind,
                                Bounds.probability(mass, precision.percentDecimals()))));
              });
      exits.sort(Comparator.comparing(Exit::kind).thenComparing(Exit::bucketId));
      points.add(
          new Point(
              point.cycles(),
              List.copyOf(exits),
              point.exits().get("UNRESOLVED"),
              point.initialUses(),
              point.cycleUses()));
    }
    var item = source.representative();
    return new Arrow(
        method.id(),
        method.source(),
        bucket(source.progress()),
        item.item(),
        item.rolls(),
        tierView(item),
        orbitView(item),
        base.initialActions(),
        base.cycleActions(),
        base.status(),
        List.copyOf(points));
  }

  static Distribution distribution(
      CertifiedMethodProtocol.Outcomes outcomes, List<MethodSearchEngine.StateView> states) {
    var needed = new HashSet<String>();
    outcomes.outcomes().forEach(o -> needed.add(o.stateId()));
    var selected = new HashMap<String, DeferredItem>();
    for (var state : states)
      if (needed.contains(state.id())) selected.put(state.id(), state.representative());
    var items =
        outcomes.outcomes().stream()
            .map(
                outcome -> {
                  var item = selected.get(outcome.stateId());
                  if (item == null) throw new IllegalStateException("Missing outcome class");
                  return new ClassItem(
                      outcome.stateId(),
                      item.item(),
                      item.rolls(),
                      tierView(item),
                      orbitView(item),
                      outcome.kind(),
                      outcome.probability());
                })
            .toList();
    return new Distribution(
        VERSION,
        outcomes.jobId(),
        outcomes.revision(),
        outcomes.methodId(),
        outcomes.cycles(),
        items,
        outcomes.nextOffset());
  }
}
