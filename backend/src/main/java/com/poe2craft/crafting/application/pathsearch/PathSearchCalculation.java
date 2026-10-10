package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;
import static com.poe2craft.crafting.domain.pathsearch.RenewalFirstHit.complement;

import com.poe2craft.crafting.application.ChaosRenewalCalculator;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.RenewalFirstHit;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** One job's exact streaming continuation. Mutated only while holding its job monitor. */
final class PathSearchCalculation {
  static final String CANDIDATES = "solar-one-explicit-candidates-v1";
  static final String PROOF = "single-explicit-full-return-numeric-v1";
  static final List<List<WorkbenchCurrency>> ACTIONS =
      List.of(
          List.of(WorkbenchCurrency.CHAOS),
          List.of(WorkbenchCurrency.GREATER_CHAOS),
          List.of(WorkbenchCurrency.PERFECT_CHAOS),
          List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.EXALTED),
          List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.GREATER_EXALTED),
          List.of(WorkbenchCurrency.ANNULMENT, WorkbenchCurrency.PERFECT_EXALTED));
  static final int DISPLAY_OUTCOMES = 24;
  static final int METHOD_DISPLAY_OUTCOMES = 6;
  final BasicCurrencyState root;
  final Function<ItemState, Status> predicate;
  final List<String> observations;
  final List<Candidate> candidates = new ArrayList<>();
  final Map<String, Node> nodes = new LinkedHashMap<>();
  final Map<String, Execution> executions = new LinkedHashMap<>();
  final Map<String, Edge> edges = new LinkedHashMap<>();
  final Map<String, Expansion> expansions = new LinkedHashMap<>();
  final BasicCurrencyTransitions transitions;
  int candidateIndex;
  long evaluations;

  PathSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Status> predicate,
      List<String> observations,
      BasicCurrencyTransitions transitions) {
    this.root = root;
    this.predicate = predicate;
    this.observations = observations;
    this.transitions = transitions;
    node(root);
    for (int i = 0; i < ACTIONS.size(); i++) {
      var policy =
          new Policy(
              "solar-policy-" + (i + 1),
              ACTIONS.get(i).stream().map(Enum::name).toList(),
              "REPEAT_CYCLE");
      candidates.add(new Candidate(policy, ACTIONS.get(i)));
      execution(root, policy, 0);
    }
  }

  boolean complete() {
    return candidateIndex == candidates.size();
  }

  boolean outputLimited() {
    for (var candidate : candidates) {
      if (!candidate.done || candidate.rootHit || candidate.emptyHit || candidate.blocked != null)
        continue;
      var survival = complement(candidate.success.add(candidate.unknown));
      if (survival.equals(Fraction.ZERO) || survival.equals(Fraction.ONE)) continue;
      long maximum = Long.parseLong(observations.getLast()) / candidate.actions.size();
      if (maximum > (65536 / observations.size()) / survival.denominator().bitLength()) return true;
    }
    return false;
  }

  /** At most one elementary outcome per call; cursor and its accumulated mass commit together. */
  void step() {
    if (complete()) return;
    var c = candidates.get(candidateIndex);
    if (predicate.apply(root.item()) == Status.MATCH) {
      c.rootHit = true;
      c.done = true;
      candidateIndex++;
      return;
    }
    if (c.cursor == null) {
      c.cursor = transitions.renewalCursor(root, c.actions);
      if (c.actions.size() == 2 && c.cursor.emptyState() != null) {
        var emptyStatus = predicate.apply(c.cursor.emptyState().item());
        c.emptyHit = emptyStatus == Status.MATCH;
        if (emptyStatus == Status.UNKNOWN || emptyStatus == Status.UNSUPPORTED)
          c.blocked = "EMPTY_GOAL_UNRESOLVED";
        var from = execution(root, c.policy, 0);
        var to = execution(c.cursor.emptyState(), c.policy, 1);
        edge(from, to, c.actions.getFirst(), Fraction.ONE, "FORWARD");
        expansions.put(from, new Expansion(from, "COMPLETE", Probability.of(Fraction.ZERO)));
      }
      if (c.emptyHit || c.blocked != null) {
        c.done = true;
        candidateIndex++;
        return;
      }
    }
    if (c.cursor.blocked() != null) {
      c.blocked = c.cursor.blocked();
      c.unavailable = c.cursor.unavailable();
      c.done = true;
      var from =
          execution(
              c.actions.size() == 2 && c.cursor.emptyState() != null ? c.cursor.emptyState() : root,
              c.policy,
              c.actions.size() == 2 && c.cursor.emptyState() != null ? 1 : 0);
      expansions.put(
          from,
          new Expansion(
              from,
              c.unavailable ? "UNAVAILABLE" : "UNSUPPORTED",
              Probability.of(c.unavailable ? Fraction.ZERO : Fraction.ONE)));
      candidateIndex++;
      return;
    }
    var outcome = c.cursor.next();
    if (outcome == null) return;
    evaluations++;
    var status = predicate.apply(outcome.state().item());
    c.emitted = c.emitted.add(outcome.probability());
    if (status == Status.MATCH) c.success = c.success.add(outcome.probability());
    else if (status != Status.NO_MATCH) c.unknown = c.unknown.add(outcome.probability());
    // Separate display budgets prevent early nonmatching rolls from hiding every success exit.
    var retained = status == Status.MATCH ? c.hitOutcomes : c.activeOutcomes;
    if ((status == Status.MATCH || status == Status.NO_MATCH)
        && (retained.containsKey(outcome.state()) || retained.size() < METHOD_DISPLAY_OUTCOMES)) {
      retained.merge(outcome.state(), outcome.probability(), Fraction::add);
      node(outcome.state());
    }
    if (c.displayed < DISPLAY_OUTCOMES) {
      var from =
          execution(
              c.actions.size() == 2 ? c.cursor.emptyState() : root, c.policy, c.actions.size() - 1);
      var to = execution(outcome.state(), c.policy, 0);
      edge(
          from,
          to,
          c.actions.getLast(),
          outcome.probability(),
          outcome.state().equals(root) ? "REPEAT" : "FORWARD");
      c.displayMass = c.displayMass.add(outcome.probability());
      c.displayed++;
      expansions.put(
          from, new Expansion(from, "PARTIAL", Probability.of(complement(c.displayMass))));
      // A proven deterministic removal makes the displayed loop/merge real, not a synthetic bucket.
      if (c.actions.size() == 2 && status == Status.NO_MATCH) {
        var empty = execution(c.cursor.emptyState(), c.policy, 1);
        edge(to, empty, c.actions.getFirst(), Fraction.ONE, "REPEAT");
        expansions.put(to, new Expansion(to, "COMPLETE", Probability.of(Fraction.ZERO)));
      }
    }
    if (c.cursor.complete()) {
      if (!c.emitted.equals(Fraction.ONE))
        throw new IllegalStateException("Renewal mass not conserved");
      if (c.displayMass.equals(Fraction.ONE)) {
        var from =
            execution(
                c.actions.size() == 2 ? c.cursor.emptyState() : root,
                c.policy,
                c.actions.size() - 1);
        expansions.put(from, new Expansion(from, "COMPLETE", Probability.of(Fraction.ZERO)));
      }
      c.done = true;
      candidateIndex++;
    }
  }

  private String node(BasicCurrencyState state) {
    String id = state.canonicalKey();
    var node = new Node(id, state.item(), predicate.apply(state.item()).name());
    var existing = nodes.putIfAbsent(id, node);
    if (existing != null && !existing.equals(node))
      throw new IllegalStateException("State digest collision");
    return id;
  }

  private String execution(BasicCurrencyState state, Policy policy, int phase) {
    String stateId = node(state), id = stateId + ":" + policy.id() + ":" + phase;
    executions.putIfAbsent(id, new Execution(id, stateId, policy.id(), phase));
    return id;
  }

  private void edge(String from, String to, WorkbenchCurrency action, Fraction mass, String kind) {
    String id = from + ":" + action + ":" + to;
    var old = edges.get(id);
    // Deterministic removal can be rediscovered at a shared state and is emitted once.
    if (old != null && action == WorkbenchCurrency.ANNULMENT) return;
    edges.put(
        id,
        new Edge(
            id,
            from,
            to,
            action.name(),
            Probability.of(old == null ? mass : old.probability().fraction().add(mass)),
            kind));
  }

  List<Recommendation> recommendations() {
    var result = new ArrayList<Recommendation>();
    for (var c : candidates) {
      if (c.cursor == null && !c.done) continue;
      var points = observations.stream().map(n -> point(c, Long.parseLong(n))).toList();
      boolean proven =
          c.rootHit || c.emptyHit || c.done && c.blocked == null && c.unknown.equals(Fraction.ZERO);
      var eventual =
          proven
              ? new Eventual(
                  "PROVEN",
                  Probability.of(
                      c.rootHit || c.emptyHit || !c.success.equals(Fraction.ZERO)
                          ? Fraction.ONE
                          : Fraction.ZERO),
                  PROOF)
              : new Eventual("UNKNOWN", null, null);
      result.add(new Recommendation(c.policy, points, eventual, method(c, points)));
    }
    return List.copyOf(result);
  }

  private MethodTransition method(Candidate c, List<Point> points) {
    if (!c.done || c.blocked != null || c.rootHit) return null;
    var exits = new ArrayList<MethodExit>();
    var omitted = new ArrayList<OmittedPoint>();
    if (c.emptyHit) {
      exits.add(
          new MethodExit(
              node(c.cursor.emptyState()),
              "HIT",
              points.stream().map(p -> new ExitPoint(p.attempts(), p.lower())).toList()));
    } else {
      for (var entry : c.hitOutcomes.entrySet()) {
        exits.add(
            new MethodExit(
                node(entry.getKey()),
                "HIT",
                points.stream()
                    .map(
                        p ->
                            new ExitPoint(
                                p.attempts(),
                                Probability.of(
                                    share(p.lower().fraction(), entry.getValue(), c.success))))
                    .toList()));
      }
      var survival = complement(c.success.add(c.unknown));
      for (var entry : c.activeOutcomes.entrySet()) {
        exits.add(
            new MethodExit(
                node(entry.getKey()),
                "ACTIVE",
                points.stream()
                    .map(
                        p -> {
                          long n = Long.parseLong(p.attempts());
                          boolean boundary = n >= c.actions.size() && n % c.actions.size() == 0;
                          return new ExitPoint(
                              p.attempts(),
                              Probability.of(
                                  boundary
                                      ? share(p.active().fraction(), entry.getValue(), survival)
                                      : Fraction.ZERO));
                        })
                    .toList()));
      }
    }
    // Zero uses remains at root; an unfinished two-action cycle remains at its real empty state.
    exits.add(
        new MethodExit(
            node(root),
            "ACTIVE",
            points.stream()
                .map(
                    p ->
                        new ExitPoint(
                            p.attempts(),
                            Probability.of(
                                p.attempts().equals("0") ? Fraction.ONE : Fraction.ZERO)))
                .toList()));
    if (c.actions.size() == 2 && !c.emptyHit) {
      exits.add(
          new MethodExit(
              node(c.cursor.emptyState()),
              "ACTIVE",
              points.stream()
                  .map(
                      p ->
                          new ExitPoint(
                              p.attempts(),
                              Probability.of(
                                  Long.parseLong(p.attempts()) % 2 == 1
                                      ? p.active().fraction()
                                      : Fraction.ZERO)))
                  .toList()));
    }
    // The same full state can be both the root and a retained active outcome.
    var merged = new LinkedHashMap<String, MethodExit>();
    for (var exit : exits) {
      String key = exit.stateId() + ":" + exit.kind();
      var old = merged.get(key);
      if (old == null) merged.put(key, exit);
      else {
        var combined = new ArrayList<ExitPoint>();
        for (int i = 0; i < points.size(); i++)
          combined.add(
              new ExitPoint(
                  points.get(i).attempts(),
                  Probability.of(
                      old.points()
                          .get(i)
                          .probability()
                          .fraction()
                          .add(exit.points().get(i).probability().fraction()))));
        merged.put(key, new MethodExit(exit.stateId(), exit.kind(), combined));
      }
    }
    for (int i = 0; i < points.size(); i++) {
      Fraction hit = Fraction.ZERO, active = Fraction.ZERO;
      for (var exit : merged.values()) {
        var mass = exit.points().get(i).probability().fraction();
        if (exit.kind().equals("HIT")) hit = hit.add(mass);
        else active = active.add(mass);
      }
      var p = points.get(i);
      omitted.add(
          new OmittedPoint(
              p.attempts(),
              Probability.of(subtract(p.lower().fraction(), hit)),
              Probability.of(subtract(p.active().fraction(), active))));
    }
    return new MethodTransition(
        node(root),
        "FIRST_GOAL_OR_OBSERVATION",
        c.actions.size(),
        PROOF,
        List.copyOf(merged.values()),
        omitted);
  }

  private static Fraction share(Fraction mass, Fraction part, Fraction total) {
    if (total.equals(Fraction.ZERO)) return Fraction.ZERO;
    return mass.multiply(part).multiply(new Fraction(total.denominator(), total.numerator()));
  }

  private static Fraction subtract(Fraction a, Fraction b) {
    return new Fraction(
        a.numerator().multiply(b.denominator()).subtract(b.numerator().multiply(a.denominator())),
        a.denominator().multiply(b.denominator()));
  }

  private Point point(Candidate c, long n) {
    RenewalFirstHit.Mass m;
    if (c.rootHit || n == 0)
      m =
          new RenewalFirstHit.Mass(
              c.rootHit ? Fraction.ONE : Fraction.ZERO,
              c.rootHit ? Fraction.ZERO : Fraction.ONE,
              Fraction.ZERO,
              Fraction.ZERO);
    else if (c.emptyHit)
      m = new RenewalFirstHit.Mass(Fraction.ONE, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);
    else if (c.unavailable) {
      boolean beforeDead = c.actions.size() == 2 && c.cursor.emptyState() != null && n == 1;
      m =
          new RenewalFirstHit.Mass(
              Fraction.ZERO,
              beforeDead ? Fraction.ONE : Fraction.ZERO,
              beforeDead ? Fraction.ZERO : Fraction.ONE,
              Fraction.ZERO);
    } else if (!c.done || c.blocked != null)
      m = new RenewalFirstHit.Mass(Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ONE);
    else if (c.unknown.equals(Fraction.ZERO)) {
      // The existing calculator reserves four working fractions. Convert this per-point output
      // budget to its arithmetic budget; elementary transition enumeration limits stay unchanged.
      var p =
          ChaosRenewalCalculator.calculate(
                  c.success,
                  false,
                  List.of(n / c.actions.size()),
                  4 * (65536 / observations.size()),
                  () -> false)
              .points()
              .getFirst();
      m = new RenewalFirstHit.Mass(p.lower(), p.active(), p.dead(), p.unresolved());
    } else
      m =
          RenewalFirstHit.at(
              c.success, c.unknown, c.actions.size(), false, false, n, 65536 / observations.size());
    return new Point(
        Long.toString(n),
        Probability.of(m.hit()),
        Probability.of(m.hit().add(m.unresolved())),
        Probability.of(m.active()),
        Probability.of(m.dead()),
        Probability.of(m.unresolved()),
        m.unresolved().equals(Fraction.ZERO)
            ? "COMPLETE"
            : m.hit().equals(Fraction.ZERO) ? "UNKNOWN" : "PARTIAL");
  }

  static List<Ranking> rankings(List<Recommendation> recommendations, List<String> observations) {
    var rankings = new ArrayList<Ranking>();
    for (int i = 0; i < observations.size(); i++) {
      final int index = i;
      var sorted =
          recommendations.stream()
              .sorted(
                  (a, b) -> {
                    int compared =
                        compare(
                            b.points().get(index).lower().fraction(),
                            a.points().get(index).lower().fraction());
                    return compared == 0 ? a.policy().id().compareTo(b.policy().id()) : compared;
                  })
              .toList();
      boolean certified =
          sorted.size() == ACTIONS.size()
              && sorted.stream().allMatch(r -> r.points().get(index).status().equals("COMPLETE"));
      var entries = new ArrayList<Rank>();
      Fraction previous = null;
      int rank = 0;
      for (int j = 0; j < sorted.size(); j++) {
        var value = sorted.get(j).points().get(i).lower().fraction();
        if (!value.equals(previous)) rank = j + 1;
        entries.add(new Rank(sorted.get(j).policy().id(), rank));
        previous = value;
      }
      rankings.add(
          new Ranking(
              observations.get(i),
              sorted.isEmpty()
                  ? "UNAVAILABLE"
                  : certified ? "CERTIFIED_WITHIN_CANDIDATES" : "PROVISIONAL",
              List.copyOf(entries)));
    }
    return List.copyOf(rankings);
  }

  private static int compare(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  private static final class Candidate {
    final Policy policy;
    final List<WorkbenchCurrency> actions;
    BasicCurrencyTransitions.RenewalCursor cursor;
    final Map<BasicCurrencyState, Fraction> hitOutcomes = new LinkedHashMap<>(),
        activeOutcomes = new LinkedHashMap<>();
    Fraction success = Fraction.ZERO,
        unknown = Fraction.ZERO,
        emitted = Fraction.ZERO,
        displayMass = Fraction.ZERO;
    int displayed;
    boolean rootHit, emptyHit, unavailable, done;
    String blocked;

    Candidate(Policy policy, List<WorkbenchCurrency> actions) {
      this.policy = policy;
      this.actions = actions;
    }
  }
}
