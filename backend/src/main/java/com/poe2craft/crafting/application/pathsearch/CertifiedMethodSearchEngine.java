package com.poe2craft.crafting.application.pathsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.Evaluation;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/** Certified class marginals. Incoming item laws remain exact retained conditional operators. */
final class CertifiedMethodSearchEngine {
  static final String VERSION = "certified-method-map-v11-backed-pages";
  private final CertifiedLawStore laws;
  private final CertifiedMutableLawStore absorptionStore;
  private final TemporaryWorkingLaws workingLaws;
  private final CertifiedMethodRunner.WorkingMaps<Integer> workingMaps;
  private final BasicClassIndex index;
  private final CertifiedInterval.Precision precision;
  private final Map<Integer, String> identifiers = new HashMap<>();
  private final Map<Integer, MethodSearchCalculation.Progress> progressCache = new HashMap<>();
  private final List<Integer> entryOffsets = new ArrayList<>();
  private long liveEntries, savedEntries;
  private long preparationEntries;
  private Method freezeOwner;

  private record Preparation(
      int source,
      MethodSearchCalculation.Progress progress,
      List<WorkbenchCurrency> cycle,
      Map<Integer, Fraction> law,
      Fraction unresolved,
      boolean unavailable) {}

  private record Prepared(Method owner, IndexedCertifiedMethodRunner.InitialLaw law) {}

  private final Map<Preparation, Prepared> preparations = new HashMap<>();
  private final Map<Map<Integer, CertifiedInterval>, Map<String, CertifiedInterval>> lawViews =
      new IdentityHashMap<>();
  private final Map<
          Map<Integer, CertifiedInterval>, Map<MethodSearchCalculation.Progress, CertifiedInterval>>
      progressLaws = new IdentityHashMap<>();
  private long progressLawEntries, targetSummaryEntries;
  private final IndexedKernelStore kernels;
  private final Function<ItemState, Evaluation> evaluate;
  private final Map<Integer, State> states = new LinkedHashMap<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private int shallowestReadyDepth = Integer.MAX_VALUE;
  private final Map<Integer, Set<Integer>> adjacency = new HashMap<>();
  private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
  private final ConditionalMethodLaw rootLaw;
  private final int root;
  private int budget;
  private long operations;
  private boolean diagnostics;
  private final Map<String, Long> diagnosticSteps = new LinkedHashMap<>();

  void enableDiagnostics() {
    diagnostics = true;
    kernels.enableDiagnostics();
  }

  record DepthWork(
      int depth,
      long states,
      long goals,
      long expanded,
      long complete,
      long undiscoveredActions,
      long methods,
      long completedMethods) {}

  record Workload(
      long operations,
      long methods,
      long completedMethods,
      long unfinishedMethods,
      long unresolvedMethods,
      long delegates,
      long queued,
      long uniqueQueued,
      Map<String, Long> steps,
      List<DepthWork> depths,
      IndexedKernelStore.Workload kernels) {}

  /** On-demand accounting, separate from the probability graph and production UI. */
  Workload workload() {
    var levels = new TreeMap<Integer, long[]>();
    for (var state : states.values()) {
      var counts = levels.computeIfAbsent(state.depth, ignored -> new long[7]);
      counts[0]++;
      if (state.progress.goal() == Status.MATCH) counts[1]++;
      if (state.expanded) counts[2]++;
      if (state.expanded && state.methods.stream().allMatch(m -> m.done)) counts[3]++;
      if (!state.expanded)
        counts[4] += BasicCurrencyTransitions.supportedActions().size() - state.discovery;
      counts[5] += state.methods.size();
      counts[6] += state.methods.stream().filter(m -> m.done).count();
    }
    long done = methods.values().stream().filter(m -> m.done).count();
    return new Workload(
        operations,
        methods.size(),
        done,
        methods.size() - done,
        methods.values().stream().filter(m -> m.unresolved).count(),
        methods.values().stream().filter(m -> m.delegate != null).count(),
        ready.size(),
        ready.stream().map(m -> m.id).distinct().count(),
        Map.copyOf(diagnosticSteps),
        levels.entrySet().stream()
            .map(
                e -> {
                  var n = e.getValue();
                  return new DepthWork(e.getKey(), n[0], n[1], n[2], n[3], n[4], n[5], n[6]);
                })
            .toList(),
        kernels.workload());
  }

  private long graphRevision;

  record CheckpointLine(String kind, JsonNode value) {}

  record ClassCheckpoint(int id, BasicCurrencyState witness, JsonNode key) {}

  record StateCheckpoint(
      int key, int depth, int discovery, int previewDiscovery, boolean expanded) {}

