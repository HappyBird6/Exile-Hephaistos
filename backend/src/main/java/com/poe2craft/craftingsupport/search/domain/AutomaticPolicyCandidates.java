package com.poe2craft.craftingsupport.search.domain;

import java.math.BigInteger;
import java.util.*;

/**
 * 결정적 정책 후보를 제한된 수로 만들고 성공 도달 가능성으로 비교한다.
 *
 * <p>기존 계약: Bounded deterministic policy alternatives. Ranking remains success reachability only.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "conditional-session-policies-v1"} ({@code String}).
 *   <li>{@code FAILURE}: 실패; 초기값 {@code -1} ({@code int}).
 *   <li>{@code GOAL}: 목표; 초기값 {@code -2} ({@code int}).
 *   <li>{@code REBASE}: 기준변경; 초기값 {@code -3} ({@code int}).
 *   <li>{@code UNRESOLVED}: 미해결; 초기값 {@code -4} ({@code int}).
 *   <li>{@code MAX_ALTERNATIVES}: ALTERNATIVES; 초기값 {@code 32} ({@code int}).
 *   <li>{@code EVALUATION_STEPS}: 단계목록; 초기값 {@code 128} ({@code int}).
 *   <li>{@code states}: 상태목록 ({@code Map<Integer, State>}).
 *   <li>{@code strategy}: 전략 ({@code CertifiedOutcomeStrategy<Integer>}).
 *   <li>{@code bits}: 비트수 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticPolicyCandidates(states, strategy, bits)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       states}(상태목록), {@code strategy}(전략), {@code bits}(비트수).
 *   <li>{@code top(count)}: 상위 처리에서 {@code states.get}, {@code entry.getKey}을(를) 사용한다. 입력: {@code
 *       count}(개수). 반환: {@code List<Candidate>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code ranked(node)}: ranked 처리에서 {@code strategy.choiceValues}, {@code states.get}을(를)
 *       사용한다. 입력: {@code node}(노드). 반환: {@code List<Action>}.
 *   <li>{@code action(node, policy)}: 행동 처리에서 {@code states.get}, {@code a.id}을(를) 사용한다. 입력: {@code
 *       node}(노드), {@code policy}(정책). 반환: {@code Action}.
 *   <li>{@code reachable(policy)}: 도달가능 처리에서 {@code pending.add}, {@code pending.isEmpty}을(를) 사용한다.
 *       입력: {@code policy}(정책). 반환: {@code List<Decision>}.
 *   <li>{@code add(candidates, policy, differences, count)}: 값 또는 항목을 더한다. 입력: {@code
 *       candidates}(후보목록), {@code policy}(정책), {@code differences}(differences), {@code count}(개수).
 *   <li>{@code evaluate(id, policy, decisions, differences)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력: {@code
 *       id}(식별자), {@code policy}(정책), {@code decisions}(결정목록), {@code differences}(differences).
 *       반환: {@code Candidate}.
 *   <li>{@code withTail(known, tail)}: Tail을(를) 반영한 값을 구한다. 입력: {@code known}(알려진), {@code
 *       tail}(tail). 반환: {@code CertifiedInterval}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Action}: {@code AutomaticPolicyCandidates}에서 사용하는 행동의 불변 값이다.
 *   <li>{@code State}: {@code AutomaticPolicyCandidates}에서 사용하는 상태의 불변 값이다.
 *   <li>{@code Decision}: {@code AutomaticPolicyCandidates}에서 사용하는 결정의 불변 값이다.
 *   <li>{@code Candidate}: {@code AutomaticPolicyCandidates}에서 사용하는 후보의 불변 값이다.
 * </ul>
 */
public final class AutomaticPolicyCandidates {
  public static final String VERSION = "conditional-session-policies-v1";
  public static final int FAILURE = -1, GOAL = -2, REBASE = -3, UNRESOLVED = -4;
  private static final int MAX_ALTERNATIVES = 32, EVALUATION_STEPS = 128;

