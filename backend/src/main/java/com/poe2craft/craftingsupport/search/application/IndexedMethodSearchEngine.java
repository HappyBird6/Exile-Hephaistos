package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator.Evaluation;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.craftingsupport.search.domain.BasicClassIndex;
import com.poe2craft.craftingsupport.search.domain.BasicTransitionClasses;
import com.poe2craft.craftingsupport.search.domain.ConditionalMethodLaw;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.craftingsupport.search.domain.IndexedKernelStore;
import com.poe2craft.craftingsupport.search.domain.IndexedMethodRunner;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 검증된 상태 동치류를 사용해 정확한 방법별 주변 확률을 계산한다.
 *
 * <p>기존 계약: Exact class marginals. Incoming item laws are explicitly bound through retained
 * observations.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "indexed-method-map-v1"} ({@code String}).
 *   <li>{@code index}: 색인 ({@code BasicClassIndex}).
 *   <li>{@code kernels}: 전이커널목록 ({@code IndexedKernelStore}).
 *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, Evaluation>}).
 *   <li>{@code states}: 상태목록 ({@code Map<Integer, State>}).
 *   <li>{@code methods}: 제작방법목록 ({@code Map<String, Method>}).
 *   <li>{@code ready}: 준비완료 ({@code ArrayDeque<Method>}).
 *   <li>{@code adjacency}: 인접목록 ({@code Map<Integer, Set<Integer>>}).
 *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
 *   <li>{@code rootLaw}: 시작점·분포 ({@code ConditionalMethodLaw}).
 *   <li>{@code root}: 시작점 ({@code int}).
 *   <li>{@code budget}: 계산예산 ({@code int}).
 *   <li>{@code operations}: 연산목록 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code IndexedMethodSearchEngine(start, classes, evaluate, budget)}: 전달받은 값으로 구성원을 초기화한다.
 *       입력: {@code start}(시작값), {@code classes}(동치류목록), {@code evaluate}(평가), {@code budget}(계산예산).
 *   <li>{@code rootLaw()}: {@code rootLaw} 값을 조회한다. 반환: {@code ConditionalMethodLaw}.
 *   <li>{@code observation(method, cycles)}: 관측값 처리에서 {@code methods.get}, {@code saved.get}을(를)
 *       사용한다. 입력: {@code method}(제작방법), {@code cycles}(주기횟수). 반환: {@code
 *       IndexedMethodRunner.Snapshot}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 *   <li>{@code classId(id)}: 동치류·식별자 처리에서 {@code states.values}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code int}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code id(key)}: 식별자의 입력에 따른 분기·결과를 처리한다. 입력: {@code key}(키). 반환: {@code String}.
 *   <li>{@code rootId()}: 시작점·식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code calculationVersion()}: {@code VERSION} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code stateVersion()}: 상태·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code templatesVersion()}: 템플릿목록·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code operations()}: {@code operations} 값을 조회한다. 반환: {@code long}.
 *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code memoryLimited()}: 메모리·제한된 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code increaseMemoryBudget(limit)}: 메모리·계산예산을(를) 한도를 늘린다. 입력: {@code limit}(한도). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
 *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
 *   <li>{@code state(key, depth)}: 현재 상태 표현을 조회한다. 입력: {@code key}(키), {@code depth}(깊이). 반환:
 *       {@code State}.
 *   <li>{@code link(source, target)}: 연결 처리에서 {@code adjacency.computeIfAbsent}, {@code
 *       queue.add}을(를) 사용한다. 입력: {@code source}(출처), {@code target}(대상).
 *   <li>{@code discover(source)}: discover 처리에서 {@code progress.goal}, {@code
 *       BasicCurrencyTransitions.supportedActions}을(를) 사용한다. 입력: {@code source}(출처).
 *   <li>{@code add(source, initial, actions)}: 값 또는 항목을 더한다. 입력: {@code source}(출처), {@code
 *       initial}(초기값), {@code actions}(행동목록).
 *   <li>{@code classify(source, key, structural)}: 완전한 전이 특성을 기준으로 상태의 동치류를 판정한다. 입력: {@code
 *       source}(출처), {@code key}(키), {@code structural}(구조적). 반환: {@code Optional<Exit>}.
 *   <li>{@code states()}: 상태목록 값을 조회·구성한다. 반환: {@code List<StateView>}.
 *   <li>{@code methods()}: 제작방법목록 값을 조회·구성한다. 반환: {@code List<MethodView>}.
 *   <li>{@code ids(input)}: 식별자목록 처리에서 {@code input.forEach}, {@code output.put}을(를) 사용한다. 입력:
 *       {@code input}(입력). 반환: {@code Map<String, Fraction>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code State}: {@code IndexedMethodSearchEngine} 내부의 상태 처리를 맡는다.
 *   <li>{@code Method}: {@code IndexedMethodSearchEngine} 내부의 제작방법 처리를 맡는다.
 * </ul>
 */
