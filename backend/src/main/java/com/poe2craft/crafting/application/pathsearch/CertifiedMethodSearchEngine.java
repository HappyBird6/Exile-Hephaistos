package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Certified class marginals. Incoming item laws remain exact retained conditional operators. */
final class CertifiedMethodSearchEngine {
  static final String VERSION = "certified-method-map-v2-shared-observations";
  private final BasicClassIndex index;
  private final CertifiedInterval.Precision precision;
  private final Map<Integer, String> identifiers = new HashMap<>();
  private final Map<Integer, MethodSearchCalculation.Progress> progressCache = new HashMap<>();
  private final List<Integer> entryOffsets = new ArrayList<>();
  private long liveEntries, savedEntries;
  private final Map<Map<Integer, CertifiedInterval>, Map<String, CertifiedInterval>> lawViews =
      new IdentityHashMap<>();
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
  private int undiscovered;

  private record Expansion(State state, int depth) {}

  private final PriorityQueue<Expansion> frontier =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth).thenComparingInt(e -> e.state().key));

  CertifiedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget,
      CertifiedInterval.Precision precision) {
    this.index = new BasicClassIndex(classes);
    this.precision = precision;
    int offset = 2;
    for (var group : evaluate.apply(start.item()).groups()) {
      offset++;
      for (var entry : group.entries()) entryOffsets.add(offset++);
    }
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

  IndexedCertifiedMethodRunner.Snapshot observation(String method, int cycles) {
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

  private String id(int key) {
    return identifiers.computeIfAbsent(key, k -> "class:" + k);
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
    return ready.isEmpty() && undiscovered == 0;
  }

  public long retainedEntries() {
    return (long) index.size() + kernels.cachedOutcomes() + liveEntries + savedEntries;
  }

  public int classCount() {
    return index.size();
  }

  record Retention(
      int classes,
      int states,
      int progressKeys,
      long adjacencyEdges,
      int queuedMethods,
      int activeRunners,
      long pending,
      long boundary,
      long exits,
      long flightOutcomes,
      long savedAccounting,
      long uniqueSavedEntries,
      int cachedKernels,
      int cachedOutcomes,
      int pendingKernels,
      long calculatedKernels,
      long sharedRequests,
      List<MethodRetention> largestMethods) {}

  record MethodRetention(String id, long cycles, long pending, long boundary, long exits) {}

  Retention retention() {
    long pending = 0, boundary = 0, exits = 0, flights = 0;
    int active = 0;
    var largest = new ArrayList<MethodRetention>();
    Set<Map<?, ?>> unique = Collections.newSetFromMap(new IdentityHashMap<>());
    for (var method : methods.values()) {
      if (method.runner != null) {
        active++;
        var retained = method.runner.retention();
        pending += retained.pending();
        boundary += retained.boundary();
        exits += retained.exits();
        flights += retained.flightOutcomes();
        largest.add(
            new MethodRetention(
                method.id,
                method.runner.cycles(),
                retained.pending(),
                retained.boundary(),
                retained.exits()));
      }
      for (var point : method.saved.values()) {
        unique.add(point.observation().active());
        unique.addAll(point.observation().exits().values());
      }
    }
    largest.sort(
        Comparator.comparingLong((MethodRetention m) -> m.pending() + m.boundary() + m.exits())
            .reversed());
    return new Retention(
        index.size(),
        states.size(),
        progressCache.size(),
        adjacency.values().stream().mapToLong(Set::size).sum(),
        ready.size(),
        active,
        pending,
        boundary,
        exits,
        flights,
        savedEntries,
        unique.stream().mapToLong(Map::size).sum(),
        kernels.cachedKernels(),
        kernels.cachedOutcomes(),
        kernels.pendingKernels(),
        kernels.calculatedKernels(),
        kernels.sharedRequests(),
        List.copyOf(largest.subList(0, Math.min(12, largest.size()))));
  }

  public CertifiedInterval.Precision precision() {
    return precision;
  }

  public boolean memoryLimited() {
    return retainedEntries() > budget;
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
    while (!frontier.isEmpty()
        && (frontier.peek().state().expanded
            || frontier.peek().depth() != frontier.peek().state().depth)) frontier.remove();
    if (!frontier.isEmpty() && (ready.isEmpty() || (operations % 4 == 0 && ready.size() < 64))) {
      discover(frontier.peek().state());
    } else if (!ready.isEmpty()) {
      var method = ready.removeFirst();
      long before = method.runner == null ? 0 : method.runner.retainedStates();
      method.step();
      liveEntries += (method.runner == null ? 0 : method.runner.retainedStates()) - before;
      if (!method.done) ready.addLast(method);
    }
  }

  private State state(int key, int depth) {
    var found = states.get(key);
    if (found != null) {
      found.depth = Math.min(found.depth, depth);
      return found;
    }
    var progress = progress(key);
    var added = new State(key, depth, progress);
    states.put(key, added);
    if (progress.goal() == Status.MATCH) added.expanded = true;
    else {
      undiscovered++;
      frontier.add(new Expansion(added, depth));
    }
    return added;
  }

  private void link(State source, int target) {
    var linked = state(target, Integer.MAX_VALUE);
    adjacency.computeIfAbsent(source.key, unused -> new LinkedHashSet<>()).add(target);
    if (linked.depth <= source.depth + 1) return;
    linked.depth = source.depth + 1;
    if (!linked.expanded) frontier.add(new Expansion(linked, linked.depth));
    var queue = new ArrayDeque<Integer>();
    queue.add(target);
    while (!queue.isEmpty()) {
      var from = states.get(queue.removeFirst());
      for (int next : adjacency.getOrDefault(from.key, Set.of())) {
        var to = states.get(next);
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          if (!to.expanded) frontier.add(new Expansion(to, to.depth));
          queue.addLast(next);
        }
      }
    }
  }

  private void discover(State source) {
    if (source.progress.goal() == Status.MATCH) {
      source.expanded = true;
      undiscovered--;
      return;
    }
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discovery == actions.size()) {
      source.expanded = true;
      undiscovered--;
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

  private MethodSearchCalculation.Progress progress(int key) {
    return progressCache.computeIfAbsent(
        key,
        value -> {
          var observations = index.key(value).observations();
          return new MethodSearchCalculation.Progress(
              observations.getFirst(), entryOffsets.stream().map(observations::get).toList());
        });
  }

  record MethodView(
      String id,
      String source,
      List<WorkbenchCurrency> initialActions,
      List<WorkbenchCurrency> actions,
      String status,
      String reason,
      Map<Integer, CertifiedMethodRunner.Observation<String, WorkbenchCurrency>> points) {}

  private Optional<Exit> classify(State source, int key, boolean structural) {
    var now = progress(key);
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

  public List<MethodSearchEngine.StateView> states() {
    return states.values().stream()
        .map(
            s ->
                new MethodSearchEngine.StateView(
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

  private Map<String, CertifiedInterval> ids(Map<Integer, CertifiedInterval> input) {
    return lawViews.computeIfAbsent(
        input,
        law -> {
          savedEntries += law.size();
          return Collections.unmodifiableMap(
              new AbstractMap<>() {
                @Override
                public int size() {
                  return law.size();
                }

                @Override
                public CertifiedInterval get(Object key) {
                  if (!(key instanceof String text) || !text.startsWith("class:")) return null;
                  try {
                    int parsed = Integer.parseInt(text.substring(6));
                    return parsed >= 0 && text.equals("class:" + parsed) ? law.get(parsed) : null;
                  } catch (NumberFormatException ignored) {
                    return null;
                  }
                }

                @Override
                public Set<Map.Entry<String, CertifiedInterval>> entrySet() {
                  return new AbstractSet<>() {
                    @Override
                    public int size() {
                      return law.size();
                    }

                    @Override
                    public Iterator<Map.Entry<String, CertifiedInterval>> iterator() {
                      var source = law.entrySet().iterator();
                      return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                          return source.hasNext();
                        }

                        @Override
                        public Map.Entry<String, CertifiedInterval> next() {
                          var next = source.next();
                          return Map.entry(id(next.getKey()), next.getValue());
                        }
                      };
                    }
                  };
                }
              });
        });
  }

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

  private final class Method {
    final State source;
    final String id;
    final List<WorkbenchCurrency> initial, actions;
    final Map<Integer, IndexedCertifiedMethodRunner.Snapshot> saved = new LinkedHashMap<>();
    final Map<Integer, CertifiedMethodRunner.Observation<String, WorkbenchCurrency>> points =
        new LinkedHashMap<>();
    IndexedCertifiedMethodRunner runner;
    final Set<Map<Integer, CertifiedInterval>> linkedLaws =
        Collections.newSetFromMap(new IdentityHashMap<>());
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
            new IndexedCertifiedMethodRunner(
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
                target -> classify(source, target, initial.isEmpty() && actions.size() == 1),
                precision);
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      if (!runner.absorbed()
          && !MethodSearchCalculation.OBSERVATIONS.contains(Math.toIntExact(runner.cycles()))) {
        runner.beginCycle();
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

    void save(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
      saved.put(cycles, snapshot);
      var point = snapshot.observation();
      var exits = new EnumMap<Exit, Map<String, CertifiedInterval>>(Exit.class);
      point
          .exits()
          .forEach(
              (kind, law) -> {
                if (linkedLaws.add(law)) law.keySet().forEach(target -> link(source, target));
                exits.put(kind, ids(law));
              });
      if (linkedLaws.add(point.active()))
        point.active().keySet().forEach(target -> link(source, target));
      unresolved |= !point.exits().get(Exit.UNRESOLVED).isEmpty();
      points.put(
          cycles,
          new CertifiedMethodRunner.Observation<>(
              point.cycles(),
              Collections.unmodifiableMap(exits),
              ids(point.active()),
              point.initialUses(),
              point.cycleUses(),
              point.mass()));
    }
  }
}