  /**
   * {@code AutomaticPolicyCandidates}에서 사용하는 행동의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<Integer, CertifiedInterval>}).
   *   <li>{@code uses}: 사용량 ({@code Map<String, CertifiedInterval>}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code decisionKey}: 결정·키 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Action(id, outcomes, uses, complete)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       id}(식별자), {@code outcomes}(결과목록), {@code uses}(사용량), {@code complete}(완료).
   *   <li>{@code Action(id, outcomes, uses, complete, decisionKey)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code id}(식별자), {@code outcomes}(결과목록), {@code uses}(사용량), {@code complete}(완료), {@code
   *       decisionKey}(결정·키).
   * </ul>
   */
  public record Action(
      String id,
      Map<Integer, CertifiedInterval> outcomes,
      Map<String, CertifiedInterval> uses,
      boolean complete,
      String decisionKey) {
    public Action(
        String id,
        Map<Integer, CertifiedInterval> outcomes,
        Map<String, CertifiedInterval> uses,
        boolean complete) {
      this(id, outcomes, uses, complete, id);
    }

    public Action {
      outcomes = Map.copyOf(outcomes);
      uses = Map.copyOf(uses);
    }
  }

  /**
   * {@code AutomaticPolicyCandidates}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code goal}: 목표 ({@code boolean}).
   *   <li>{@code open}: 열린 ({@code boolean}).
   *   <li>{@code actions}: 행동목록 ({@code List<Action>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code State(goal, open, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code goal}(목표), {@code
   *       open}(열린), {@code actions}(행동목록).
   * </ul>
   */
  public record State(boolean goal, boolean open, List<Action> actions) {
    public State {
      actions = List.copyOf(actions);
    }
  }

  /**
   * {@code AutomaticPolicyCandidates}에서 사용하는 결정의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code node}: 노드 ({@code int}).
   *   <li>{@code method}: 제작방법 ({@code String}).
   * </ul>
   */
  public record Decision(int node, String method) {}

  /**
   * {@code AutomaticPolicyCandidates}에서 사용하는 후보의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code decisions}: 결정목록 ({@code List<Decision>}).
   *   <li>{@code success}: 성공 ({@code CertifiedInterval}).
   *   <li>{@code rebase}: 기준변경 ({@code CertifiedInterval}).
   *   <li>{@code failure}: 실패 ({@code CertifiedInterval}).
   *   <li>{@code unresolved}: 미해결 ({@code CertifiedInterval}).
   *   <li>{@code uses}: 사용량 ({@code Map<String, CertifiedInterval>}).
   *   <li>{@code usageComplete}: 사용량·완료 ({@code boolean}).
   *   <li>{@code rankStatus}: 순위·상태 ({@code String}).
   *   <li>{@code distinguishingDecisions}: distinguishing·결정목록 ({@code List<Decision>}).
   * </ul>
   */
  public record Candidate(
      String id,
      List<Decision> decisions,
      CertifiedInterval success,
      CertifiedInterval rebase,
      CertifiedInterval failure,
      CertifiedInterval unresolved,
      Map<String, CertifiedInterval> uses,
      boolean usageComplete,
      String rankStatus,
      List<Decision> distinguishingDecisions) {}

  private final Map<Integer, State> states;
  private final CertifiedOutcomeStrategy<Integer> strategy;
  private final int bits;

  public AutomaticPolicyCandidates(
      Map<Integer, State> states, CertifiedOutcomeStrategy<Integer> strategy, int bits) {
    this.states = Map.copyOf(states);
    this.strategy = strategy;
    this.bits = bits;
  }