  record MethodCheckpoint(
      String id,
      int source,
      List<WorkbenchCurrency> initial,
      List<WorkbenchCurrency> actions,
      boolean done,
      boolean executionDone,
      String delegate) {}

  record PointCheckpoint(
      String method,
      int cycles,
      CertifiedMethodRunner.Observation<Integer, WorkbenchCurrency> point) {}

  record QueueCheckpoint(
      long operations,
      long graphRevision,
      int budget,
      List<String> ready,
      List<int[]> frontier,
      List<int[]> previews,
      List<PreviewPattern> patterns,
      Map<Integer, Set<Integer>> adjacency,
      List<WorkbenchSimulator.Assumption> assumptions) {}

  /** Finish publication bookkeeping only; this never advances a primitive action. */
  void settleCheckpoint() {
    long before =
        methods.values().stream()
            .filter(m -> m.runner != null)
            .mapToLong(m -> m.runner.retainedStates())
            .sum();
    if (freezeOwner != null) {
      var owner = freezeOwner;
      while (owner.freezing != null) owner.step();
    }
    for (var method : methods.values()) {
      while (!method.publications.isEmpty()) method.step();
    }
    ready.removeIf(m -> m.done);
    liveEntries +=
        methods.values().stream()
                .filter(m -> m.runner != null)
                .mapToLong(m -> m.runner.retainedStates())
                .sum()
            - before;
    shallowestReadyDepth =
        ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
  }

  void checkpoint(ObjectMapper json, Consumer<CheckpointLine> sink) {
    if (freezeOwner != null || methods.values().stream().anyMatch(m -> !m.publications.isEmpty()))
      throw new IllegalStateException("Checkpoint publication is not settled");
    for (int id = 0; id < index.size(); id++)
      sink.accept(
          new CheckpointLine(
              "CLASS",
              json.valueToTree(
                  new ClassCheckpoint(id, index.witness(id), json.valueToTree(index.key(id))))));
    for (var state : states.values())
      sink.accept(
          new CheckpointLine(
              "STATE",
              json.valueToTree(
                  new StateCheckpoint(
                      state.key,
                      state.depth,
                      state.discovery,
                      state.previewDiscovery,
                      state.expanded))));
    for (var method : methods.values()) {
      sink.accept(
          new CheckpointLine(
              "METHOD",
              json.valueToTree(
                  new MethodCheckpoint(
                      method.id,
                      method.source.key,
                      method.initial,
                      method.actions,
                      method.done,
                      method.executionDone,
                      method.delegate == null ? null : method.delegate.id))));
      for (var point : method.saved.entrySet())
        sink.accept(
            new CheckpointLine(
                "POINT",
                json.valueToTree(
                    new PointCheckpoint(
                        method.id, point.getKey(), point.getValue().observation()))));
      if (method.runner != null)
        try (var lease = method.runner.leaseCursor()) {
          sink.accept(new CheckpointLine("RUNNER", json.valueToTree(lease.header())));
          lease
              .rows()
              .forEachRemaining(
                  row -> sink.accept(new CheckpointLine("ROW", json.valueToTree(row))));
          sink.accept(new CheckpointLine("END_RUNNER", json.valueToTree(method.id)));
        }
    }
    sink.accept(
        new CheckpointLine(
            "QUEUES",
            json.valueToTree(
                new QueueCheckpoint(
                    operations,
                    graphRevision,
                    budget,
                    ready.stream().map(m -> m.id).toList(),
                    frontier.stream().map(e -> new int[] {e.state.key, e.depth}).toList(),
                    previews.stream().map(e -> new int[] {e.state.key, e.depth}).toList(),
                    List.copyOf(previewedPatterns),
                    adjacency,
                    assumptions()))));
  }

  CheckpointRestore checkpointRestore(ObjectMapper json) {
    return new CheckpointRestore(json);
  }

  final class CheckpointRestore implements AutoCloseable {
    final ObjectMapper json;
    final Map<String, String> delegates = new LinkedHashMap<>();
    Method current;
    IndexedCertifiedMethodRunner.Restoration runner;
    QueueCheckpoint queues;
    boolean finished;
    int classes;

    CheckpointRestore(ObjectMapper json) {
      if (operations != 0 || index.size() != 1)
        throw new IllegalStateException("Fresh engine required");
      this.json = json;
      states.clear();
      frontier.clear();
      previews.clear();
      undiscovered = 0;
    }

