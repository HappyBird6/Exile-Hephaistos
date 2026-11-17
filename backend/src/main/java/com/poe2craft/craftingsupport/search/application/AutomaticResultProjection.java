package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.application.AutomaticSearchService.*;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodExecution;
import com.poe2craft.craftingsupport.search.domain.AutomaticMethodGraph;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.craftingsupport.search.domain.TierCell;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.probability.Fraction;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 탐색 계산 결과를 응답 그래프·추천·확률 구간으로 투영한다. 작업 소유권이나 저장 상태는 변경하지 않는다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticResultProjection()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code uses(input)}: 경로에서 사용한 재료 수량을 집계한다. 입력: {@code input}(입력). 반환: {@code Map<String,
 *       CertifiedMethodProtocol.Bounds>}.
 *   <li>{@code arrows(nodes, methodViews)}: 그래프 간선을 공개 응답으로 변환한다. 입력: {@code nodes}(노드목록), {@code
 *       methodViews}(제작방법·Views). 반환: {@code List<Arrow>}.
 *   <li>{@code evaluate(graph)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code graph}(그래프). 반환: {@code
 *       List<PolicyCandidate>}.
 *   <li>{@code render(id, revision, generation, status, reason, graph, candidates)}: 계산 결과를 외부 응답
 *       형태로 만든다. 입력: {@code id}(식별자), {@code revision}(리비전), {@code generation}(세대번호), {@code
 *       status}(상태), {@code reason}(이유), {@code graph}(그래프), {@code candidates}(후보목록). 반환: {@code
 *       View}.
 * </ul>
 */
final class AutomaticResultProjection {
  private AutomaticResultProjection() {}

  private static Map<String, CertifiedMethodProtocol.Bounds> uses(
      Map<AutomaticCraftingAction, CertifiedInterval> input) {
    var result = new LinkedHashMap<String, CertifiedMethodProtocol.Bounds>();
    AutomaticMethodExecution.materialUses(input)
        .forEach((key, value) -> result.put(key, CertifiedMethodProtocol.Bounds.quantity(value)));
    return result;
  }

  static List<Arrow> arrows(
      List<AutomaticMethodGraph.NodeView> nodes,
      List<AutomaticMethodGraph.MethodView> methodViews) {
    var probabilities = new HashMap<CertifiedInterval, CertifiedMethodProtocol.Bounds>();
    Function<CertifiedInterval, CertifiedMethodProtocol.Bounds> probability =
        value ->
            probabilities.computeIfAbsent(
                value, v -> CertifiedMethodProtocol.Bounds.probability(v, 2));
    var ids = new HashMap<AutomaticMethodGraph.Identity, Integer>();
    nodes.forEach(
        n ->
            ids.put(
                new AutomaticMethodGraph.Identity(
                    n.item(), n.quality40(), n.abstractQuality(), n.tierCells()),
                n.id()));
    var memberViews = new HashMap<AutomaticMethodGraph.Identity, AutomaticMethodGraph.MemberView>();
    for (var n : nodes)
      for (var member : n.members()) {
        var cells = new LinkedHashMap<String, TierCell>();
        member
            .tiers()
            .forEach(
                (id, law) ->
                    cells.put(
                        id,
                        new TierCell(
                            law.variants().stream()
                                .map(
                                    v ->
                                        new TierCell.Variant(
                                            v.modifierId(),
                                            v.stat(),
                                            v.min(),
                                            v.max(),
                                            new Fraction(
                                                new java.math.BigInteger(
                                                    v.probability().numerator()),
                                                new java.math.BigInteger(
                                                    v.probability().denominator()))))
                                .toList())));
        var key =
            new AutomaticMethodGraph.Identity(
                member.item(), n.quality40(), n.abstractQuality(), cells);
        ids.put(key, n.id());
        memberViews.put(key, member);
      }
    var arrows = new ArrayList<Arrow>();
    for (var method : methodViews) {
      var points = new ArrayList<PointView>();
      for (var entry : method.points().entrySet()) {
        var rows = new ArrayList<ExitView>();
        var grouped = new LinkedHashMap<String, CertifiedInterval>();
        var components = new LinkedHashMap<String, List<JointCase>>();
        for (var mass : AutomaticMethodGraph.encode(entry.getValue()).outcomes()) {
          var target =
              ids.get(
                  new AutomaticMethodGraph.Identity(
                      mass.state().item(),
                      mass.state().quality40(),
                      mass.state().abstractQuality(),
                      mass.state().tiers()));
          if (mass.exit() == Exit.LOSS || mass.exit() == Exit.DEAD || mass.exit() == Exit.REBASE)
            target = null;
          boolean retry =
              mass.exit() == null && method.retryRepresentatives().contains(mass.state().item());
          if (retry) target = null;
          String kind = retry ? "RETRY" : mass.exit() == null ? "ACTIVE" : mass.exit().name();
          if ((mass.exit() == Exit.UNRESOLVED && !mass.state().temporaryPreparation())
              || (target == null
                  && !retry
                  && mass.exit() != Exit.DEAD
                  && mass.exit() != Exit.LOSS
                  && mass.exit() != Exit.REBASE)) {
            target = null;
            kind = "UNRESOLVED";
          }
          var key = (target == null ? "null" : target.toString()) + ":" + kind;
          grouped.merge(key, mass.probability(), CertifiedInterval::add);
          if (target != null && !memberViews.isEmpty()) {
            var state = mass.state();
            var member =
                memberViews.get(
                    new AutomaticMethodGraph.Identity(
                        state.item(), state.quality40(), state.abstractQuality(), state.tiers()));
            var fresh = new LinkedHashMap<>(member.tiers());
            fresh.keySet().removeAll(state.retainedFromRoot());
            var rolls = new LinkedHashMap<>(member.rolls());
            rolls.keySet().removeAll(state.retainedFromRoot());
            rolls.keySet().removeAll(fresh.keySet());
            components
                .computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(
                    new JointCase(
                        state.item().explicits().stream()
                            .filter(m -> !state.retainedFromRoot().contains(m.modifierId()))
                            .toList(),
                        Map.copyOf(fresh),
                        Map.copyOf(rolls),
                        state.retainedFromRoot(),
                        probability.apply(mass.probability())));
          }
        }
        grouped.forEach(
            (key, value) -> {
              var pair = key.split(":");
              rows.add(
                  new ExitView(
                      pair[0].equals("null") ? null : Integer.valueOf(pair[0]),
                      pair[1],
                      probability.apply(value),
                      components.getOrDefault(key, List.of())));
            });
        rows.sort(
            Comparator.comparing(ExitView::kind)
                .thenComparing(ExitView::target, Comparator.nullsFirst(Comparator.naturalOrder())));
        points.add(
            new PointView(
                entry.getKey(),
                method.completeObservations().contains(entry.getKey()),
                rows,
                uses(entry.getValue().initialUses()),
                uses(entry.getValue().cycleUses())));
      }
      points.sort(Comparator.comparingInt(PointView::cycles));
      arrows.add(
          new Arrow(
              method.id(),
              method.source(),
              method.template().initial(),
              method.template().cycle(),
              method.executionMode(),
              points,
              method.unresolvedReasons(),
              method.finished()));
    }
    return List.copyOf(arrows);
  }

