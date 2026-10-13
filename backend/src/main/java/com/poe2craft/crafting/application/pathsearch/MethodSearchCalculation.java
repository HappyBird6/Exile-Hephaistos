package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.DeferredItem;
import com.poe2craft.crafting.domain.pathsearch.DeferredMixtures;
import com.poe2craft.crafting.domain.pathsearch.DeferredTransitions;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy;
import com.poe2craft.crafting.domain.pathsearch.RollPartition;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/** Job-local, resumable method-depth exploration. UI buckets never key calculation states. */
public final class MethodSearchCalculation {
  public static final String VERSION = "conditional-mixture-method-search-v12";
  static final int OPEN_METHOD_WINDOW = 512;
  public static final String TEMPLATES = "applicable-single-and-two-action-cycles-v3";
  public static final List<Integer> OBSERVATIONS = List.of(100, 300, 500);

  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  public record StateView(
      String id,
      DeferredItem state,
      int depth,
      Progress progress,
      boolean expanded,
      boolean complete) {}

  public record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, Observation<DeferredItem, WorkbenchCurrency>> points) {}

  private record Key(DeferredItem state, WorkbenchCurrency action) {}

  private static final class PendingKernel extends RuntimeException {
    final Key key;

    PendingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final DeferredTransitions transitions;
  private final Function<ItemState, Progress> evaluate;
  private Function<Map<DeferredItem, Fraction>, ExactMethodRunner.Compaction<DeferredItem>> compact;
  private DeferredMixtures mixtures;
  private UnaryOperator<Map<DeferredItem, Fraction>> expandMixtures = UnaryOperator.identity();
  private final LinkedHashMap<DeferredItem, State> states = new LinkedHashMap<>();
  private final Map<String, DeferredItem> identities = new HashMap<>();
  private final LinkedHashMap<String, Method> methods = new LinkedHashMap<>();

  private record Ready(Method method, long ticket) {}

  private final ArrayDeque<Ready> runnable = new ArrayDeque<>();
  private final PriorityQueue<Ready> preferredMethods =
      new PriorityQueue<>(
          Comparator.comparingInt((Ready ready) -> ready.method().source.depth)
              .thenComparingInt(ready -> -ready.method().source.matchingEntries)
              .thenComparingInt(
                  ready -> ready.method().initial.size() + ready.method().actions.size())
              .thenComparingLong(Ready::ticket));
  private int runnableCount;
  private int methodRound;
  private long readySequence;
  private final Map<Key, List<Method>> waiting = new HashMap<>();
  private final ArrayDeque<Key> fairKernels = new ArrayDeque<>();

  private record Expansion(State state, int depth, int matches) {}

  private final PriorityQueue<Expansion> frontier =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth)
              .thenComparing(Comparator.comparingInt(Expansion::matches).reversed())
              .thenComparing(e -> e.state().id));
  private final Map<State, Set<State>> adjacency = new HashMap<>();
  private final Map<Key, Kernel<DeferredItem>> kernels = new HashMap<>();
  private final Map<Key, Key> kernelAliases = new HashMap<>();
  private final Map<Key, Integer> kernelDepth = new HashMap<>();
  private final Map<Key, Integer> kernelMethodLength = new HashMap<>();
  private final Map<DeferredTransitions.Identity, Key> kernelIdentities = new HashMap<>();
  private long sharedKernelLookups;
  private final LinkedHashMap<Key, KernelWork> pendingKernels = new LinkedHashMap<>();
  private final Map<DeferredItem, Progress> progress = new HashMap<>();
  private final Set<String> activeOmens;
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  private int retainedOutcomeLimit;
  private final int openMethodWindow;
  private int openMethods;
  private int undiscoveredStates;
  private long operations;
  private int kernelRound;
  private int retainedOutcomes;

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit) {
    this(root, evaluator, transitions, activeOmens, retainedOutcomeLimit, RollPartition.exact());
  }

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      RollPartition partition) {
    this(
        root,
        item -> {
          var result = evaluator.apply(item);
          return new Progress(
              result.status(),
              result.groups().stream()
                  .flatMap(group -> group.entries().stream())
                  .map(entry -> entry.status())
                  .toList());
        },
        transitions,
        activeOmens,
        retainedOutcomeLimit,
        true,
        OPEN_METHOD_WINDOW,
        partition);
  }

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      RollPartition partition,
      DeferredMixtures mixtures) {
    this(root, evaluator, transitions, activeOmens, retainedOutcomeLimit, partition);
    if (activeOmens.isEmpty()) {
      this.mixtures = mixtures;
      compact = mixtures::start;
      expandMixtures = mixtures::expand;
    }
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct) {
    this(
        root, evaluate, transitions, activeOmens, retainedOutcomeLimit, direct, OPEN_METHOD_WINDOW);
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct,
      int openMethodWindow) {
    this(
        root,
        evaluate,
        transitions,
        activeOmens,
        retainedOutcomeLimit,
        direct,
        openMethodWindow,
        RollPartition.exact());
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct,
      int openMethodWindow,
      RollPartition partition) {
    this.evaluate = evaluate;
    this.transitions = new DeferredTransitions(transitions, partition);
    this.activeOmens = Set.copyOf(activeOmens);
    if (retainedOutcomeLimit < 1)
      throw new IllegalArgumentException("Positive memory budget required");
    this.retainedOutcomeLimit = retainedOutcomeLimit;
    if (openMethodWindow < 1) throw new IllegalArgumentException("Positive work window required");
    this.openMethodWindow = openMethodWindow;
    state(new DeferredItem(root), 0);
  }

  public long operations() {
    return operations;
  }

  public DeferredMixtures.Metrics mixtureMetrics() {
    return mixtures == null ? new DeferredMixtures.Metrics(0, 0, 0) : mixtures.metrics();
  }

  public boolean memoryLimited() {
    return retainedOutcomes >= retainedOutcomeLimit;
  }

  public boolean complete() {
    return openMethods == 0
        && undiscoveredStates == 0
        && pendingKernels.isEmpty()
        && runnableCount == 0
        && waiting.isEmpty();
  }

  public void increaseMemoryBudget(int limit) {
    if (limit < retainedOutcomeLimit) throw new IllegalArgumentException("Budget cannot shrink");
    retainedOutcomeLimit = limit;
  }

  /** A primitive cursor emits at most one outcome per kernel turn; methods alternate fairly. */
  public void step() {
    if (memoryLimited() || complete()) return;
    operations++;
    while (!frontier.isEmpty()
        && (frontier.peek().state().expanded
            || frontier.peek().depth() != frontier.peek().state().depth)) frontier.remove();
    // Backpressure postpones frontier discovery; it never discards states/actions or marks
    // them complete. At most one action's finite template batch can overshoot this window.
    if (!frontier.isEmpty()
        && openMethods < openMethodWindow
        && (methods.isEmpty() || openMethods == 0 || operations % 3 == 1)) {
      discover(frontier.peek().state());
      return;
    }
    // Alternating queues prevents one expensive kernel from starving cheap method observations.
    if (!pendingKernels.isEmpty() && (runnableCount == 0 || operations % 2 == 0)) {
      if (Math.floorMod(kernelRound++, 4) == 3) {
        Key key;
        do {
          key = fairKernels.removeFirst();
        } while (!pendingKernels.containsKey(key));
        pendingKernels.get(key).step();
        if (pendingKernels.containsKey(key)) fairKernels.addLast(key);
      } else {
        var selected =
            pendingKernels.values().stream()
                .filter(KernelWork::ready)
                .min(
                    Comparator.comparingInt((KernelWork work) -> kernelDepth.get(work.key))
                        .thenComparingInt(work -> kernelMethodLength.get(work.key))
                        .thenComparing(KernelWork::remainingWork)
                        .thenComparingInt(work -> -matches(work.key.state())))
                .orElseThrow();
        selected.step();
      }
      return;
    }
    if (runnableCount == 0) return;
    boolean fair = Math.floorMod(methodRound++, 4) == 3;
    Ready ready;
    do {
      ready = fair ? runnable.removeFirst() : preferredMethods.remove();
    } while (!ready.method().queued || ready.ticket() != ready.method().ticket);
    var selected = ready.method();
    selected.queued = false;
    runnableCount--;
    try {
      selected.step();
      if (!selected.done) queue(selected);
    } catch (PendingKernel pending) {
      var key =
          registerKernel(
              pending.key,
              selected.source.depth,
              selected.initial.size() + selected.actions.size());
      if (kernels.containsKey(key)) queue(selected);
      else waiting.computeIfAbsent(key, unused -> new ArrayList<>()).add(selected);
    }
  }

  private Key registerKernel(Key request, int depth, int methodLength) {
    var known = kernelAliases.get(request);
    if (known != null) {
      kernelDepth.merge(known, depth, Math::min);
      kernelMethodLength.merge(known, methodLength, Math::min);
      return known;
    }
    if (mixtures != null) {
      var basis = mixtures.expand(Map.of(request.state(), Fraction.ONE));
      if (!basis.equals(Map.of(request.state(), Fraction.ONE))) {
        var dependencies = new LinkedHashMap<Key, Fraction>();
        basis.forEach(
            (state, mass) ->
                dependencies.merge(
                    registerKernel(new Key(state, request.action()), depth, methodLength),
                    mass,
                    Fraction::add));
        kernelAliases.put(request, request);
        kernelDepth.put(request, depth);
        kernelMethodLength.put(request, methodLength);
        fairKernels.addLast(request);
        pendingKernels.put(request, new MixtureKernelWork(request, dependencies));
        return request;
      }
    }
    var cursor = transitions.primitiveCursor(request.state(), request.action(), activeOmens);
    assumptions.addAll(cursor.assumptions());
    var identity = cursor.identity();
    var canonical = identity.map(kernelIdentities::get).orElse(null);
    if (canonical != null) {
      kernelDepth.merge(canonical, depth, Math::min);
      kernelMethodLength.merge(canonical, methodLength, Math::min);
      kernelAliases.put(request, canonical);
      sharedKernelLookups++;
      return canonical;
    }
    kernelAliases.put(request, request);
    kernelDepth.put(request, depth);
    kernelMethodLength.put(request, methodLength);
    identity.ifPresent(value -> kernelIdentities.put(value, request));
    fairKernels.addLast(request);
    pendingKernels.put(request, new KernelWork(request, cursor));
    return request;
  }

  public record CacheMetrics(
      int sourceActions,
      int distinctKernels,
      long sharedLookups,
      int outcomes,
      int methodInstances,
      long liveRunners,
      int completedMethods,
      long groupedExecutionInputs) {}

  public CacheMetrics cacheMetrics() {
    return new CacheMetrics(
        kernelAliases.size(),
        kernels.size() + pendingKernels.size(),
        sharedKernelLookups,
        retainedOutcomes,
        methods.size(),
        methods.values().stream().filter(m -> m.runner != null).count(),
        methods.size() - openMethods,
        methods.values().stream()
            .mapToLong(m -> m.foldedInputs + (m.runner == null ? 0 : m.runner.groupedInputs()))
            .sum());
  }

  private int matches(DeferredItem state) {
    return (int) progress(state).entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private void wake(Key key) {
    var ready = waiting.remove(key);
    if (ready != null) ready.forEach(this::queue);
  }

  private void queue(Method method) {
    if (method.queued || method.done)
      throw new IllegalStateException("Method queued twice or after completion");
    method.queued = true;
    method.ticket = ++readySequence;
    var ready = new Ready(method, method.ticket);
    runnable.addLast(ready);
    preferredMethods.add(ready);
    runnableCount++;
  }

  private State state(DeferredItem value, int depth) {
    var old = states.get(value);
    if (old != null) {
      if (depth < old.depth) {
        old.depth = depth;
        enqueue(old);
      }
      return old;
    }
    var id = value.canonicalKey();
    var collision = identities.putIfAbsent(id, value);
    if (collision != null && !collision.equals(value))
      throw new IllegalStateException("State digest collision");
    var result = new State(value, id, depth);
    states.put(value, result);
    enqueue(result);
    undiscoveredStates++;
    return result;
  }

  private void enqueue(State state) {
    frontier.add(new Expansion(state, state.depth, matches(state.value)));
  }

  private void link(State source, DeferredItem value) {
    var target = states.get(value);
    if (target == null) target = state(value, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source, unused -> new LinkedHashSet<>()).add(target);
    var queue = new ArrayDeque<State>();
    if (target.depth > source.depth + 1) {
      target.depth = source.depth + 1;
      enqueue(target);
      queue.add(target);
    }
    while (!queue.isEmpty()) {
      var from = queue.removeFirst();
      for (var to : adjacency.getOrDefault(from, Set.of()))
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          enqueue(to);
          queue.add(to);
        }
    }
  }

  private Progress progress(DeferredItem state) {
    return progress.computeIfAbsent(state, s -> evaluate.apply(s.item()));
  }

  private boolean finished(State state) {
    return state.methods.stream().allMatch(id -> methods.get(id).done);
  }

  private void discover(State source) {
    if (progress(source.value).goal() == Status.MATCH) {
      source.expanded = true;
      undiscoveredStates--;
      return;
    }
    // Applicability comes from the shared rule engine, not a fixed six-policy catalog.
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discoveryIndex < actions.size()) {
      var first = actions.get(source.discoveryIndex++);
      var cursor = transitions.primitiveCursor(source.value, first, activeOmens);
      if (cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE) return;
      add(source, List.of(first));
      if (cursor.status() == BasicCurrencyTransitions.Status.UNSUPPORTED) return;
      for (var second : BasicCurrencyTransitions.supportedActions()) {
        if (!cursor.mayApplyAfter(second)) continue;
        if (second != first) add(source, List.of(first, second));
        add(source, List.of(first), List.of(second));
      }
      return;
    }
    source.expanded = true;
    undiscoveredStates--;
  }

  private void add(State source, List<WorkbenchCurrency> actions) {
    add(source, List.of(), actions);
  }

  private void add(State source, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
    var id =
        source.id
            + ":"
            + initial.stream().map(Enum::name).reduce((a, b) -> a + "/" + b).orElse("")
            + ">"
            + actions.stream().map(Enum::name).reduce((a, b) -> a + "/" + b).orElseThrow();
    if (methods.containsKey(id)) return;
    var method = new Method(source, id, initial, actions);
    methods.put(id, method);
    source.methods.add(id);
    queue(method);
    openMethods++;
  }

  private Optional<Exit> classify(
      State source, DeferredItem outcome, boolean structuralCheckpoint) {
    var now = progress(outcome);
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    var before = progress(source.value).entries();
    if (!before.equals(now.entries())) {
      for (int i = 0; i < before.size(); i++)
        if (before.get(i) == Status.MATCH && now.entries().get(i) != Status.MATCH)
          return Optional.of(Exit.LOSS);
      return Optional.of(Exit.CHECKPOINT);
    }
    if (structuralCheckpoint
        && (source.value.item().rarity() != outcome.item().rarity()
            || source.value.item().explicits().size() != outcome.item().explicits().size()))
      return Optional.of(Exit.CHECKPOINT);
    return Optional.empty();
  }

  private Kernel<DeferredItem> kernel(DeferredItem state, WorkbenchCurrency action) {
    var key = new Key(state, action);
    var result = kernels.get(kernelAliases.getOrDefault(key, key));
    if (result == null) throw new PendingKernel(key);
    return result;
  }

  public List<StateView> states() {
    return states.values().stream()
        .map(
            s ->
                new StateView(
                    s.id,
                    s.value,
                    s.depth,
                    progress(s.value),
                    s.expanded,
                    s.expanded && finished(s)))
        .toList();
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    m.source.id,
                    m.initial,
                    m.actions,
                    m.done ? m.unresolved ? "UNSUPPORTED" : "COMPLETE" : "PARTIAL",
                    m.reason,
                    Map.copyOf(m.points)))
        .toList();
  }

  public OutcomeStrategy<DeferredItem> strategy(int cycles) {
    var graph = new LinkedHashMap<DeferredItem, OutcomeStrategy.State<DeferredItem>>();
    for (var s : states.values()) {
      var choices = new ArrayList<OutcomeStrategy.Choice<DeferredItem>>();
      boolean closed = s.expanded;
      for (var id : s.methods) {
        var method = methods.get(id);
        var point = method.points.get(cycles);
        if (point == null) {
          closed = false;
          continue;
        }
        var outcomes = new LinkedHashMap<DeferredItem, Fraction>(point.active());
        var unresolved = Fraction.ZERO;
        for (var exit : point.exits().entrySet())
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
          else
            exit.getValue().forEach((target, mass) -> outcomes.merge(target, mass, Fraction::add));
        choices.add(new OutcomeStrategy.Choice<>(id, outcomes, unresolved));
      }
      graph.put(
          s.value,
          new OutcomeStrategy.State<>(progress(s.value).goal() == Status.MATCH, closed, choices));
    }
    return new OutcomeStrategy<>(graph);
  }

  private final class State {
    final DeferredItem value;
    final String id;
    final List<String> methods = new ArrayList<>();
    final int matchingEntries;
    int depth;
    int discoveryIndex;
    boolean expanded;

    State(DeferredItem value, String id, int depth) {
      this.value = value;
      this.id = id;
      this.depth = depth;
      this.matchingEntries = matches(value);
    }
  }

  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial;
    final List<WorkbenchCurrency> actions;
    ExactMethodRunner<DeferredItem, WorkbenchCurrency> runner;
    Observation<DeferredItem, WorkbenchCurrency> previousBoundary;
    long foldedInputs;
    final Map<Integer, Observation<DeferredItem, WorkbenchCurrency>> points = new LinkedHashMap<>();
    boolean done;
    boolean queued;
    long ticket;
    boolean unresolved;
    String reason;

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = List.copyOf(initial);
      this.actions = List.copyOf(actions);
    }

    void initialize() {
      List<Function<DeferredItem, WorkbenchCurrency>> selectors =
          actions.stream()
              .<Function<DeferredItem, WorkbenchCurrency>>map(action -> s -> action)
              .toList();
      runner =
          new ExactMethodRunner<>(
              source.value,
              new Template<>(
                  TEMPLATES + ":" + id,
                  initial.stream()
                      .<Function<DeferredItem, WorkbenchCurrency>>map(action -> s -> action)
                      .toList(),
                  selectors),
              MethodSearchCalculation.this::kernel,
              outcome -> classify(source, outcome, initial.isEmpty() && actions.size() == 1),
              compact);
    }

    void step() {
      if (runner == null) initialize();
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      var point = expanded(runner.observation());
      int cycles = Math.toIntExact(point.cycles());
      if (point.active().isEmpty()) {
        for (int n : OBSERVATIONS)
          if (!points.containsKey(n)) save(n, point.atLaterAbsorbedBoundary(n));
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      if (cycles == 1
          && point.initialUses().isEmpty()
          && point.active().keySet().equals(Set.of(source.value))) {
        // Every nonabsorbed result has the exact source state and phase. The unchanged kernel
        // therefore proves renewal, including all losses and unresolved exits, not just successes.
        for (int n : OBSERVATIONS)
          save(
              n,
              com.poe2craft.crafting.domain.pathsearch.RenewalMethodObservation.repeat(
                  point, source.value, n));
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      if (previousBoundary != null) {
        var invariant =
            com.poe2craft.crafting.domain.pathsearch.RenewalMethodObservation.repeatInvariant(
                previousBoundary, point, cycles);
        if (invariant.isPresent()) {
          for (int n : OBSERVATIONS)
            if (!points.containsKey(n))
              save(
                  n,
                  com.poe2craft.crafting.domain.pathsearch.RenewalMethodObservation.repeatInvariant(
                          previousBoundary, point, n)
                      .orElseThrow());
          done = true;
          foldedInputs += runner.groupedInputs();
          runner = null;
          previousBoundary = null;
          openMethods--;
          return;
        }
      }
      if (OBSERVATIONS.contains(cycles) && !points.containsKey(cycles)) {
        save(cycles, point);
      }
      if (cycles == OBSERVATIONS.getLast()) {
        done = true;
        foldedInputs += runner.groupedInputs();
        runner = null;
        previousBoundary = null;
        openMethods--;
        return;
      }
      previousBoundary = point;
      runner.beginCycle();
    }

    Observation<DeferredItem, WorkbenchCurrency> expanded(
        Observation<DeferredItem, WorkbenchCurrency> point) {
      var exits = new EnumMap<Exit, Map<DeferredItem, Fraction>>(Exit.class);
      point.exits().forEach((kind, values) -> exits.put(kind, expandMixtures.apply(values)));
      return new Observation<>(
          point.cycles(),
          Map.copyOf(exits),
          Map.copyOf(expandMixtures.apply(point.active())),
          point.initialUses(),
          point.cycleUses());
    }

    void save(int cycles, Observation<DeferredItem, WorkbenchCurrency> point) {
      if (points.putIfAbsent(cycles, point) != null)
        throw new IllegalStateException("An earlier observation must not be overwritten");
      point.active().keySet().forEach(s -> link(source, s));
      point
          .exits()
          .forEach(
              (kind, values) -> {
                if (kind != Exit.UNRESOLVED) values.keySet().forEach(s -> link(source, s));
              });
      unresolved |= !point.exits().get(Exit.UNRESOLVED).isEmpty();
      if (unresolved) reason = "KERNEL_OR_GOAL_UNRESOLVED";
    }
  }

  private class KernelWork {
    final Key key;
    final DeferredTransitions.Cursor cursor;
    final Map<DeferredItem, Fraction> outcomes = new LinkedHashMap<>();

    KernelWork(Key key, DeferredTransitions.Cursor cursor) {
      this.key = key;
      this.cursor = cursor;
    }

    boolean ready() {
      return true;
    }

    java.math.BigInteger remainingWork() {
      return cursor.remainingOutcomes();
    }

    void step() {
      if (cursor.blocked()) {
        boolean unavailable = cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE;
        kernels.put(
            key, new Kernel<>(List.of(), unavailable ? Fraction.ZERO : Fraction.ONE, unavailable));
        pendingKernels.remove(key);
        wake(key);
        return;
      }
      var next = cursor.next();
      if (!outcomes.containsKey(next.state())) retainedOutcomes++;
      outcomes.merge(next.state(), next.probability(), Fraction::add);
      if (cursor.complete()) {
        kernels.put(
            key,
            new Kernel<>(
                outcomes.entrySet().stream()
                    .map(e -> new Outcome<>(e.getKey(), e.getValue()))
                    .toList(),
                Fraction.ZERO,
                false));
        pendingKernels.remove(key);
        wake(key);
      }
    }
  }

  /** Linear transport of one conditional mixture through already verified finite-cell kernels. */
  private final class MixtureKernelWork extends KernelWork {
    final Map<Key, Fraction> dependencies;
    Iterator<Map.Entry<Key, Fraction>> inputs;
    Map.Entry<Key, Fraction> input;
    Kernel<DeferredItem> current;
    int position;
    Boolean unavailable;
    Fraction unresolved = Fraction.ZERO;
    ExactMethodRunner.Compaction<DeferredItem> normalizer;

    MixtureKernelWork(Key key, Map<Key, Fraction> dependencies) {
      super(key, null);
      this.dependencies = Map.copyOf(dependencies);
    }

    @Override
    boolean ready() {
      return dependencies.keySet().stream().allMatch(kernels::containsKey);
    }

    @Override
    java.math.BigInteger remainingWork() {
      return java.math.BigInteger.valueOf(dependencies.size());
    }

    @Override
    void step() {
      if (!ready()) return;
      if (dependencies.size() == 1) {
        kernels.put(key, kernels.get(dependencies.keySet().iterator().next()));
        sharedKernelLookups++;
        pendingKernels.remove(key);
        wake(key);
        return;
      }
      if (inputs == null) inputs = dependencies.entrySet().iterator();
      if (current == null && inputs.hasNext()) {
        input = inputs.next();
        current = kernels.get(input.getKey());
        if (unavailable != null && unavailable != current.unavailable())
          throw new IllegalStateException("Mixture crossed an applicability boundary");
        unavailable = current.unavailable();
        unresolved = unresolved.add(input.getValue().multiply(current.unresolved()));
        position = 0;
      }
      if (current != null) {
        for (int n = 0; n < 32 && position < current.outcomes().size(); n++, position++) {
          var outcome = current.outcomes().get(position);
          if (!outcomes.containsKey(outcome.state())) retainedOutcomes++;
          outcomes.merge(
              outcome.state(), input.getValue().multiply(outcome.probability()), Fraction::add);
        }
        if (position == current.outcomes().size()) current = null;
        return;
      }
      if (normalizer == null) normalizer = mixtures.start(outcomes);
      if (!normalizer.complete()) normalizer.step(32);
      if (!normalizer.complete()) return;
      var values = normalizer.result();
      retainedOutcomes += values.size() - outcomes.size();
      kernels.put(
          key,
          new Kernel<>(
              values.entrySet().stream().map(e -> new Outcome<>(e.getKey(), e.getValue())).toList(),
              unresolved,
              Boolean.TRUE.equals(unavailable)));
      pendingKernels.remove(key);
      wake(key);
    }
  }
}
