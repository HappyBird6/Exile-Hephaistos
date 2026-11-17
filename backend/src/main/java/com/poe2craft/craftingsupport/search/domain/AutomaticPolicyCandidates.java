package com.poe2craft.craftingsupport.search.domain;

import java.math.BigInteger;
import java.util.*;

/** Bounded deterministic policy alternatives. Ranking remains success reachability only. */
public final class AutomaticPolicyCandidates {
  public static final String VERSION = "conditional-session-policies-v1";
  public static final int FAILURE = -1, GOAL = -2, REBASE = -3, UNRESOLVED = -4;
  private static final int MAX_ALTERNATIVES = 32, EVALUATION_STEPS = 128;

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

  public record State(boolean goal, boolean open, List<Action> actions) {
    public State {
      actions = List.copyOf(actions);
    }
  }

  public record Decision(int node, String method) {}

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
