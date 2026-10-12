package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Job-local, resumable method-depth exploration. UI buckets never key calculation states. */
public final class MethodSearchCalculation {
  public static final String VERSION = "full-state-method-search-v2";
  public static final String TEMPLATES = "applicable-single-and-two-action-cycles-v2";
  public static final List<Integer> OBSERVATIONS = List.of(100, 300, 500);

  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  public record StateView(
      String id,
      BasicCurrencyState state,
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
      Map<Integer, Observation<BasicCurrencyState, WorkbenchCurrency>> points) {}

  private record Key(BasicCurrencyState state, WorkbenchCurrency action) {}

  private static final class PendingKernel extends RuntimeException {
    final Key key;

    PendingKernel(Key key) {
      super(null, null, false, false);
      this.key = key;
    }
  }

  private final BasicCurrencyTransitions transitions;
  private final Function<ItemState, Progress> evaluate;
  private final LinkedHashMap<BasicCurrencyState, State> states = new LinkedHashMap<>();
  private final Map<String, BasicCurrencyState> identities = new HashMap<>();
  private final LinkedHashMap<String, Method> methods = new LinkedHashMap<>();
  private final List<Method> schedule = new ArrayList<>();
  private final Map<State, Set<State>> adjacency = new HashMap<>();
  private final Map<Key, Kernel<BasicCurrencyState>> kernels = new HashMap<>();
  private final LinkedHashMap<Key, KernelWork> pendingKernels = new LinkedHashMap<>();
  private final Map<BasicCurrencyState, Progress> progress = new HashMap<>();
  private final Set<String> activeOmens;
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();

  public List<WorkbenchSimulator.Assumption> assumptions() {
    return List.copyOf(assumptions);
  }

  private int retainedOutcomeLimit;
  private int openMethods;
  private int undiscoveredStates;
  private long operations;
  private int round;
  private int kernelRound;
  private int retainedOutcomes;