  public List<Candidate> top(int count) {
    if (count < 1 || count > 10)
      throw new IllegalArgumentException("Candidate count must be 1..10");
    if (states.get(0).goal() || states.get(0).actions().isEmpty()) return List.of();
    var baseline = new TreeMap<Integer, String>();
    for (var entry : new TreeMap<>(states).entrySet()) {
      var actions = ranked(entry.getKey());
      if (actions.isEmpty() || entry.getValue().goal()) continue;
      var witness = strategy.result(entry.getKey()).methodIds();
      baseline.put(
          entry.getKey(), witness.isEmpty() ? actions.getFirst().id() : witness.getFirst());
    }
    var candidates = new LinkedHashMap<String, Candidate>();
    add(candidates, baseline, List.of(), count);
    // First diversify the initial crafting decision, then reachable follow-up decisions.
    var sources = new ArrayList<Integer>();
    sources.add(0);
    for (var decision : reachable(baseline)) if (decision.node() != 0) sources.add(decision.node());
    for (int source : sources) {
      for (var action : ranked(source)) {
        if (action.id().equals(baseline.get(source))) continue;
        var policy = new TreeMap<>(baseline);
        policy.put(source, action.id());
        add(candidates, policy, List.of(new Decision(source, action.id())), count);
        if (candidates.size() >= MAX_ALTERNATIVES) break;
      }
      if (candidates.size() >= MAX_ALTERNATIVES) break;
    }
    var all = new ArrayList<>(candidates.values());
    all.sort(
        Comparator.<Candidate, BigInteger>comparing(c -> c.success().lowerUnits())
            .reversed()
            .thenComparing(c -> c.success().upperUnits(), Comparator.reverseOrder())
            .thenComparing(Candidate::id));
    // Safe top-k elimination only: k distinct policies must strictly dominate an upper bound.
    var survivors =
        all.stream()
            .filter(
                c ->
                    all.stream()
                            .filter(
                                other ->
                                    other != c
                                        && other
                                                .success()
                                                .lowerUnits()
                                                .compareTo(c.success().upperUnits())
                                            > 0)
                            .count()
                        < count)
            .toList();
    var result = new ArrayList<Candidate>();
    for (var c : survivors.stream().limit(count).toList()) {
      boolean overlap =
          all.stream()
              .anyMatch(
                  other ->
                      other != c
                          && c.success().lowerUnits().compareTo(other.success().upperUnits()) <= 0
                          && other.success().lowerUnits().compareTo(c.success().upperUnits()) <= 0);
      result.add(
          new Candidate(
              c.id(),
              c.decisions(),
              c.success(),
              c.rebase(),
              c.failure(),
              c.unresolved(),
              c.uses(),
              c.usageComplete(),
              overlap ? "OVERLAPPING" : "SEPARATED_IN_CURRENT_SET",
              c.distinguishingDecisions()));
    }
    return List.copyOf(result);
  }

  private List<Action> ranked(int node) {
    var scores = strategy.choiceValues(node);
    return states.get(node).actions().stream()
        .sorted(
            Comparator.<Action, BigInteger>comparing(a -> scores.get(a.id()).lowerUnits())
                .reversed()
                .thenComparing(a -> scores.get(a.id()).upperUnits(), Comparator.reverseOrder())
                .thenComparing(Action::id))
        .toList();
  }

  private Action action(int node, Map<Integer, String> policy) {
    return states.get(node).actions().stream()
        .filter(a -> a.id().equals(policy.get(node)))
        .findFirst()
        .orElse(null);
  }

  private List<Decision> reachable(Map<Integer, String> policy) {
    var reached = new TreeSet<Integer>();
    var pending = new ArrayDeque<Integer>();
    pending.add(0);
    while (!pending.isEmpty()) {
      int node = pending.removeFirst();
      if (node < 0 || !reached.add(node) || states.get(node).goal()) continue;
      var action = action(node, policy);
      if (action != null)
        action
            .outcomes()
            .forEach(
                (target, mass) -> {
                  if (!mass.zero() && target >= 0) pending.add(target);
                });
    }
    return reached.stream()
        .filter(n -> !states.get(n).goal() && action(n, policy) != null)
        .map(n -> new Decision(n, policy.get(n)))
        .toList();
  }