    void accept(CheckpointLine line) {
      if (finished) throw new IllegalStateException("Checkpoint restore finished");
      switch (line.kind()) {
        case "CLASS" -> {
          var row = json.convertValue(line.value(), ClassCheckpoint.class);
          if (row.id() != classes++
              || index.intern(row.witness()) != row.id()
              || !json.valueToTree(index.key(row.id()))
                  .equals(
                      (a, b) ->
                          a.isNumber() && b.isNumber()
                              ? a.decimalValue().compareTo(b.decimalValue())
                              : a.equals(b) ? 0 : 1,
                      row.key()))
            throw new IllegalArgumentException("Transition class identity changed");
        }
        case "STATE" -> {
          var row = json.convertValue(line.value(), StateCheckpoint.class);
          int actions = BasicCurrencyTransitions.supportedActions().size();
          if (row.depth() < 0
              || row.discovery() < 0
              || row.discovery() > actions
              || row.previewDiscovery() < 0
              || row.previewDiscovery() > actions)
            throw new IllegalArgumentException("Invalid discovery position");
          var state = new State(row.key(), row.depth(), progress(row.key()));
          state.discovery = row.discovery();
          state.previewDiscovery = row.previewDiscovery();
          state.expanded = row.expanded();
          if (states.putIfAbsent(row.key(), state) != null)
            throw new IllegalArgumentException("Duplicate state");
          if (!state.expanded) undiscovered++;
        }
        case "METHOD" -> {
          if (runner != null) throw new IllegalArgumentException("Unfinished runner");
          var row = json.convertValue(line.value(), MethodCheckpoint.class);
          if (row.actions().isEmpty()
              || !row.id().equals(id(row.source()) + ":" + row.initial() + ">" + row.actions()))
            throw new IllegalArgumentException("Method definition identity changed");
          current =
              new Method(
                  Objects.requireNonNull(states.get(row.source())),
                  row.id(),
                  row.initial(),
                  row.actions());
          current.done = row.done();
          current.executionDone = row.executionDone();
          current.prepared = true;
          if (methods.putIfAbsent(row.id(), current) != null)
            throw new IllegalArgumentException("Duplicate method");
          current.source.methods.add(current);
          if (row.delegate() != null) delegates.put(row.id(), row.delegate());
        }
        case "POINT" -> {
          var row = json.convertValue(line.value(), PointCheckpoint.class);
          if (current == null || !current.id.equals(row.method()))
            throw new IllegalArgumentException("Point owner mismatch");
          if (!MethodSearchCalculation.OBSERVATIONS.contains(row.cycles())
              || current.saved.containsKey(row.cycles())
              || row.point().cycles() != row.cycles())
            throw new IllegalArgumentException("Invalid observation horizon");
          var method = current;
          var point =
              IndexedCertifiedMethodRunner.restoredSnapshot(
                      index,
                      method.source.key,
                      method.template(),
                      target ->
                          classify(
                              method.source,
                              target,
                              method.initial.isEmpty() && method.actions.size() == 1),
                      precision,
                      row.point())
                  .freeze(laws);
          // Derive only display summaries. Discovery queues and adjacency are restored separately.
          var parts = new ArrayList<>(point.observation().exits().values());
          parts.add(point.observation().active());
          for (var law : parts) {
            var totals = new LinkedHashMap<MethodSearchCalculation.Progress, CertifiedInterval>();
            law.forEach(
                (target, mass) ->
                    totals.merge(progress(target), mass, (a, b) -> a.add(b).probability()));
            progressLaws.put(law, Map.copyOf(totals));
            progressLawEntries += totals.size() + 1;
            current.linkedLaws.add(law);
          }
          current.publish(row.cycles(), point);
        }
        case "RUNNER" -> {
          if (current == null || runner != null)
            throw new IllegalArgumentException("Invalid runner owner");
          CertifiedMethodRunner.CursorHeader<Integer> header =
              json.convertValue(
                  line.value(),
                  json.getTypeFactory()
                      .constructParametricType(
                          CertifiedMethodRunner.CursorHeader.class, Integer.class));
          var method = current;
          runner =
              IndexedCertifiedMethodRunner.restoreCursor(
                  header,
                  kernels,
                  method.source.key,
                  method.template(),
                  target ->
                      classify(
                          method.source,
                          target,
                          method.initial.isEmpty() && method.actions.size() == 1),
                  precision,
                  absorptionStore,
                  workingMaps);
        }
        case "ROW" -> {
          if (runner == null) throw new IllegalArgumentException("Missing runner header");
          runner.accept(
              json.convertValue(
                  line.value(),
                  json.getTypeFactory()
                      .constructParametricType(
                          CertifiedMethodRunner.CursorRow.class,
                          Integer.class,
                          WorkbenchCurrency.class)));
        }
        case "END_RUNNER" -> {
          if (runner == null || !current.id.equals(line.value().asText()))
            throw new IllegalArgumentException("Runner owner mismatch");
          current.runner = runner.finish();
          runner = null;
          liveEntries += current.runner.retainedStates();
        }
        case "QUEUES" -> {
          if (queues != null) throw new IllegalArgumentException("Duplicate queues");
          queues = json.convertValue(line.value(), QueueCheckpoint.class);
        }
        default -> throw new IllegalArgumentException("Unknown engine checkpoint row");
      }
    }