  public MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Evaluation> evaluator,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit) {
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
        true);
  }

  MethodSearchCalculation(
      BasicCurrencyState root,
      Function<ItemState, Progress> evaluate,
      BasicCurrencyTransitions transitions,
      Set<String> activeOmens,
      int retainedOutcomeLimit,
      boolean direct) {
    this.evaluate = evaluate;
    this.transitions = transitions;
    this.activeOmens = Set.copyOf(activeOmens);
    if (retainedOutcomeLimit < 1)
      throw new IllegalArgumentException("Positive memory budget required");
    this.retainedOutcomeLimit = retainedOutcomeLimit;
    state(root, 0);
  }

  public long operations() {
    return operations;
  }

  public boolean memoryLimited() {
    return retainedOutcomes >= retainedOutcomeLimit;
  }

  public boolean complete() {
    return openMethods == 0 && undiscoveredStates == 0 && pendingKernels.isEmpty();
  }

  public void increaseMemoryBudget(int limit) {
    if (limit < retainedOutcomeLimit) throw new IllegalArgumentException("Budget cannot shrink");
    retainedOutcomeLimit = limit;
  }

  /** A primitive cursor emits at most one outcome per kernel turn; methods alternate fairly. */
  public void step() {
    if (memoryLimited() || complete()) return;
    operations++;
    var toExpand =
        states.values().stream()
            .filter(s -> !s.expanded)
            .min(Comparator.comparingInt(s -> s.depth));
    if (toExpand.isPresent() && (schedule.isEmpty() || operations % 3 == 1)) {
      discover(toExpand.get());
      return;
    }
    // Alternating queues prevents one expensive kernel from starving cheap method observations.
    if (!pendingKernels.isEmpty() && (operations % 2 == 0)) {
      var work = new ArrayList<>(pendingKernels.values());
      var selected = work.get(Math.floorMod(kernelRound++, work.size()));
      selected.step();
      return;
    }
    if (openMethods == 0) return;
    Method selected;
    do {
      selected = schedule.get(Math.floorMod(round++, schedule.size()));
    } while (selected.done);
    try {
      selected.step();
    } catch (PendingKernel pending) {
      pendingKernels.computeIfAbsent(pending.key, KernelWork::new);
    }
  }

  private State state(BasicCurrencyState value, int depth) {
    var old = states.get(value);
    if (old != null) {
      old.depth = Math.min(old.depth, depth);
      return old;
    }
    var id = value.canonicalKey();
    var collision = identities.putIfAbsent(id, value);
    if (collision != null && !collision.equals(value))
      throw new IllegalStateException("State digest collision");
    var result = new State(value, id, depth);
    states.put(value, result);
    undiscoveredStates++;
    return result;
  }

  private void link(State source, BasicCurrencyState value) {
    var target = states.get(value);
    if (target == null) target = state(value, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source, unused -> new LinkedHashSet<>()).add(target);
    var queue = new ArrayDeque<State>();
    if (target.depth > source.depth + 1) {
      target.depth = source.depth + 1;
      queue.add(target);
    }
    while (!queue.isEmpty()) {
      var from = queue.removeFirst();
      for (var to : adjacency.getOrDefault(from, Set.of()))
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          queue.add(to);
        }
    }
  }

  private Progress progress(BasicCurrencyState state) {
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
    schedule.add(method);
    openMethods++;
  }

  private Optional<Exit> classify(
      State source, BasicCurrencyState outcome, boolean structuralCheckpoint) {
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

  private Kernel<BasicCurrencyState> kernel(BasicCurrencyState state, WorkbenchCurrency action) {
    var key = new Key(state, action);
    var result = kernels.get(key);
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

  public OutcomeStrategy<BasicCurrencyState> strategy(int cycles) {
    var graph = new LinkedHashMap<BasicCurrencyState, OutcomeStrategy.State<BasicCurrencyState>>();
    for (var s : states.values()) {
      var choices = new ArrayList<OutcomeStrategy.Choice<BasicCurrencyState>>();
      boolean closed = s.expanded;
      for (var id : s.methods) {
        var method = methods.get(id);
        var point = method.points.get(cycles);
        if (point == null) {
          closed = false;
          continue;
        }
        var outcomes = new LinkedHashMap<BasicCurrencyState, Fraction>(point.active());
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
    final BasicCurrencyState value;
    final String id;
    final List<String> methods = new ArrayList<>();
    int depth;
    int discoveryIndex;
    boolean expanded;

    State(BasicCurrencyState value, String id, int depth) {
      this.value = value;
      this.id = id;
      this.depth = depth;
    }
  }

  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial;
    final List<WorkbenchCurrency> actions;
    final ExactMethodRunner<BasicCurrencyState, WorkbenchCurrency> runner;
    final Map<Integer, Observation<BasicCurrencyState, WorkbenchCurrency>> points =
        new LinkedHashMap<>();
    boolean done;
    boolean unresolved;
    String reason;

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = List.copyOf(initial);
      this.actions = List.copyOf(actions);
      List<Function<BasicCurrencyState, WorkbenchCurrency>> selectors =
          actions.stream()
              .<Function<BasicCurrencyState, WorkbenchCurrency>>map(action -> s -> action)
              .toList();
      runner =
          new ExactMethodRunner<>(
              source.value,
              new Template<>(
                  TEMPLATES + ":" + id,
                  initial.stream()
                      .<Function<BasicCurrencyState, WorkbenchCurrency>>map(action -> s -> action)
                      .toList(),
                  selectors),
              MethodSearchCalculation.this::kernel,
              outcome -> classify(source, outcome, initial.isEmpty() && actions.size() == 1));
    }

    void step() {
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      var point = runner.observation();
      int cycles = Math.toIntExact(point.cycles());
      if (point.active().isEmpty()) {
        for (int n : OBSERVATIONS)
          save(
              n,
              new Observation<>(
                  n, point.exits(), point.active(), point.initialUses(), point.cycleUses()));
        done = true;
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
        openMethods--;
        return;
      }
      if (OBSERVATIONS.contains(cycles) && !points.containsKey(cycles)) {
        save(cycles, point);
      }
      if (cycles == OBSERVATIONS.getLast()) {
        done = true;
        openMethods--;
        return;
      }
      runner.beginCycle();
    }

    void save(int cycles, Observation<BasicCurrencyState, WorkbenchCurrency> point) {
      points.put(cycles, point);
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

  private final class KernelWork {
    final Key key;
    final BasicCurrencyTransitions.PrimitiveCursor cursor;
    final Map<BasicCurrencyState, Fraction> outcomes = new LinkedHashMap<>();

    KernelWork(Key key) {
      this.key = key;
      cursor = transitions.primitiveCursor(key.state(), key.action(), activeOmens);
      assumptions.addAll(cursor.assumptions());
    }

    void step() {
      if (cursor.blocked()) {
        boolean unavailable = cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE;
        kernels.put(
            key, new Kernel<>(List.of(), unavailable ? Fraction.ZERO : Fraction.ONE, unavailable));
        pendingKernels.remove(key);
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
      }
    }
  }
}