  static List<PolicyCandidate> evaluate(AutomaticMethodGraph graph) {
    graph.refineStrategy(graph.strategy(100), 16);
    graph.updatePriorities();
    return graph.policyCandidates(3).stream()
        .map(
            c -> {
              var uses = new LinkedHashMap<String, ExpectedUse>();
              c.uses()
                  .forEach(
                      (material, amount) -> {
                        var bounds = CertifiedMethodProtocol.Bounds.quantity(amount);
                        uses.put(
                            material,
                            new ExpectedUse(
                                bounds.lower(), c.usageComplete() ? bounds.upper() : null));
                      });
              return new PolicyCandidate(
                  c.id(),
                  100,
                  c.decisions(),
                  CertifiedMethodProtocol.Bounds.probability(c.success(), 2),
                  CertifiedMethodProtocol.Bounds.probability(c.rebase(), 2),
                  CertifiedMethodProtocol.Bounds.probability(c.failure(), 2),
                  CertifiedMethodProtocol.Bounds.probability(c.unresolved(), 2),
                  uses,
                  c.usageComplete() ? "BOUNDED" : "LOWER_BOUND_ONLY",
                  c.rankStatus(),
                  c.distinguishingDecisions());
            })
        .toList();
  }

  static View render(
      String id,
      long revision,
      long generation,
      String status,
      String reason,
      AutomaticMethodGraph graph,
      List<PolicyCandidate> candidates) {
    var nodes = graph.nodes();
    var methodViews = graph.methods();
    var probabilities = new HashMap<CertifiedInterval, CertifiedMethodProtocol.Bounds>();
    Function<CertifiedInterval, CertifiedMethodProtocol.Bounds> probability =
        value ->
            probabilities.computeIfAbsent(
                value, v -> CertifiedMethodProtocol.Bounds.probability(v, 2));
    var arrows = arrows(nodes, methodViews);
    var recommendations = new ArrayList<Recommendation>();
    for (int cycles : List.of(100)) {
      var strategy = graph.strategy(cycles);
      for (var node : nodes) {
        var scores = new LinkedHashMap<String, CertifiedInterval>();
        if (node.directFinished())
          for (var method : methodViews)
            if (method.source() == node.id() && method.points().containsKey(cycles)) {
              var point = method.points().get(cycles);
              var score = CertifiedInterval.zero(64);
              for (var kind : List.of(Exit.GOAL, Exit.CHECKPOINT))
                for (var mass : point.exits().get(kind).values()) score = score.add(mass);
              var unknown = CertifiedInterval.zero(64);
              for (var mass : point.exits().get(Exit.UNRESOLVED).values())
                unknown = unknown.add(mass);
              score =
                  new CertifiedInterval(
                      64, score.lowerUnits(), score.upperUnits().add(unknown.upperUnits()));
              scores.put(method.id(), score.probability());
            }
        var local =
            scores.entrySet().stream()
                .filter(
                    e ->
                        scores.entrySet().stream()
                            .filter(other -> !other.getKey().equals(e.getKey()))
                            .allMatch(
                                other ->
                                    e.getValue()
                                            .lowerUnits()
                                            .compareTo(other.getValue().upperUnits())
                                        >= 0))
                .map(Map.Entry::getKey)
                .toList();
        var result = strategy.result(node.id());
        recommendations.add(
            new Recommendation(
                node.id(),
                cycles,
                local,
                probability.apply(result.value()),
                result.status(),
                result.methodIds()));
      }
    }
    return new View(
        AutomaticSearchService.VERSION,
        id,
        revision,
        generation,
        status,
        reason,
        graph.complete(),
        !reason.equals("STORAGE_BUDGET") && graph.canResume(),
        graph.operations(),
        graph.retainedStates(),
        graph.resources(),
        nodes,
        arrows,
        recommendations,
        graph.rollModels(),
        candidates,
        graph.rareOutcomePolicy());
  }
}
