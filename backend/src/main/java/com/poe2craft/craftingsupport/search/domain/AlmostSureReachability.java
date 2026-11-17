package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.OutcomeStrategy.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 아이템 이력을 나열하지 않고 확률 1의 도달 가능성과 정책 근거를 계산한다.
 *
 * <p>기존 계약: Qualitative reachability with a proper policy witness, without enumerating item
 * histories.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code graph}: 그래프 ({@code Map<S, State<S>>}).
 *   <li>{@code candidate}: 후보 ({@code Set<S>}).
 *   <li>{@code reachable}: 도달가능 ({@code Set<S>}).
 *   <li>{@code reverse}: reverse ({@code Map<S, List<Arc<S>>>}).
 *   <li>{@code policy}: 정책 ({@code Map<S, String>}).
 *   <li>{@code queue}: 대기열 ({@code ArrayDeque<S>}).
 *   <li>{@code states}: 상태목록 ({@code Iterator<Map.Entry<S, State<S>>>}).
 *   <li>{@code choices}: 선택목록 ({@code Iterator<Choice<S>>}).
 *   <li>{@code targets}: 대상목록 ({@code Iterator<Map.Entry<S, Fraction>>}).
 *   <li>{@code predecessors}: predecessors ({@code Iterator<Arc<S>>}).
 *   <li>{@code source}: 출처 ({@code S}).
 *   <li>{@code choice}: 선택 ({@code Choice<S>}).
 *   <li>{@code safe}: 안전범위 ({@code boolean}).
 *   <li>{@code adding}: adding ({@code boolean}).
 *   <li>{@code traversing}: traversing ({@code boolean}).
 *   <li>{@code unsupported}: 미지원 ({@code boolean}).
 *   <li>{@code solution}: 해 ({@code Solution<S>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AlmostSureReachability(graph)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code graph}(그래프).
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 반환: {@code Optional<Solution<S>>}.
 *   <li>{@code beginPass()}: 시작·Pass 처리에서 {@code graph.entrySet}, {@code
 *       Collections.emptyIterator}을(를) 사용한다.
 *   <li>{@code advance()}: 허용된 계산량만큼 작업을 진행한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Solution}: {@code AlmostSureReachability}에서 사용하는 해의 불변 값이다.
 *   <li>{@code Arc}: {@code AlmostSureReachability}에서 사용하는 Arc의 불변 값이다.
 * </ul>
 */
final class AlmostSureReachability<S> {
  /**
   * {@code AlmostSureReachability}에서 사용하는 해의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code winning}: winning ({@code Set<S>}).
   *   <li>{@code policy}: 정책 ({@code Map<S, String>}).
   * </ul>
   */
  record Solution<S>(Set<S> winning, Map<S, String> policy) {}

  /**
   * {@code AlmostSureReachability}에서 사용하는 Arc의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code S}).
   *   <li>{@code method}: 제작방법 ({@code String}).
   * </ul>
   */
  private record Arc<S>(S source, String method) {}

  private final Map<S, State<S>> graph;
  private Set<S> candidate;
  private Set<S> reachable;
  private Map<S, List<Arc<S>>> reverse;
  private Map<S, String> policy;
  private ArrayDeque<S> queue;
  private Iterator<Map.Entry<S, State<S>>> states;
  private Iterator<Choice<S>> choices = Collections.emptyIterator();
  private Iterator<Map.Entry<S, Fraction>> targets = Collections.emptyIterator();
  private Iterator<Arc<S>> predecessors = Collections.emptyIterator();
  private S source;
  private Choice<S> choice;
  private boolean safe, adding, traversing, unsupported;
  private Solution<S> solution;

  AlmostSureReachability(Map<S, State<S>> graph) {
    this.graph = graph;
    candidate = new HashSet<>(graph.keySet());
    beginPass();
  }

  /** At most 128 state/choice/support-edge operations; no numeric convergence tolerance. */
  Optional<Solution<S>> step() {
    for (int n = 0; n < 128 && solution == null && !unsupported; n++) advance();
    return Optional.ofNullable(solution);
  }

  private void beginPass() {
    reachable = new HashSet<>();
    reverse = new HashMap<>();
    policy = new HashMap<>();
    queue = new ArrayDeque<>();
    states = graph.entrySet().iterator();
    choices = Collections.emptyIterator();
    targets = Collections.emptyIterator();
    predecessors = Collections.emptyIterator();
    choice = null;
    traversing = false;
  }

  private void advance() {
    if (traversing) {
      if (predecessors.hasNext()) {
        var arc = predecessors.next();
        if (reachable.add(arc.source())) {
          queue.add(arc.source());
          policy.put(arc.source(), arc.method());
        }
      } else if (!queue.isEmpty()) {
        predecessors = reverse.getOrDefault(queue.remove(), List.of()).iterator();
      } else if (reachable.equals(candidate)) {
        // Each witness stays inside this set and has a positive edge to a smaller BFS rank.
        // A finite closed non-goal recurrent class is therefore impossible under this policy.
        solution = new Solution<>(Set.copyOf(reachable), Map.copyOf(policy));
      } else {
        candidate = reachable;
        beginPass();
      }
      return;
    }
    if (targets.hasNext()) {
      var target = targets.next();
      if (!target.getValue().equals(Fraction.ZERO)) {
        if (adding)
          reverse
              .computeIfAbsent(target.getKey(), ignored -> new ArrayList<>())
              .add(new Arc<>(source, choice.methodId()));
        else if (!candidate.contains(target.getKey())) safe = false;
      }
      return;
    }
    if (choice != null) {
      if (!adding && safe) {
        adding = true;
        targets = choice.outcomes().entrySet().iterator();
      } else choice = null;
      return;
    }
    if (choices.hasNext()) {
      choice = choices.next();
      if (!choice.unresolved().equals(Fraction.ZERO)) {
        unsupported = true;
        return;
      }
      safe = candidate.contains(source);
      adding = false;
      targets = choice.outcomes().entrySet().iterator();
      return;
    }
    if (states.hasNext()) {
      var state = states.next();
      if (!state.getValue().expansionComplete()) {
        unsupported = true;
        return;
      }
      source = state.getKey();
      if (state.getValue().goal()) {
        reachable.add(source);
        queue.add(source);
      }
      choices = state.getValue().choices().iterator();
      return;
    }
    traversing = true;
  }
}