    void finish() {
      if (finished || runner != null || queues == null || !states.containsKey(root))
        throw new IllegalArgumentException("Incomplete engine checkpoint");
      for (var entry : delegates.entrySet()) {
        var method = methods.get(entry.getKey());
        var owner = Objects.requireNonNull(methods.get(entry.getValue()));
        var first = kernels.request(owner.source.key, owner.initial.getFirst());
        while (!first.complete()) first.step();
        var second = kernels.request(method.source.key, method.initial.getFirst());
        while (!second.complete()) second.step();
        method.delegate = owner;
        method.equivalence =
            new IndexedCertifiedMethodRunner.InitialLaw(first)
                .equivalentTo(new IndexedCertifiedMethodRunner.InitialLaw(second));
      }
      ready.clear();
      for (var id : queues.ready()) ready.addLast(Objects.requireNonNull(methods.get(id)));
      frontier.clear();
      for (var e : queues.frontier())
        frontier.add(new Expansion(Objects.requireNonNull(states.get(e[0])), e[1]));
      previews.clear();
      for (var e : queues.previews())
        previews.add(new Expansion(Objects.requireNonNull(states.get(e[0])), e[1]));
      previewedPatterns.addAll(queues.patterns());
      adjacency.putAll(queues.adjacency());
      assumptions.addAll(queues.assumptions());
      operations = queues.operations();
      graphRevision = queues.graphRevision();
      budget = queues.budget();
      shallowestReadyDepth =
          ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
      finished = true;
    }

    public void close() {
      if (runner != null) runner.close();
    }
  }

  private int undiscovered;

  private record Expansion(State state, int depth) {}

  private record PreviewPattern(int depth, MethodSearchCalculation.Progress progress) {}

  private final Set<PreviewPattern> previewedPatterns = new HashSet<>();

