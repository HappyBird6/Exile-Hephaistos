package com.poe2craft.craftingsupport.path.application;

import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 고정 행동 계획의 확률을 층별로 전파하고 같은 상태 노드를 공유한다.
 *
 * <p>기존 계약: Layered probability propagation for a fixed action plan. Shared/cyclic graph nodes
 * remain shared.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code transitions}: 전이목록 ({@code TransitionCache}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GraphExplorer(transitions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code transitions}(전이목록).
 *   <li>{@code explore(root, plan, maxNodes, maxEdges, stop)}: explore 처리에서 {@code plan.isEmpty},
 *       {@code plan.size}을(를) 사용한다. 입력: {@code root}(시작점), {@code plan}(계획), {@code
 *       maxNodes}(최대값·노드목록), {@code maxEdges}(최대값·간선목록), {@code stop}(stop). 반환: {@code Result}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Edge}: {@code GraphExplorer}에서 사용하는 간선의 불변 값이다.
 *   <li>{@code Terminal}: {@code GraphExplorer}에서 사용하는 종료의 불변 값이다.
 *   <li>{@code Result}: {@code GraphExplorer}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class GraphExplorer {
  private final TransitionCache transitions;

  public GraphExplorer(TransitionCache transitions) {
    this.transitions = transitions;
  }

  public Result explore(
      StateBucket root,
      List<CraftingAction> plan,
      int maxNodes,
      int maxEdges,
      BooleanSupplier stop) {
    if (plan == null
        || plan.isEmpty()
        || plan.size() > 4
        || plan.stream().anyMatch(java.util.Objects::isNull)
        || maxNodes < 1
        || maxNodes > 2000
        || maxEdges < 1
        || maxEdges > 10000) {
      throw new IllegalArgumentException("Invalid exploration limits or action plan");
    }
    Map<String, StateBucket> nodes = new LinkedHashMap<>();
    nodes.put(root.id(), root);
    Map<StateBucket, Double> frontier = new LinkedHashMap<>();
    frontier.put(root, 1.0);
    var edges = new ArrayList<Edge>();
    var terminals = new ArrayList<Terminal>();
    double blocked = 0, deferred = 0;
    for (int step = 0; step < plan.size(); step++) {
      Map<StateBucket, Double> next = new LinkedHashMap<>();
      for (var entry : frontier.entrySet()) {
        var state = entry.getKey();
        double mass = entry.getValue();
        if (stop.getAsBoolean()) {
          terminals.add(new Terminal(state.id(), step, "DEFERRED", mass));
          deferred += mass;
          continue;
        }
        var result = transitions.get(state, plan.get(step));
        if (!result.available()) {
          terminals.add(new Terminal(state.id(), step, "BLOCKED", mass));
          blocked += mass;
          continue;
        }
        long added = result.outcomes().stream().filter(o -> !nodes.containsKey(o.id())).count();
        if (nodes.size() + added > maxNodes
            || edges.size() + result.outcomes().size() > maxEdges
            || stop.getAsBoolean()) {
          terminals.add(new Terminal(state.id(), step, "DEFERRED", mass));
          deferred += mass;
          continue;
        }
        for (var outcome : result.outcomes()) {
          nodes.putIfAbsent(outcome.id(), outcome.state());
          edges.add(
              new Edge(state.id(), outcome.id(), step, plan.get(step), outcome.probability()));
          next.merge(outcome.state(), mass * outcome.probability(), Double::sum);
        }
      }
      frontier = next;
    }
    double completed = 0;
    for (var entry : frontier.entrySet()) {
      terminals.add(new Terminal(entry.getKey().id(), plan.size(), "COMPLETE", entry.getValue()));
      completed += entry.getValue();
    }
    terminals.sort(
        java.util.Comparator.comparingDouble(Terminal::probability)
            .reversed()
            .thenComparing(Terminal::id));
    return new Result(
        Map.copyOf(nodes),
        List.copyOf(edges),
        List.copyOf(terminals),
        completed,
        blocked,
        deferred,
        deferred == 0);
  }

  /**
   * {@code GraphExplorer}에서 사용하는 간선의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code fromId}: 원본·식별자 ({@code String}).
   *   <li>{@code toId}: 대상·식별자 ({@code String}).
   *   <li>{@code step}: 단계 ({@code int}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code CraftingAction}).
   *   <li>{@code probability}: 확률 ({@code double}).
   * </ul>
   */
  public record Edge(
      String fromId, String toId, int step, CraftingAction action, double probability) {}

  /**
   * {@code GraphExplorer}에서 사용하는 종료의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code step}: 단계 ({@code int}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code double}).
   * </ul>
   */
  public record Terminal(String id, int step, String status, double probability) {}

  /**
   * {@code GraphExplorer}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code nodes}: 노드목록 ({@code Map<String, StateBucket>}).
   *   <li>{@code edges}: 간선목록 ({@code List<Edge>}).
   *   <li>{@code terminals}: terminals ({@code List<Terminal>}).
   *   <li>{@code completedProbability}: 완료·확률 ({@code double}).
   *   <li>{@code blockedProbability}: 차단·확률 ({@code double}).
   *   <li>{@code unexploredProbability}: unexplored·확률 ({@code double}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   * </ul>
   */
  public record Result(
      Map<String, StateBucket> nodes,
      List<Edge> edges,
      List<Terminal> terminals,
      double completedProbability,
      double blockedProbability,
      double unexploredProbability,
      boolean complete) {}
}
