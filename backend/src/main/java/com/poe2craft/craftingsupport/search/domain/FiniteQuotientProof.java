package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 닫힌 유한 사례에서 행동별 확률적 동치 관계를 증명한다.
 *
 * <p>기존 계약: Exact labelled probabilistic bisimulation for closed finite fixtures. This proof oracle
 * requires the original graph; it is not an on-demand production state-space reduction. Observation
 * keys must be immutable and include every goal/checkpoint judgement relevant to the caller.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code original}: 원본 ({@code Map<S, Node<S>>}).
 *   <li>{@code blocks}: 블록목록 ({@code Map<S, Integer>}).
 *   <li>{@code quotient}: quotient ({@code Map<Integer, Node<Integer>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code FiniteQuotientProof(graph)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code graph}(그래프). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code blocks()}: {@code blocks} 값을 조회한다. 반환: {@code Map<S, Integer>}.
 *   <li>{@code graph()}: {@code quotient} 값을 조회한다. 반환: {@code Map<Integer, Node<Integer>>}.
 *   <li>{@code condition(entry, action, targetBlock)}: 조건에 해당하는 분포를 구한다. 입력: {@code entry}(항목),
 *       {@code action}(행동), {@code targetBlock}(대상·블록). 반환: {@code Conditional<S>}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code project(law)}: 투영의 입력에 따른 분기·결과를 처리한다. 입력: {@code law}(분포). 반환: {@code Map<Integer,
 *       Fraction>}.
 *   <li>{@code aggregate(law, blocks)}: aggregate 처리에서 {@code law.forEach}, {@code blocks.get}을(를)
 *       사용한다. 입력: {@code law}(분포), {@code blocks}(블록목록). 반환: {@code Map<Integer, Fraction>}.
 *   <li>{@code sum(law)}: 항목들의 합을 계산한다. 입력: {@code law}(분포). 반환: {@code Fraction}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Action}: {@code FiniteQuotientProof}에서 사용하는 행동의 불변 값이다.
 *   <li>{@code Node}: {@code FiniteQuotientProof}에서 사용하는 노드의 불변 값이다.
 *   <li>{@code Conditional}: {@code FiniteQuotientProof}에서 사용하는 조건부의 불변 값이다.
 *   <li>{@code Law}: {@code FiniteQuotientProof}에서 사용하는 분포의 불변 값이다.
 *   <li>{@code Signature}: {@code FiniteQuotientProof}에서 사용하는 서명의 불변 값이다.
 * </ul>
 */
public final class FiniteQuotientProof<S> {
  /**
   * {@code FiniteQuotientProof}에서 사용하는 행동의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code label}: 표시이름 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<S, Fraction>}).
   *   <li>{@code costs}: costs ({@code Map<String, Fraction>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Action(label, outcomes, costs)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code label}(표시이름),
   *       {@code outcomes}(결과목록), {@code costs}(costs). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Action<S>(String label, Map<S, Fraction> outcomes, Map<String, Fraction> costs) {
    public Action {
      if (label == null || label.isBlank())
        throw new IllegalArgumentException("Action label required");
      outcomes = Map.copyOf(outcomes);
      costs = Map.copyOf(costs);
      if (!sum(outcomes).equals(Fraction.ONE))
        throw new IllegalArgumentException("Only complete unit-mass transition laws can be proved");
    }
  }

  /**
   * {@code FiniteQuotientProof}에서 사용하는 노드의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code observation}: 관측값 ({@code Object}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code actions}: 행동목록 ({@code List<Action<S>>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Node(observation, complete, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       observation}(관측값), {@code complete}(완료), {@code actions}(행동목록). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Node<S>(Object observation, boolean complete, List<Action<S>> actions) {
    public Node {
      Objects.requireNonNull(observation);
      actions = List.copyOf(actions);
      if (!complete || actions.stream().map(Action::label).distinct().count() != actions.size())
        throw new IllegalArgumentException("Complete states with unique action labels required");
    }
  }

  /**
   * {@code FiniteQuotientProof}에서 사용하는 조건부의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
   *   <li>{@code law}: 분포 ({@code Map<S, Fraction>}).
   * </ul>
   */
  public record Conditional<S>(Fraction mass, Map<S, Fraction> law) {}

  /**
   * {@code FiniteQuotientProof}에서 사용하는 분포의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code blocks}: 블록목록 ({@code Map<Integer, Fraction>}).
   *   <li>{@code costs}: costs ({@code Map<String, Fraction>}).
   * </ul>
   */
  private record Law(Map<Integer, Fraction> blocks, Map<String, Fraction> costs) {}