  private final PriorityQueue<Expansion> frontier =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth).thenComparingInt(e -> e.state().key));
  private final PriorityQueue<Expansion> previews =
      new PriorityQueue<>(
          Comparator.comparingInt(Expansion::depth)
              .thenComparingInt(e -> -matched(e.state().progress))
              .thenComparingInt(this::previewSlots)
              .thenComparingInt(e -> e.state().key));

  private static int matched(MethodSearchCalculation.Progress progress) {
    return (int) progress.entries().stream().filter(s -> s == Status.MATCH).count();
  }

  private int previewSlots(Expansion expansion) {
    return index.witness(expansion.state().key).item().explicits().size();
  }

  CertifiedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget,
      CertifiedInterval.Precision precision) {
    this(
        start,
        classes,
        evaluate,
        budget,
        precision,
        new TemporaryLawPages(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), 8L * 1024 * 1024));
  }

  CertifiedMethodSearchEngine(
      BasicCurrencyState start,
      BasicTransitionClasses classes,
      Function<ItemState, Evaluation> evaluate,
      int budget,
      CertifiedInterval.Precision precision,
      CertifiedLawStore.PackedPages backing) {
    this.index = new BasicClassIndex(classes);
    this.precision = precision;
    laws = new CertifiedLawStore(precision.fractionalBits(), backing);
    absorptionStore = new CertifiedMutableLawStore(precision.fractionalBits(), 64);
    workingLaws =
        new TemporaryWorkingLaws(
            java.nio.file.Path.of(System.getProperty("java.io.tmpdir")),
            precision.fractionalBits(),
            128);
    workingMaps =
        new CertifiedMethodRunner.WorkingMaps<>(workingLaws::pending, workingLaws::boundary);
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

  boolean rootDirectComplete() {
    var state = states.get(root);
    return state.expanded && state.methods.stream().allMatch(m -> m.done);
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

  long graphRevision() {
    return graphRevision;
  }

  public boolean complete() {
    return ready.isEmpty() && undiscovered == 0;
  }

  public long retainedEntries() {
    return workingEntries() + kernels.cachedEntries();
  }

  private long workingEntries() {
    return (long) index.size()
        + index.profileRetainedRows()
        + liveEntries
        + laws.pageCount()
        + laws.descriptors()
        + laws.materializedRows()
        + progressLawEntries
        + targetSummaryEntries
        + previews.size()
        + previewedPatterns.size()
        + absorptionStore.residentEntries()
        + workingLaws.residentEntries()
        + preparationEntries;
  }

  CertifiedLawStore.Metrics storage() {
    return laws.metrics();
  }

  CertifiedMutableLawStore.Metrics absorptionStorage() {
    return absorptionStore.metrics();
  }

  IndexedKernelStore.CacheMetrics kernelStorage() {
    return kernels.cacheMetrics();
  }

  long freezeTransientBytes() {
    return freezeOwner == null || freezeOwner.freezing == null
        ? 0
        : freezeOwner.freezing.transientBytes();
  }

  boolean storageLimited() {
    return laws.residentPackedBytes() + absorptionStore.packedBytes() > 32L * 1024 * 1024;
  }

  void close() {
    if (freezeOwner != null && freezeOwner.freezing != null) freezeOwner.freezing.close();
    freezeOwner = null;
    ready.clear();
    preparations.clear();
    methods.clear();
    states.clear();
    frontier.clear();
    previews.clear();
    previewedPatterns.clear();
    lawViews.clear();
    progressLaws.clear();
    adjacency.clear();
    progressCache.clear();
    identifiers.clear();
    laws.close();
    absorptionStore.close();
    workingLaws.close();
    kernels.close();
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
      IndexedKernelStore.CacheMetrics kernelStorage,
      int preparationGroups,
      long preparationEntries,
      long sharedMethods,
      long freezeTransientBytes,
      CertifiedLawStore.Metrics lawStorage,
      CertifiedMutableLawStore.Metrics absorptionStorage,
      TemporaryWorkingLaws.Metrics workingStorage,
      BasicTransitionClasses.ProfileMetrics additionProfiles,
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
        kernels.cacheMetrics(),
        preparations.size(),
        preparationEntries,
        methods.values().stream().filter(m -> m.delegate != null).count(),
        freezeTransientBytes(),
        laws.metrics(),
        absorptionStore.metrics(),
        workingLaws.metrics(),
        index.profileMetrics(),
        List.copyOf(largest.subList(0, Math.min(12, largest.size()))));
  }

  public CertifiedInterval.Precision precision() {
    return precision;
  }

  public boolean memoryLimited() {
    kernels.trimToEntries(
        (int) Math.max(0, Math.min(Integer.MAX_VALUE, budget - workingEntries())));
    return retainedEntries() > budget || storageLimited();
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
    while (!previews.isEmpty()
        && (previews.peek().state().expanded
            || previews.peek().state().previewDiscovery
                == BasicCurrencyTransitions.supportedActions().size()
            || (previews.peek().state().previewDiscovery == 0
                && previewedPatterns.contains(
                    new PreviewPattern(previews.peek().depth(), previews.peek().state().progress)))
            || previews.peek().depth() != previews.peek().state().depth)) previews.remove();
    if (operations % 4 == 0
        && ready.size() < 64
        && !previews.isEmpty()
        && (ready.isEmpty() || previews.peek().depth() <= shallowestReadyDepth + 1)) {
      preview(previews.peek().state());
    } else if (!frontier.isEmpty()
        && (ready.isEmpty()
            || (operations % 4 == 0
                && ready.size() < 64
                && frontier.peek().depth() <= shallowestReadyDepth))) {
      discover(frontier.peek().state());
    } else if (!ready.isEmpty()) {
      var method = ready.removeFirst();
      if (diagnostics) diagnosticSteps.merge(method.phase(), 1L, Long::sum);
      long before = method.runner == null ? 0 : method.runner.retainedStates();
      method.step();
      if (method.done) graphRevision++;
      liveEntries += (method.runner == null ? 0 : method.runner.retainedStates()) - before;
      if (!method.done) ready.addLast(method);
      else if (method.source.depth == shallowestReadyDepth)
        shallowestReadyDepth =
            ready.stream().mapToInt(m -> m.source.depth).min().orElse(Integer.MAX_VALUE);
    }
  }

  private State state(int key, int depth) {
    var found = states.get(key);
    if (found != null) {
      if (depth < found.depth) graphRevision++;
      found.depth = Math.min(found.depth, depth);
      return found;
    }
    var progress = progress(key);
    var added = new State(key, depth, progress);
    states.put(key, added);
    graphRevision++;
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
    graphRevision++;
    if (!linked.expanded) frontier.add(new Expansion(linked, linked.depth));
    previewCandidate(linked);
    var queue = new ArrayDeque<Integer>();
    queue.add(target);
    while (!queue.isEmpty()) {
      var from = states.get(queue.removeFirst());
      for (int next : adjacency.getOrDefault(from.key, Set.of())) {
        var to = states.get(next);
        if (to.depth > from.depth + 1) {
          to.depth = from.depth + 1;
          graphRevision++;
          if (!to.expanded) frontier.add(new Expansion(to, to.depth));
          previewCandidate(to);
          queue.addLast(next);
        }
      }
    }
  }

  private void previewCandidate(State state) {
    if (state.methods.stream().anyMatch(m -> !m.done))
      shallowestReadyDepth = Math.min(shallowestReadyDepth, state.depth);
    if (!state.expanded
        && matched(state.progress) > 0
        && !previewedPatterns.contains(new PreviewPattern(state.depth, state.progress)))
      previews.add(new Expansion(state, state.depth));
  }

  /** One-layer lookahead exposes checkpoint continuations without declaring full discovery. */
  private void preview(State source) {
    if (source.previewDiscovery == 0)
      previewedPatterns.add(new PreviewPattern(source.depth, source.progress));
    var action = BasicCurrencyTransitions.supportedActions().get(source.previewDiscovery++);
    // Replacement can retain an active law for many cycles; it remains in normal discovery.
    if (action.baseAction() == CraftingAction.CHAOS) return;
    var cursor = index.replay(source.key, index.witness(source.key), action);
    assumptions.addAll(cursor.assumptions());
    if (cursor.status() != BasicCurrencyTransitions.Status.UNAVAILABLE)
      add(source, List.of(), List.of(action));
  }

  private void discover(State source) {
    if (source.progress.goal() == Status.MATCH) {
      source.expanded = true;
      graphRevision++;
      undiscovered--;
      return;
    }
    var actions = BasicCurrencyTransitions.supportedActions();
    if (source.discovery == actions.size()) {
      source.expanded = true;
      graphRevision++;
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
    graphRevision++;
    source.methods.add(method);
    ready.addLast(method);
    shallowestReadyDepth = Math.min(shallowestReadyDepth, source.depth);
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
      Map<Integer, CertifiedMethodRunner.Observation<String, WorkbenchCurrency>> points,
      Map<Integer, Map<MethodSearchCalculation.Progress, CertifiedInterval>> checkpoints,
      Map<Integer, Map<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>>
          branches) {}

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
                    Map.copyOf(m.points),
                    Map.copyOf(m.checkpoints),
                    Map.copyOf(m.branches)))
        .toList();
  }

  /** Finite completed graph adapter. Large-job cooperative strategy scheduling is separate. */
  boolean strategyFitsPreparationBudget() {
    if (!complete()) return false;
    if (states.size() > 128 || methods.size() > 1024) return false;
    for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
      long rows = 0;
      for (var method : methods.values()) {
        var point = method.points.get(cycles);
        if (point == null) throw new IllegalStateException("Completed method lacks observation");
        rows += point.active().size();
        for (var law : point.exits().values()) rows += law.size();
        if (rows > 8192) return false;
      }
    }
    return true;
  }

  CertifiedStrategyWork strategyWork() {
    if (!strategyFitsPreparationBudget())
      throw new IllegalStateException("Strategy preparation budget exceeded");
    return new CertifiedStrategyWork(
        states.keySet().stream().map(this::id).toList(),
        precision.percentDecimals(),
        this::strategy);
  }

  /** Finite completed graph adapter; callers must guard synchronous preparation. */
  CertifiedOutcomeStrategy<String> strategy(int cycles) {
    if (!complete()) throw new IllegalStateException("Enumeration incomplete");
    if (!MethodSearchCalculation.OBSERVATIONS.contains(cycles))
      throw new IllegalArgumentException("Unknown horizon");
    var graph = new LinkedHashMap<String, CertifiedOutcomeStrategy.State<String>>();
    for (var state : states.values()) {
      var choices = new ArrayList<CertifiedOutcomeStrategy.Choice<String>>();
      for (var method : state.methods) {
        var point = method.points.get(cycles);
        if (point == null) throw new IllegalStateException("Completed method lacks observation");
        var outcomes = new LinkedHashMap<>(point.active());
        var unresolved = CertifiedInterval.zero(precision.fractionalBits());
        for (var exit : point.exits().entrySet()) {
          if (exit.getKey() == Exit.UNRESOLVED)
            for (var mass : exit.getValue().values())
              unresolved = unresolved.add(mass).probability();
          else
            exit.getValue()
                .forEach(
                    (target, mass) ->
                        outcomes.merge(target, mass, (a, b) -> a.add(b).probability()));
        }
        choices.add(new CertifiedOutcomeStrategy.Choice<>(method.id, outcomes, unresolved));
      }
      graph.put(
          id(state.key),
          new CertifiedOutcomeStrategy.State<>(
              state.progress.goal() == Status.MATCH, false, choices));
    }
    return new CertifiedOutcomeStrategy<>(graph, precision.fractionalBits());
  }

  private Map<String, CertifiedInterval> ids(Map<Integer, CertifiedInterval> input) {
    return lawViews.computeIfAbsent(
        input,
        law -> {
          savedEntries += law.size();
          return new IdentifierLaw(law);
        });
  }

  private final class IdentifierLaw extends AbstractMap<String, CertifiedInterval>
      implements CertifiedLawStore.PagedLaw<String> {
    private final Map<Integer, CertifiedInterval> law;

    IdentifierLaw(Map<Integer, CertifiedInterval> law) {
      this.law = law;
    }

    @Override
    public CertifiedInterval total() {
      return CertifiedLawStore.total(law, precision.fractionalBits());
    }

    @Override
    public List<Map.Entry<String, CertifiedInterval>> page(int offset, int limit) {
      return CertifiedLawStore.page(law, offset, limit).stream()
          .map(e -> Map.entry(id(e.getKey()), e.getValue()))
          .toList();
    }

    @Override
    public Collection<CertifiedInterval> values() {
      return law.values();
    }

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
  }

  private static final class State {
    final int key;
    final MethodSearchCalculation.Progress progress;
    final List<Method> methods = new ArrayList<>();
    int depth, discovery, previewDiscovery;
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
    final Map<Integer, Map<MethodSearchCalculation.Progress, CertifiedInterval>> checkpoints =
        new LinkedHashMap<>();
    final Map<Integer, Map<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>>
        branches = new LinkedHashMap<>();
    IndexedCertifiedMethodRunner runner;
    IndexedCertifiedMethodRunner.FrozenSnapshot freezing;
    final ArrayDeque<Publication> publications = new ArrayDeque<>();
    boolean executionDone;
    IndexedKernelStore.Request preparationRequest;
    Method delegate;
    IndexedCertifiedMethodRunner.InitialEquivalence equivalence;
    boolean prepared;
    final Set<Map<Integer, CertifiedInterval>> linkedLaws =
        Collections.newSetFromMap(new IdentityHashMap<>());
    boolean done, unresolved;

    Template<Integer, WorkbenchCurrency> template() {
      return new Template<>(
          templatesVersion() + id,
          initial.stream().<Function<Integer, WorkbenchCurrency>>map(a -> s -> a).toList(),
          actions.stream().<Function<Integer, WorkbenchCurrency>>map(a -> s -> a).toList());
    }

    Method(
        State source, String id, List<WorkbenchCurrency> initial, List<WorkbenchCurrency> actions) {
      this.source = source;
      this.id = id;
      this.initial = initial;
      this.actions = actions;
    }

    void step() {
      if (!publications.isEmpty()) {
        if (publications.peekFirst().step()) publications.removeFirst();
        if (publications.isEmpty() && executionDone) done = true;
        return;
      }
      if (executionDone) {
        done = true;
        return;
      }
      if (!prepared && !initial.isEmpty()) {
        if (preparationRequest == null)
          preparationRequest = kernels.request(source.key, initial.getFirst());
        if (!preparationRequest.complete()) {
          preparationRequest.step();
          return;
        }
        var law = new IndexedCertifiedMethodRunner.InitialLaw(preparationRequest);
        preparationRequest = null;
        var key =
            new Preparation(
                source.progress.goal() == Status.NO_MATCH
                        && source.progress.entries().stream()
                            .allMatch(s -> s == Status.MATCH || s == Status.NO_MATCH)
                    ? -1
                    : source.key,
                source.progress,
                actions,
                law.outcomes(),
                law.unresolved(),
                law.unavailable());
        var shared = preparations.get(key);
        if (shared == null) {
          preparations.put(key, new Prepared(this, law));
          preparationEntries += law.outcomes().size();
        } else {
          delegate = shared.owner();
          equivalence = shared.law().equivalentTo(law);
        }
        prepared = true;
      }
      if (delegate != null) {
        for (int cycles : MethodSearchCalculation.OBSERVATIONS)
          if (!saved.containsKey(cycles) && delegate.saved.containsKey(cycles))
            save(cycles, delegate.saved.get(cycles).withEquivalentInitial(equivalence));
        executionDone = delegate.done;
        return;
      }
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
                precision,
                absorptionStore,
                workingMaps);
      if (!runner.atBoundary()) {
        runner.step();
        return;
      }
      if (!runner.absorbed()
          && !MethodSearchCalculation.OBSERVATIONS.contains(Math.toIntExact(runner.cycles()))) {
        runner.beginCycle();
        return;
      }
      if (freezing == null) {
        if (freezeOwner != null && freezeOwner != this) return;
        freezeOwner = this;
        freezing = runner.freezeSnapshot(laws);
        return;
      }
      if (!freezing.complete()) {
        freezing.step();
        return;
      }
      var snapshot = freezing.result();
      freezing.close();
      freezing = null;
      freezeOwner = null;
      var point = snapshot.observation();
      if (point.active().isEmpty()) {
        for (int cycles : MethodSearchCalculation.OBSERVATIONS)
          if (!saved.containsKey(cycles)) save(cycles, snapshot.atLaterAbsorbedBoundary(cycles));
        executionDone = true;
        runner.close();
        runner = null;
        return;
      }
      int cycles = Math.toIntExact(point.cycles());
      if (MethodSearchCalculation.OBSERVATIONS.contains(cycles)) save(cycles, snapshot);
      if (cycles == 500) {
        executionDone = true;
        runner.close();
        runner = null;
      } else runner.beginCycle();
    }

    String phase() {
      if (!publications.isEmpty()) return "PUBLICATION";
      if (executionDone) return "FINISH";
      if (!prepared && !initial.isEmpty()) return "PREPARATION";
      if (delegate != null) return "DELEGATE";
      if (runner == null) return "START";
      if (!runner.atBoundary()) return "TRANSITION";
      if (!runner.absorbed()
          && !MethodSearchCalculation.OBSERVATIONS.contains(Math.toIntExact(runner.cycles())))
        return "NEXT_CYCLE";
      if (freezing == null)
        return freezeOwner != null && freezeOwner != this ? "FREEZE_WAIT" : "FREEZE_START";
      return freezing.complete() ? "FREEZE_FINISH" : "FREEZE";
    }

    void save(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
      publications.addLast(new Publication(cycles, snapshot));
    }

    private final class Publication {
      final int cycles;
      final IndexedCertifiedMethodRunner.Snapshot snapshot;
      final Iterator<Map<Integer, CertifiedInterval>> lawsToLink;
      Iterator<Map.Entry<Integer, CertifiedInterval>> targets = Collections.emptyIterator();
      Map<Integer, CertifiedInterval> currentLaw;
      Map<MethodSearchCalculation.Progress, CertifiedInterval> totals;
      boolean linkLaw;

      Publication(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
        this.cycles = cycles;
        this.snapshot = snapshot;
        var laws = new ArrayList<>(snapshot.observation().exits().values());
        laws.add(snapshot.observation().active());
        lawsToLink = laws.iterator();
      }

      boolean step() {
        int used = 0;
        while (used < 256) {
          if (targets.hasNext()) {
            var target = targets.next();
            if (linkLaw) link(source, target.getKey());
            if (totals != null)
              totals.merge(
                  progress(target.getKey()), target.getValue(), (a, b) -> a.add(b).probability());
            used++;
            continue;
          }
          if (currentLaw != null && totals != null) {
            if (!progressLaws.containsKey(currentLaw)) {
              progressLaws.put(currentLaw, Map.copyOf(totals));
              progressLawEntries += totals.size() + 1;
            }
            totals = null;
          }
          if (!lawsToLink.hasNext()) {
            publish(cycles, snapshot);
            return true;
          }
          currentLaw = lawsToLink.next();
          linkLaw = linkedLaws.add(currentLaw);
          totals = progressLaws.containsKey(currentLaw) ? null : new LinkedHashMap<>();
          if (linkLaw || totals != null) targets = currentLaw.entrySet().iterator();
        }
        return false;
      }
    }

    void publish(int cycles, IndexedCertifiedMethodRunner.Snapshot snapshot) {
      graphRevision++;
      saved.put(cycles, snapshot);
      var point = snapshot.observation();
      var targets = new LinkedHashMap<MethodSearchCalculation.Progress, CertifiedInterval>();
      for (var kind : List.of(Exit.GOAL, Exit.CHECKPOINT, Exit.LOSS))
        progressLaws
            .get(point.exits().get(kind))
            .forEach(
                (target, mass) -> targets.merge(target, mass, (a, b) -> a.add(b).probability()));
      checkpoints.put(cycles, Map.copyOf(targets));
      targetSummaryEntries += targets.size() + 1;
      var exitsByProgress =
          new LinkedHashMap<String, Map<MethodSearchCalculation.Progress, CertifiedInterval>>();
      point.exits().forEach((kind, law) -> exitsByProgress.put(kind.name(), progressLaws.get(law)));
      exitsByProgress.put("ACTIVE", progressLaws.get(point.active()));
      branches.put(cycles, Map.copyOf(exitsByProgress));
      targetSummaryEntries += exitsByProgress.size() + 1;
      var exits = new EnumMap<Exit, Map<String, CertifiedInterval>>(Exit.class);
      point
          .exits()
          .forEach(
              (kind, law) -> {
                exits.put(kind, ids(law));
              });
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