  private void add(
      Map<String, Candidate> candidates,
      Map<Integer, String> policy,
      List<Decision> differences,
      int count) {
    // Optimal continuation bounds apply to every fixed policy using this root action.
    var upper = strategy.choiceValues(0).get(policy.get(0)).upperUnits();
    if (candidates.values().stream()
            .filter(c -> c.success().lowerUnits().compareTo(upper) > 0)
            .count()
        >= count) return;
    var decisions = reachable(policy);
    // Unreachable choices and display names do not create distinct policies.
    String signature =
        decisions.stream()
            .map(d -> d.node() + ":" + action(d.node(), policy).decisionKey())
            .toList()
            .toString();
    String id =
        UUID.nameUUIDFromBytes(
                (VERSION + signature).getBytes(java.nio.charset.StandardCharsets.UTF_8))
            .toString();
    if (!candidates.containsKey(id))
      candidates.put(id, evaluate(id, policy, decisions, differences));
  }

  /** Probability-weighted rewards; the unprocessed tail is never silently called failure. */
  Candidate evaluate(
      String id,
      Map<Integer, String> policy,
      List<Decision> decisions,
      List<Decision> differences) {
    var zero = CertifiedInterval.zero(bits);
    var terminal = new HashMap<Integer, CertifiedInterval>();
    var uses = new TreeMap<String, CertifiedInterval>();
    Map<Integer, CertifiedInterval> pending = new LinkedHashMap<>();
    pending.put(0, CertifiedInterval.one(bits));
    boolean usageComplete = true;
    for (int step = 0; step < EVALUATION_STEPS && !pending.isEmpty(); step++) {
      var next = new LinkedHashMap<Integer, CertifiedInterval>();
      for (var entry : pending.entrySet()) {
        int node = entry.getKey();
        var reach = entry.getValue();
        var state = states.get(node);
        if (state.goal()) {
          terminal.merge(GOAL, reach, CertifiedInterval::add);
          continue;
        }
        var action = action(node, policy);
        if (action == null) {
          terminal.merge(state.open() ? UNRESOLVED : FAILURE, reach, CertifiedInterval::add);
          if (state.open()) usageComplete = false;
          continue;
        }
        usageComplete &= action.complete();
        action
            .uses()
            .forEach(
                (material, amount) ->
                    uses.merge(material, reach.multiply(amount), CertifiedInterval::add));
        action
            .outcomes()
            .forEach(
                (target, mass) -> {
                  var arrival = reach.multiply(mass);
                  if (!arrival.zero())
                    (target < 0 ? terminal : next).merge(target, arrival, CertifiedInterval::add);
                });
      }
      pending = next;
    }
    var tail = zero;
    for (var mass : pending.values()) tail = tail.add(mass);
    tail = tail.probability();
    var unknown = terminal.getOrDefault(UNRESOLVED, zero).add(tail).probability();
    usageComplete &=
        unknown.zero() && uses.keySet().stream().noneMatch(key -> key.startsWith("WORK:"));
    var success = terminal.getOrDefault(GOAL, zero).probability();
    success =
        new CertifiedInterval(
                bits, success.lowerUnits(), success.upperUnits().add(unknown.upperUnits()))
            .probability();
    var rebase = withTail(terminal.getOrDefault(REBASE, zero), tail);
    var failure = withTail(terminal.getOrDefault(FAILURE, zero), tail);
    return new Candidate(
        id,
        decisions,
        success,
        rebase,
        failure,
        unknown,
        Map.copyOf(uses),
        usageComplete,
        "PENDING",
        differences);
  }

  private CertifiedInterval withTail(CertifiedInterval known, CertifiedInterval tail) {
    return new CertifiedInterval(
            bits, known.lowerUnits(), known.upperUnits().add(tail.upperUnits()))
        .probability();
  }
}