  /**
   * {@code FiniteQuotientProof}에서 사용하는 서명의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code previousBlock}: 이전·블록 ({@code int}).
   *   <li>{@code actions}: 행동목록 ({@code Map<String, Law>}).
   * </ul>
   */
  private record Signature(int previousBlock, Map<String, Law> actions) {}

  private final Map<S, Node<S>> original;
  private final Map<S, Integer> blocks;
  private final Map<Integer, Node<Integer>> quotient;

  public FiniteQuotientProof(Map<S, Node<S>> graph) {
    original = Map.copyOf(graph);
    for (var node : graph.values())
      for (var action : node.actions())
        for (var state : action.outcomes().keySet())
          if (!graph.containsKey(state)) throw new IllegalArgumentException("Graph must be closed");
    var observations = new HashMap<Object, Integer>();
    var partition = new LinkedHashMap<S, Integer>();
    graph.forEach(
        (s, n) ->
            partition.put(
                s, observations.computeIfAbsent(n.observation(), ignored -> observations.size())));
    while (true) {
      var keys = new HashMap<Signature, Integer>();
      var refined = new LinkedHashMap<S, Integer>();
      for (var entry : graph.entrySet()) {
        var actions = new HashMap<String, Law>();
        for (var action : entry.getValue().actions())
          actions.put(
              action.label(), new Law(aggregate(action.outcomes(), partition), action.costs()));
        var key = new Signature(partition.get(entry.getKey()), Map.copyOf(actions));
        refined.put(entry.getKey(), keys.computeIfAbsent(key, ignored -> keys.size()));
      }
      // Refinement never merges previous classes; an unchanged class count proves stability.
      boolean stable = new HashSet<>(partition.values()).size() == keys.size();
      partition.clear();
      partition.putAll(refined);
      if (stable) break;
    }
    blocks = Map.copyOf(partition);
    var reduced = new LinkedHashMap<Integer, Node<Integer>>();
    graph.forEach(
        (state, node) ->
            reduced.computeIfAbsent(
                blocks.get(state),
                ignored ->
                    new Node<>(
                        node.observation(),
                        true,
                        node.actions().stream()
                            .map(
                                action ->
                                    new Action<>(
                                        action.label(),
                                        aggregate(action.outcomes(), blocks),
                                        action.costs()))
                            .toList())));
    quotient = Map.copyOf(reduced);
  }

  public Map<S, Integer> blocks() {
    return blocks;
  }

  public Map<Integer, Node<Integer>> graph() {
    return quotient;
  }

  /**
   * Push one specified entry law through one action, then condition on its selected result block.
   */
  public Conditional<S> condition(Map<S, Fraction> entry, String action, int targetBlock) {
    if (!sum(entry).equals(Fraction.ONE))
      throw new IllegalArgumentException("Normalized entry law required");
    var sourceBlocks = new HashSet<Integer>();
    var reached = new LinkedHashMap<S, Fraction>();
    for (var source : entry.entrySet()) {
      var node = original.get(source.getKey());
      if (node == null) throw new IllegalArgumentException("Unknown entry state");
      if (source.getValue().equals(Fraction.ZERO)) continue;
      sourceBlocks.add(blocks.get(source.getKey()));
      var selected =
          node.actions().stream().filter(a -> a.label().equals(action)).findFirst().orElseThrow();
      selected
          .outcomes()
          .forEach(
              (target, p) -> {
                if (blocks.get(target) == targetBlock && !p.equals(Fraction.ZERO))
                  reached.merge(target, source.getValue().multiply(p), Fraction::add);
              });
    }
    if (sourceBlocks.size() != 1)
      throw new IllegalArgumentException("Entry law must belong to one proved block");
    var mass = sum(reached);
    var conditional = new LinkedHashMap<S, Fraction>();
    if (!mass.equals(Fraction.ZERO))
      reached.forEach(
          (state, p) ->
              conditional.put(
                  state,
                  new Fraction(
                      p.numerator().multiply(mass.denominator()),
                      p.denominator().multiply(mass.numerator()))));
    return new Conditional<>(mass, Map.copyOf(conditional));
  }

  public Map<Integer, Fraction> project(Map<S, Fraction> law) {
    return aggregate(law, blocks);
  }

  private static <S> Map<Integer, Fraction> aggregate(
      Map<S, Fraction> law, Map<S, Integer> blocks) {
    var result = new HashMap<Integer, Fraction>();
    law.forEach(
        (state, mass) -> {
          var block = Objects.requireNonNull(blocks.get(state), "Unknown state");
          if (!mass.equals(Fraction.ZERO)) result.merge(block, mass, Fraction::add);
        });
    return Map.copyOf(result);
  }

  private static Fraction sum(Map<?, Fraction> law) {
    return law.values().stream().reduce(Fraction.ZERO, Fraction::add);
  }
}
