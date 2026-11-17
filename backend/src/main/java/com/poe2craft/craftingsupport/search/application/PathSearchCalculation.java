package com.poe2craft.craftingsupport.search.application;

import static com.poe2craft.craftingsupport.search.application.PathSearchProtocol.*;
import static com.poe2craft.craftingsupport.search.domain.RenewalFirstHit.complement;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.path.application.ChaosRenewalCalculator;
import com.poe2craft.craftingsupport.search.domain.RenewalFirstHit;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 작업 잠금 아래에서 정확한 경로 탐색을 이어 계산한다.
 *
 * <p>기존 계약: One job's exact streaming continuation. Mutated only while holding its job monitor.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CANDIDATES}: 후보목록; 초기값 {@code "solar-one-explicit-candidates-v1"} ({@code String}).
 *   <li>{@code PROOF}: 증명; 초기값 {@code "single-explicit-full-return-numeric-v1"} ({@code String}).
 *   <li>{@code ACTIONS}: 행동목록 ({@code List<List<WorkbenchCurrency>>}).
 *   <li>{@code DISPLAY_OUTCOMES}: 결과목록; 초기값 {@code 24} ({@code int}).
 *   <li>{@code METHOD_DISPLAY_OUTCOMES}: 결과목록; 초기값 {@code 6} ({@code int}).
 *   <li>{@code root}: 시작점 ({@code BasicCurrencyState}).
 *   <li>{@code predicate}: 판정조건 ({@code Function<ItemState, Status>}).
 *   <li>{@code observations}: 관측값목록 ({@code List<String>}).
 *   <li>{@code candidates}: 후보목록 ({@code List<Candidate>}).
 *   <li>{@code nodes}: 노드목록 ({@code Map<String, Node>}).
 *   <li>{@code executions}: 실행목록 ({@code Map<String, Execution>}).
 *   <li>{@code edges}: 간선목록 ({@code Map<String, Edge>}).
 *   <li>{@code expansions}: expansions ({@code Map<String, Expansion>}).
 *   <li>{@code transitions}: 전이목록 ({@code BasicCurrencyTransitions}).
 *   <li>{@code candidateIndex}: 후보·색인 ({@code int}).
 *   <li>{@code evaluations}: 평가목록 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PathSearchCalculation(root, predicate, observations, transitions)}: 전달받은 값으로 구성원을
 *       초기화한다. 입력: {@code root}(시작점), {@code predicate}(판정조건), {@code observations}(관측값목록), {@code
 *       transitions}(전이목록).
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code outputLimited()}: output·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code node(state)}: 노드 처리에서 {@code state.canonicalKey}, {@code state.item}을(를) 사용한다. 입력:
 *       {@code state}(상태). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code execution(state, policy, phase)}: 실행 처리에서 {@code policy.id}, {@code
 *       executions.putIfAbsent}을(를) 사용한다. 입력: {@code state}(상태), {@code policy}(정책), {@code
 *       phase}(단계). 반환: {@code String}.
 *   <li>{@code edge(from, to, action, mass, kind)}: 간선 처리에서 {@code edges.get}, {@code
 *       edges.put}을(를) 사용한다. 입력: {@code from}(원본), {@code to}(대상), {@code action}(행동), {@code
 *       mass}(확률질량), {@code kind}(종류).
 *   <li>{@code recommendations()}: 추천목록 값을 조회·구성한다. 반환: {@code List<Recommendation>}.
 *   <li>{@code method(c, points)}: 제작방법 처리에서 {@code exits.add}, {@code cursor.emptyState}을(를) 사용한다.
 *       입력: {@code c}(c), {@code points}(지점목록). 반환: {@code MethodTransition}.
 *   <li>{@code share(mass, part, total)}: share 처리에서 {@code total.equals}, {@code
 *       mass.multiply}을(를) 사용한다. 입력: {@code mass}(확률질량), {@code part}(part), {@code total}(합계). 반환:
 *       {@code Fraction}.
 *   <li>{@code subtract(a, b)}: 두 값의 차를 계산한다. 입력: {@code a}(a), {@code b}(b). 반환: {@code Fraction}.
 *   <li>{@code point(c, n)}: 지점 처리에서 {@code RenewalFirstHit.Mass}, {@code actions.size}을(를) 사용한다.
 *       입력: {@code c}(c), {@code n}(n). 반환: {@code Point}.
 *   <li>{@code rankings(recommendations, observations)}: rankings 처리에서 {@code observations.size},
 *       {@code recommendations.stream}을(를) 사용한다. 입력: {@code recommendations}(추천목록), {@code
 *       observations}(관측값목록). 반환: {@code List<Ranking>}.
 *   <li>{@code compare(a, b)}: 비교 처리에서 {@code a.numerator}, {@code b.denominator}을(를) 사용한다. 입력:
 *       {@code a}(a), {@code b}(b). 반환: {@code int}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Candidate}: {@code PathSearchCalculation} 내부의 후보 처리를 맡는다.
 * </ul>
 */
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
    boolean methodOutcome =
        (status == Status.MATCH || status == Status.NO_MATCH)
            && (retained.containsKey(outcome.state()) || retained.size() < METHOD_DISPLAY_OUTCOMES);
    if (methodOutcome) {
      retained.merge(outcome.state(), outcome.probability(), Fraction::add);
    }
    // Retain the real enumerated transition for every method exit, including outcomes after
    // the legacy display budget. A node alone cannot establish recovery ancestry or phase.
    // The union is emitted once, so overlapping display selections do not double-count mass.
    if (c.displayed < DISPLAY_OUTCOMES || methodOutcome) {
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

  /**
   * {@code PathSearchCalculation} 내부의 후보 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code policy}: 정책 ({@code Policy}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code cursor}: 진행커서 ({@code BasicCurrencyTransitions.RenewalCursor}).
   *   <li>{@code hitOutcomes}: 목표도달·결과목록 ({@code Map<BasicCurrencyState, Fraction>}).
   *   <li>{@code activeOutcomes}: 활성·결과목록 ({@code Map<BasicCurrencyState, Fraction>}).
   *   <li>{@code success}: 성공 ({@code Fraction}).
   *   <li>{@code unknown}: 미확정 ({@code Fraction}).
   *   <li>{@code emitted}: 내보낸 ({@code Fraction}).
   *   <li>{@code displayMass}: 표시·확률질량 ({@code Fraction}).
   *   <li>{@code displayed}: displayed ({@code int}).
   *   <li>{@code rootHit}: 시작점·목표도달 ({@code boolean}).
   *   <li>{@code emptyHit}: 빈값·목표도달 ({@code boolean}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   *   <li>{@code done}: 완료여부 ({@code boolean}).
   *   <li>{@code blocked}: 차단 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Candidate(policy, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code policy}(정책), {@code
   *       actions}(행동목록).
   * </ul>
   */
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