final class IndexedMethodSearchEngine implements MethodSearchEngine {
  static final String VERSION = "indexed-method-map-v1";
  private final BasicClassIndex index;
  private final IndexedKernelStore kernels;
  private final Function<ItemState, Evaluation> evaluate;
  private final Map<Integer, State> states = new LinkedHashMap<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private final Map<Integer, Set<Integer>> adjacency = new HashMap<>();
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
  private final ConditionalMethodLaw rootLaw;
  private final int root;
  private int budget;
  private long operations;

  IndexedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget) {
    this.index = new BasicClassIndex(classes);
    this.evaluate = evaluate;
    this.budget = budget;
    kernels = new IndexedKernelStore(index, 2000, budget);
    root = index.intern(start);
    rootLaw = ConditionalMethodLaw.root(index, Map.of(start, Fraction.ONE));
    state(root, 0);
  }

  ConditionalMethodLaw rootLaw() {
    return rootLaw;
  }

  IndexedMethodRunner.Snapshot observation(String method, int cycles) {
    var found = methods.get(method);
    if (found == null) throw new IllegalArgumentException("Unknown method");
    var point = found.saved.get(cycles);
    if (point == null) throw new IllegalStateException("Observation not ready");
    return point;
  }

  int classId(String id) {
    for (var state : states.values()) if (id(state.key).equals(id)) return state.key;
    throw new IllegalArgumentException("Unknown class");
  }

  private static String id(int key) {
    return "class:" + key;
  }

  public String rootId() {
    return id(root);
  }

  public String calculationVersion() {
    return VERSION;
  }

  public String stateVersion() {
    return BasicClassIndex.VERSION;
  }

  public String templatesVersion() {
    return MethodSearchCalculation.TEMPLATES;
  }

  public long operations() {
    return operations;
  }

  public boolean complete() {
    return ready.isEmpty() && states.values().stream().allMatch(s -> s.expanded);
  }

  public boolean memoryLimited() {
    return index.size() > budget;
  }

  public void increaseMemoryBudget(int limit) {
    if (limit < budget) throw new IllegalArgumentException("Cannot shrink budget");
    budget = limit;
  }

  public List<WorkbenchSimulator.Assumption> assumptions() {
    var all = new LinkedHashSet<>(assumptions);
    all.addAll(kernels.assumptions());
    return List.copyOf(all);
  }

  public void step() {
    if (complete() || memoryLimited()) return;
    operations++;
    var pending =
        states.values().stream()
            .filter(s -> !s.expanded)
            .min(Comparator.comparingInt((State s) -> s.depth).thenComparingInt(s -> s.key));
    if (pending.isPresent() && (ready.isEmpty() || (operations % 4 == 0 && ready.size() < 64))) {
      discover(pending.orElseThrow());
    } else if (!ready.isEmpty()) {
      var method = ready.removeFirst();
      method.step();
      if (!method.done) ready.addLast(method);
    }
  }

  private State state(int key, int depth) {
    var found = states.get(key);
    if (found != null) {
      found.depth = Math.min(found.depth, depth);
      return found;
    }
    var evaluation = evaluate.apply(index.witness(key).item());
    var progress =
        new MethodSearchCalculation.Progress(
            evaluation.status(),
            evaluation.groups().stream()
                .flatMap(g -> g.entries().stream())
                .map(e -> e.status())
                .toList());
    var added = new State(key, depth, progress);
    states.put(key, added);
    return added;
  }

  private void link(State source, int target) {
    var linked = state(target, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source.key, unused -> new LinkedHashSet<>()).add(target);
    if (linked.depth <= source.depth + 1) return;
    linked.depth = source.depth + 1;
    var queue = new ArrayDeque<Integer>();
    queue.add(target);
    while (!queue.isEmpty()) {
      var from = states.get(queue.removeFirst());
      for (int next : adjacency.getOrDefault(from.key, Set.of())) {
        var to = states.get(next);
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          queue.addLast(next);
        }
      }
    }
  }

  private void discover(State source) {
    if (source.progress.goal() == Status.MATCH) {
      source.expanded = true;
      return;
    }
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discovery == actions.size()) {
      source.expanded = true;
      return;
    }
    var action = actions.get(source.discovery++);
    var cursor = index.replay(source.key, index.witness(source.key), action);
    assumptions.addAll(cursor.assumptions());
    if (cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE) return;
    add(source, List.of(), List.of(action));
    if (cursor.status() == BasicCurrencyTransitions.Status.UNSUPPORTED) return;
    for (var next : actions)
      if (cursor.mayApplyAfter(next)) {
        if (next != action) add(source, List.of(), List.of(action, next));
        add(source, List.of(action), List.of(next));
      }
  }

  private void add(State source, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
    String key = id(source.key) + ":" + initial + ">" + actions;
    if (methods.containsKey(key)) return;
    var method = new Method(source, key, initial, actions);
    methods.put(key, method);
    source.methods.add(method);
    ready.addLast(method);
  }

  private Optional<Exit> classify(State source, int key, boolean structural) {
    var evaluation = evaluate.apply(index.witness(key).item());
    var now =
        new MethodSearchCalculation.Progress(
            evaluation.status(),
            evaluation.groups().stream()
                .flatMap(g -> g.entries().stream())
                .map(e -> e.status())
                .toList());
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    if (!now.entries().equals(source.progress.entries())) {
      for (int i = 0; i < now.entries().size(); i++)
        if (source.progress.entries().get(i) == Status.MATCH
            && now.entries().get(i) != Status.MATCH) return Optional.of(Exit.LOSS);
      return Optional.of(Exit.CHECKPOINT);
    }
    var before = index.witness(source.key).item();
    var after = index.witness(key).item();
    if (structural
        && (before.rarity() != after.rarity()
            || before.explicits().size() != after.explicits().size()))
      return Optional.of(Exit.CHECKPOINT);
    return Optional.empty();
  }

  public List<StateView> states() {
    return states.values().stream()
        .map(
            s ->
                new StateView(
                    id(s.key),
                    new DeferredItem(index.witness(s.key)),
                    s.depth,
                    s.progress,
                    s.expanded,
                    s.expanded && s.methods.stream().allMatch(m -> m.done),
                    Optional.empty()))
        .toList();
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    id(m.source.key),
                    m.initial,
                    m.actions,
                    m.done ? m.unresolved ? "UNSUPPORTED" : "COMPLETE" : "PARTIAL",
                    null,
                    Map.copyOf(m.points)))
        .toList();
  }

  private static Map<String, Fraction> ids(Map<Integer, Fraction> input) {
    var output = new LinkedHashMap<String, Fraction>();
    input.forEach((key, mass) -> output.put(id(key), mass));
    return Collections.unmodifiableMap(output);
  }

  /**
   * {@code IndexedMethodSearchEngine} 내부의 상태 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code key}: 키 ({@code int}).
   *   <li>{@code progress}: 진행상황 ({@code MethodSearchCalculation.Progress}).
   *   <li>{@code methods}: 제작방법목록 ({@code List<Method>}).
   *   <li>{@code depth}: 깊이 ({@code int}).
   *   <li>{@code discovery}: 발견 ({@code int}).
   *   <li>{@code expanded}: 확장된 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code State(key, depth, progress)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code key}(키), {@code
   *       depth}(깊이), {@code progress}(진행상황).
   * </ul>
   */
  private static final class State {
    final int key;
    final MethodSearchCalculation.Progress progress;
    final List<Method> methods = new ArrayList<>();
    int depth, discovery;
    boolean expanded;

    State(int key, int depth, MethodSearchCalculation.Progress progress) {
      this.key = key;
      this.depth = depth;
      this.progress = progress;
    }
  }

  /**
   * {@code IndexedMethodSearchEngine} 내부의 제작방법 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code State}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code initial}: 초기값 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code saved}: 저장된 ({@code Map<Integer, IndexedMethodRunner.Snapshot>}).
   *   <li>{@code points}: 지점목록 ({@code Map<Integer, Observation<String, WorkbenchCurrency>>}).
   *   <li>{@code runner}: 실행기 ({@code IndexedMethodRunner}).
   *   <li>{@code done}: 완료여부 ({@code boolean}).
   *   <li>{@code unresolved}: 미해결 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Method(source, id, initial, actions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       source}(출처), {@code id}(식별자), {@code initial}(초기값), {@code actions}(행동목록).
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code save(cycles, snapshot)}: 현재 값을 저장 경계에 기록한다. 입력: {@code cycles}(주기횟수), {@code
   *       snapshot}(스냅샷).
   * </ul>
   */
  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial, actions;
    final Map<Integer, IndexedMethodRunner.Snapshot> saved = new LinkedHashMap<>();
    final Map<Integer, Observation<String, WorkbenchCurrency>> points = new LinkedHashMap<>();
    IndexedMethodRunner runner;
    boolean done, unresolved;

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = initial;
      this.actions = actions;
    }

    void step() {
      if (runner == null)
        runner =
            new IndexedMethodRunner(
                kernels,
                source.key,
                new Template<>(
                    templatesVersion() + id,
                    initial.stream()
                        .<Function<Integer, WorkbenchCurrency>>map(a -> s -> a)
                        .toList(),
                    actions.stream()
                        .<Function<Integer, WorkbenchCurrency>>map(a -> s -> a)
                        .toList()),
                target -> classify(source, target, initial.isEmpty() && actions.size() == 1));
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      var snapshot = runner.snapshot();
      var point = snapshot.observation();
      if (point.active().isEmpty()) {
        for (int cycles : MethodSearchCalculation.OBSERVATIONS)
          if (!saved.containsKey(cycles)) save(cycles, snapshot.atLaterAbsorbedBoundary(cycles));
        done = true;
        runner = null;
        return;
      }
      int cycles = Math.toIntExact(point.cycles());
      if (MethodSearchCalculation.OBSERVATIONS.contains(cycles)) save(cycles, snapshot);
      if (cycles == 500) {
        done = true;
        runner = null;
      } else runner.beginCycle();
    }

    void save(int cycles, IndexedMethodRunner.Snapshot snapshot) {
      saved.put(cycles, snapshot);
      var point = snapshot.observation();
      var exits = new EnumMap<Exit, Map<String, Fraction>>(Exit.class);
      point
          .exits()
          .forEach(
              (kind, law) -> {
                law.keySet().forEach(target -> link(source, target));
                exits.put(kind, ids(law));
              });
      point.active().keySet().forEach(target -> link(source, target));
      unresolved |= !point.exits().get(Exit.UNRESOLVED).isEmpty();
      points.put(
          cycles,
          new Observation<>(
              point.cycles(),
              Collections.unmodifiableMap(exits),
              ids(point.active()),
              point.initialUses(),
              point.cycleUses()));
    }
  }
}
