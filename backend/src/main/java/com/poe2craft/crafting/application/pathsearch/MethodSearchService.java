package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.MethodSearchProtocol.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Probability;
import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.BasicTransitionClasses;
import com.poe2craft.crafting.domain.pathsearch.DeferredItem;
import com.poe2craft.crafting.domain.pathsearch.DeferredMixtures;
import com.poe2craft.crafting.domain.pathsearch.OutcomeStrategy;
import com.poe2craft.item.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Process-local v2 jobs. A generation token guards each publication and queued continuation. */
public final class MethodSearchService implements AutoCloseable {
  private static final int PAGE_SIZE = 12, MAX_JOBS = 8, MAX_REVISIONS = 4, MAX_COMMANDS = 64;
  private final GoalFilterService goals;
  private final BasicCurrencyTransitions transitions;
  private final Map<String, ItemCatalog> catalogs = new LinkedHashMap<>();
  private final Map<String, BasicCurrencyTransitions> models = new LinkedHashMap<>();
  private final ObjectMapper json;
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final Map<String, Job> jobs = new LinkedHashMap<>();

  public MethodSearchService(
      List<ItemCatalog> catalogs, GoalFilterService goals, String identity, ObjectMapper json) {
    this(
        catalogs.stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow(),
        goals,
        identity,
        json);
    register(catalogs, identity);
  }

  public MethodSearchService(
      List<ItemCatalog> catalogs,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget) {
    this(catalogs.getFirst(), goals, identity, json, clock, executor, runBudget);
    register(catalogs, identity);
  }

  private void register(List<ItemCatalog> catalogs, String identity) {
    for (var entry : catalogs) {
      this.catalogs.put(entry.base().id(), entry);
      models.computeIfAbsent(
          entry.base().id(), ignored -> BasicCurrencyTransitions.forMethodSearch(entry, identity));
    }
  }

  public MethodSearchService(
      ItemCatalog catalog, GoalFilterService goals, String identity, ObjectMapper json) {
    this(
        catalog,
        goals,
        identity,
        json,
        Clock.systemUTC(),
        new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(MAX_JOBS),
            r -> {
              var t = new Thread(r, "crafting-method-search");
              t.setDaemon(true);
              return t;
            }),
        12000);
  }

  public MethodSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget) {
    this.goals = goals;
    this.transitions = BasicCurrencyTransitions.forMethodSearch(catalog, identity);
    catalogs.put(catalog.base().id(), catalog);
    models.put(catalog.base().id(), transitions);
    this.json = json.copy();
    this.clock = clock;
    this.executor = executor;
    if (runBudget < 1) throw new IllegalArgumentException("Positive run budget required");
    this.runBudget = runBudget;
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public BasicCurrencyState.Provenance provenance(String baseItemId) {
    var model = models.get(baseItemId);
    if (model == null) throw new Rejected(422, "UNSUPPORTED_BASE");
    return model.provenance();
  }

  public Snapshot create(String owner, Create input) {
    Create request;
    try {
      request = json.readValue(json.writeValueAsBytes(input), Create.class);
    } catch (java.io.IOException ex) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.clientRequestId().length() > 256
        || request.start() == null
        || request.goal() == null
        || request.activeOmens() == null) throw new Rejected(422, "INVALID_REQUEST");
    var catalog = catalogs.get(request.start().item().baseItemId());
    if (catalog == null) throw new Rejected(422, "UNSUPPORTED_BASE");
    var transitions = models.get(catalog.base().id());
    if (!transitions.provenance().equals(request.start().provenance()))
      throw new Rejected(422, "RULESET_IDENTITY_MISMATCH");
    if (!request.start().item().baseItemId().equals(catalog.base().id()))
      throw new Rejected(422, "UNSUPPORTED_BASE");
    if (!new ItemStateValidator(catalog).validate(request.start().item()).isEmpty())
      throw new Rejected(422, "INVALID_ITEM");
    request.activeOmens().forEach(WorkbenchOmen::fromId);
    var evaluator = goals.compileEvaluation(request.start().item(), request.goal());
    synchronized (jobs) {
      expire();
      for (var job : jobs.values())
        if (job.owner.equals(owner)
            && job.request.clientRequestId().equals(request.clientRequestId())) {
          if (!job.request.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
          try (var ignored = job.acquire()) {
            return job.snapshot;
          }
        }
      if (jobs.size() >= MAX_JOBS) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var partition = goals.rollPartition(request.start().item(), request.goal());
      var mixtures =
          new DeferredMixtures(
              new BasicTransitionClasses(catalog, transitions, partition, evaluator));
      var job =
          new Job(
              owner,
              request,
              new MethodSearchCalculation(
                  request.start(),
                  evaluator,
                  transitions,
                  Set.copyOf(request.activeOmens()),
                  100000,
                  partition,
                  mixtures));
      jobs.put(job.id, job);
      try (var ignored = job.acquire()) {
        publish(job);
        var accepted = job.snapshot;
        submit(job);
        return accepted;
      }
    }
  }

  public Snapshot get(String owner, String id) {
    return job(owner, id).snapshot;
  }

  public Snapshot mutate(String owner, String id, Mutation command) {
    if (command == null
        || !Integer.valueOf(2).equals(command.version())
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.expectedRevision() == null
        || command.operation() == null
        || !Set.of("CANCEL", "RESUME").contains(command.operation()))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var old = job.commands.get(command.commandId());
      if (old != null) {
        if (!old.command.equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.response;
      }
      if (job.commands.size() >= MAX_COMMANDS) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      if (command.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (command.operation().equals("CANCEL")) {
        if (Set.of("QUEUED", "RUNNING", "PAUSED").contains(job.status)) {
          job.generation++;
          job.status = "CANCELLED";
          publish(job);
        }
      } else {
        if (!job.snapshot.resumable()) throw new Rejected(409, "JOB_NOT_RESUMABLE");
        if (command.retainedOutcomeLimit() != null) {
          if (command.retainedOutcomeLimit() < 100000 || command.retainedOutcomeLimit() > 1000000)
            throw new Rejected(422, "INVALID_BUDGET");
          try {
            job.calculation.increaseMemoryBudget(command.retainedOutcomeLimit());
          } catch (IllegalArgumentException ex) {
            // A previously enlarged job cannot shrink its retained-outcome allowance.
            throw new Rejected(422, "INVALID_BUDGET");
          }
        }
        job.generation++;
        job.status = "QUEUED";
        job.reason = null;
        publish(job);
        submit(job);
      }
      job.commands.put(command.commandId(), new Command(command, job.snapshot));
      return job.snapshot;
    }
  }

  public Graph graph(String owner, String id, long revision, String cursor) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var saved = revision(job, revision);
      return saved.graph(position(saved, cursor, "graph"));
    }
  }

  public Distribution distribution(
      String owner, String id, long revision, String methodId, int cycles, String cursor) {
    var job = job(owner, id);
    try (var ignored = job.acquire()) {
      var saved = revision(job, revision);
      var method =
          saved.methods.stream()
              .filter(m -> m.id().equals(methodId))
              .findFirst()
              .orElseThrow(() -> new Rejected(422, "INVALID_METHOD"));
      var point = method.points().get(cycles);
      if (point == null) throw new Rejected(409, "OBSERVATION_NOT_READY");
      String scope = "distribution:" + methodId + ":" + cycles;
      var offset = bigPosition(saved, cursor, scope);
      var rows = new ArrayList<ConditionalState>();
      var cells = new ArrayList<DistributionCell>();
      var unresolved = Fraction.ZERO;
      for (var exit : point.exits().entrySet()) {
        if (exit.getKey()
            == com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit.UNRESOLVED)
          for (var mass : exit.getValue().values()) unresolved = unresolved.add(mass);
        else
          exit.getValue()
              .forEach((s, mass) -> cells.add(new DistributionCell(s, exit.getKey().name(), mass)));
      }
      point.active().forEach((s, mass) -> cells.add(new DistributionCell(s, "ACTIVE", mass)));
      cells.sort(
          Comparator.comparing((DistributionCell cell) -> cell.state().canonicalKey())
              .thenComparing(DistributionCell::kind));
      var total =
          cells.stream()
              .map(cell -> cell.state().cardinality())
              .reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
      if (offset.compareTo(total) > 0) throw new Rejected(422, "INVALID_CURSOR");
      var skip = offset;
      for (var cell : cells) {
        var size = cell.state().cardinality();
        if (skip.compareTo(size) >= 0) {
          skip = skip.subtract(size);
          continue;
        }
        while (skip.compareTo(size) < 0 && rows.size() < 48) {
          var concrete = cell.state().concrete(skip);
          var mass = cell.mass().multiply(cell.state().concreteProbability(skip));
          rows.add(
              new ConditionalState(concrete.canonicalKey(), concrete.item(), cell.kind(), p(mass)));
          skip = skip.add(java.math.BigInteger.ONE);
        }
        if (rows.size() == 48) break;
        skip = java.math.BigInteger.ZERO;
      }
      var end = offset.add(java.math.BigInteger.valueOf(rows.size()));
      return new Distribution(
          2,
          id,
          revision,
          methodId,
          cycles,
          List.copyOf(rows),
          p(unresolved),
          end.compareTo(total) < 0 ? saved.cursor(scope, end) : null);
    }
  }

  private int position(Revision saved, String cursor, String scope) {
    var offset = bigPosition(saved, cursor, scope);
    if (offset.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
      throw new Rejected(422, "INVALID_CURSOR");
    return offset.intValueExact();
  }

  private java.math.BigInteger bigPosition(Revision saved, String cursor, String scope) {
    if (cursor == null) return java.math.BigInteger.ZERO;
    var position = saved.cursors.get(cursor);
    if (position == null || !position.scope.equals(scope))
      throw new Rejected(422, "INVALID_CURSOR");
    return position.offset;
  }

  private record DistributionCell(DeferredItem state, String kind, Fraction mass) {}

  private Revision revision(Job job, long revision) {
    var saved = job.revisions.get(revision);
    if (saved == null) throw new Rejected(410, "REVISION_EXPIRED");
    return saved;
  }

  private void submit(Job job) {
    long generation = job.generation;
    try {
      executor.execute(() -> run(job, generation));
    } catch (RejectedExecutionException ex) {
      job.status = "PAUSED";
      job.reason = "WORKER_CAPACITY";
      publish(job);
    }
  }

  private void run(Job job, long generation) {
    int used = 0, lastPublishedWork = 0;
    while (used < runBudget) {
      try (var ignored = job.acquire()) {
        if (generation != job.generation || !Set.of("QUEUED", "RUNNING").contains(job.status))
          return;
        job.status = "RUNNING";
        try {
          // Each slice publishes a coherent revision; queued controls acquire the fair lock between
          // slices.
          long until = System.nanoTime() + 20_000_000L;
          int steps = 0;
          while (steps < 128
              && used < runBudget
              && System.nanoTime() < until
              && !job.calculation.complete()
              && !job.calculation.memoryLimited()) {
            job.calculation.step();
            used++;
            steps++;
          }
          if (job.calculation.complete()) {
            boolean exact = true;
            for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
              var solver = job.solvers.computeIfAbsent(cycles, n -> job.calculation.strategy(n));
              solver.refine();
              solver.refineExactPolicy();
              solver.refineAlmostSure();
              used++;
              exact &= solver.bounds().values().stream().allMatch(OutcomeStrategy.Bounds::exact);
            }
            if (exact) {
              job.status = "COMPLETED";
              publish(job);
              return;
            }
          }
          if (job.calculation.memoryLimited()) {
            job.status = "PAUSED";
            job.reason = "MEMORY_BUDGET";
            publish(job);
            return;
          }
          if (used >= runBudget) {
            job.status = "PAUSED";
            job.reason = "COMPUTE_BUDGET";
            publish(job);
            return;
          }
          if (used - lastPublishedWork >= 512) {
            lastPublishedWork = used;
            publish(job);
          }
        } catch (RuntimeException ex) {
          job.status = "FAILED";
          job.reason = "CALCULATION_FAILED";
          publish(job);
          return;
        }
      }
      Thread.yield();
    }
  }

  static List<Local> localRecommendations(
      List<MethodSearchCalculation.StateView> states,
      Map<String, List<MethodSearchCalculation.MethodView>> bySource) {
    var locals = new ArrayList<Local>();
    for (var source : states)
      if (source.expanded()) {
        var methods = bySource.getOrDefault(source.id(), List.of());
        for (int cycles : MethodSearchCalculation.OBSERVATIONS) {
          Fraction best = null;
          var ids = new ArrayList<String>();
          for (var method : methods) {
            var observation = method.points().get(cycles);
            if (observation == null) continue;
            var value =
                observation
                    .exits()
                    .get(com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit.GOAL)
                    .values()
                    .stream()
                    .reduce(Fraction.ZERO, Fraction::add);
            if (best == null || compare(value, best) > 0) {
              best = value;
              ids.clear();
              ids.add(method.id());
            } else if (compare(value, best) == 0) ids.add(method.id());
          }
          if (best != null) {
            var selected = best;
            boolean certified =
                methods.stream()
                    .allMatch(
                        method -> {
                          var observation = method.points().get(cycles);
                          if (observation == null) return false;
                          var goal =
                              observation
                                  .exits()
                                  .get(
                                      com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner
                                          .Exit.GOAL)
                                  .values()
                                  .stream()
                                  .reduce(Fraction.ZERO, Fraction::add);
                          var unknown =
                              observation
                                  .exits()
                                  .get(
                                      com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner
                                          .Exit.UNRESOLVED)
                                  .values()
                                  .stream()
                                  .reduce(Fraction.ZERO, Fraction::add);
                          return compare(selected, goal.add(unknown)) >= 0;
                        });
            if (certified) locals.add(new Local(source.id(), cycles, List.copyOf(ids)));
          }
        }
      }
    return List.copyOf(locals);
  }

  private void publish(Job job) {
    job.revision++;
    var saved = new Revision(job);
    job.revisions.put(job.revision, saved);
    while (job.revisions.size() > MAX_REVISIONS)
      job.revisions.remove(job.revisions.keySet().iterator().next());
    var locals = localRecommendations(saved.states, saved.bySource);
    var strategies = new ArrayList<Strategy>();
    job.solvers.forEach(
        (cycles, solver) -> {
          solver
              .bounds()
              .forEach(
                  (state, value) -> {
                    var ids =
                        solver.certifiedAlternatives(state).stream()
                            .map(OutcomeStrategy.Recommendation::methodId)
                            .toList();
                    strategies.add(
                        new Strategy(
                            saved.byState.get(state).id(),
                            cycles,
                            value.exact() ? "EXACT" : "BOUNDS",
                            p(value.lower()),
                            p(value.upper()),
                            ids));
                  });
        });
    job.snapshot =
        new Snapshot(
            2,
            job.id,
            job.request.clientRequestId(),
            job.revision,
            job.generation,
            job.status,
            job.reason,
            Set.of("PAUSED", "CANCELLED").contains(job.status),
            job.calculation.complete(),
            job.calculation.operations(),
            job.request.start().canonicalKey(),
            job.request.start().provenance(),
            job.request.goal().catalogVersion(),
            MethodSearchCalculation.TEMPLATES,
            MethodSearchCalculation.VERSION,
            DeferredItem.VERSION,
            com.poe2craft.crafting.domain.pathsearch.RollPartition.VERSION,
            OutcomeStrategy.VERSION,
            BasicCurrencyTransitions.INTERPRETATION,
            job.calculation.assumptions(),
            List.copyOf(locals),
            List.copyOf(strategies),
            saved.graph(0));
  }

  private Job job(String owner, String id) {
    synchronized (jobs) {
      expire();
      var job = jobs.get(id);
      if (job == null || !job.owner.equals(owner)) throw new Rejected(410, "JOB_EXPIRED");
      return job;
    }
  }

  private void expire() {
    var expired = jobs.values().stream().filter(j -> !clock.instant().isBefore(j.expires)).toList();
    for (var job : expired) {
      try (var ignored = job.acquire()) {
        job.generation++;
        jobs.remove(job.id);
      }
    }
  }

  @Override
  public void close() {
    if (executor instanceof ExecutorService service) service.shutdownNow();
  }

  private static Probability p(Fraction f) {
    return Probability.of(f);
  }

  private static Map<String, List<TierVariant>> tierView(DeferredItem state) {
    var result = new LinkedHashMap<String, List<TierVariant>>();
    state
        .tiers()
        .forEach(
            (key, cell) ->
                result.put(
                    key,
                    cell.variants().stream()
                        .map(v -> new TierVariant(v.modifierId(), v.stat(), v.min(), v.max()))
                        .toList()));
    return Map.copyOf(result);
  }

  private static Map<String, OrbitView> orbitView(DeferredItem state) {
    var result = new TreeMap<String, OrbitView>();
    state
        .orbits()
        .forEach(
            (id, cell) ->
                result.put(
                    id,
                    new OrbitView(
                        cell.representatives().stream()
                            .map(com.poe2craft.item.ModifierInstance::modifierId)
                            .toList(),
                        cell.assignments().stream()
                            .map(
                                a ->
                                    a.families().stream()
                                        .map(
                                            tier ->
                                                tier.variants().stream()
                                                    .map(
                                                        v ->
                                                            new TierVariant(
                                                                v.modifierId(),
                                                                v.stat(),
                                                                v.min(),
                                                                v.max()))
                                                    .toList())
                                        .toList())
                            .toList())));
    return Map.copyOf(result);
  }

  private static int compare(Fraction a, Fraction b) {
    return a.numerator()
        .multiply(b.denominator())
        .compareTo(b.numerator().multiply(a.denominator()));
  }

  private static String bucket(MethodSearchCalculation.Progress p) {
    return p.goal()
        + ":"
        + p.entries().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");
  }

  private final class Job {
    final java.util.concurrent.locks.ReentrantLock lock =
        new java.util.concurrent.locks.ReentrantLock(true);

    LockGuard acquire() {
      lock.lock();
      return lock::unlock;
    }

    final String id = UUID.randomUUID().toString(), owner;
    final Create request;
    final Instant expires = clock.instant().plus(Duration.ofMinutes(20));
    final MethodSearchCalculation calculation;
    final Map<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new LinkedHashMap<>();
    final Map<Integer, OutcomeStrategy<DeferredItem>> solvers = new LinkedHashMap<>();
    final Map<String, CachedArrow> arrows = new HashMap<>();
    long revision = -1, generation;
    String status = "QUEUED", reason;
    volatile Snapshot snapshot;

    Job(String owner, Create request, MethodSearchCalculation calculation) {
      this.owner = owner;
      this.request = request;
      this.calculation = calculation;
    }
  }

  private interface LockGuard extends AutoCloseable {
    @Override
    void close();
  }

  private record Command(Mutation command, Snapshot response) {}

  private record CachedArrow(MethodSearchCalculation.MethodView method, Arrow arrow) {
    boolean matches(MethodSearchCalculation.MethodView next) {
      return method.status().equals(next.status())
          && Objects.equals(method.reason(), next.reason())
          && method.points().size() == next.points().size()
          && method.points().entrySet().stream()
              .allMatch(e -> next.points().get(e.getKey()) == e.getValue());
    }
  }

  private record Position(String scope, java.math.BigInteger offset) {}

  private final class Revision {
    final Map<String, CachedArrow> arrows;
    final String jobId;
    final long number;
    final List<MethodSearchCalculation.StateView> states;
    final List<MethodSearchCalculation.MethodView> methods;
    final Map<String, MethodSearchCalculation.StateView> byId = new HashMap<>();
    final Map<DeferredItem, MethodSearchCalculation.StateView> byState = new HashMap<>();
    final Map<String, List<MethodSearchCalculation.MethodView>> bySource = new HashMap<>();
    final List<Bucket> buckets;
    final Map<String, Position> cursors = new HashMap<>();
    final Map<Position, String> tokens = new HashMap<>();

    Revision(Job job) {
      arrows = job.arrows;
      jobId = job.id;
      number = job.revision;
      states = job.calculation.states();
      var groups = new LinkedHashMap<String, List<MethodSearchCalculation.StateView>>();
      states.forEach(
          s -> {
            byId.put(s.id(), s);
            byState.put(s.state(), s);
            groups.computeIfAbsent(bucket(s.progress()), unused -> new ArrayList<>()).add(s);
          });
      methods =
          job.calculation.methods().stream()
              .sorted(
                  Comparator.comparingInt(
                          (MethodSearchCalculation.MethodView m) -> byId.get(m.source()).depth())
                      .thenComparing(m -> m.points().isEmpty())
                      .thenComparing(MethodSearchCalculation.MethodView::id))
              .toList();
      methods.forEach(
          method ->
              bySource.computeIfAbsent(method.source(), unused -> new ArrayList<>()).add(method));
      buckets =
          groups.entrySet().stream()
              .map(
                  e -> {
                    var first = e.getValue().getFirst();
                    return new Bucket(
                        e.getKey(),
                        e.getValue().stream()
                            .mapToInt(MethodSearchCalculation.StateView::depth)
                            .min()
                            .orElseThrow(),
                        first.progress().goal().name(),
                        first.progress().entries().stream().map(Enum::name).toList(),
                        e.getValue().size(),
                        first.state().item(),
                        first.state().rolls(),
                        tierView(first.state()),
                        orbitView(first.state()),
                        e.getValue().stream()
                            .allMatch(MethodSearchCalculation.StateView::complete));
                  })
              .toList();
    }

    String cursor(String scope, int offset) {
      return cursor(scope, java.math.BigInteger.valueOf(offset));
    }

    String cursor(String scope, java.math.BigInteger offset) {
      var position = new Position(scope, offset);
      return tokens.computeIfAbsent(
          position,
          key -> {
            var token = UUID.randomUUID().toString();
            cursors.put(token, key);
            return token;
          });
    }

    Graph graph(int offset) {
      if (offset > methods.size()) throw new Rejected(422, "INVALID_CURSOR");
      int end = Math.min(methods.size(), offset + PAGE_SIZE);
      return new Graph(
          2,
          jobId,
          number,
          buckets,
          methods.subList(offset, end).stream().map(this::arrow).toList(),
          methods.size(),
          end < methods.size() ? cursor("graph", end) : null);
    }

    Arrow arrow(MethodSearchCalculation.MethodView method) {
      var cached = arrows.get(method.id());
      if (cached != null && cached.matches(method)) return cached.arrow();
      var source = byId.get(method.source());
      var points = new ArrayList<Point>();
      for (var entry : new TreeMap<>(method.points()).entrySet()) {
        var groups = new LinkedHashMap<String, Map<DeferredItem, Fraction>>();
        var observation = entry.getValue();
        var unresolved = Fraction.ZERO;
        for (var exit : observation.exits().entrySet()) {
          if (exit.getKey()
              == com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit.UNRESOLVED) {
            for (var value : exit.getValue().values()) unresolved = unresolved.add(value);
          } else
            exit.getValue()
                .forEach((state, mass) -> group(groups, exit.getKey().name(), state, mass));
        }
        observation.active().forEach((state, mass) -> group(groups, "ACTIVE", state, mass));
        var exits =
            groups.entrySet().stream()
                .map(
                    e -> {
                      int split = e.getKey().indexOf('|');
                      return new Exit(
                          e.getKey().substring(split + 1),
                          e.getKey().substring(0, split),
                          p(e.getValue().values().stream().reduce(Fraction.ZERO, Fraction::add)),
                          e.getValue().size());
                    })
                .toList();
        var initial = new LinkedHashMap<String, Probability>();
        var cycle = new LinkedHashMap<String, Probability>();
        observation.initialUses().forEach((a, mass) -> initial.put(a.name(), p(mass)));
        observation.cycleUses().forEach((a, mass) -> cycle.put(a.name(), p(mass)));
        points.add(
            new Point(
                entry.getKey(), exits, p(unresolved), Map.copyOf(initial), Map.copyOf(cycle)));
      }
      var result =
          new Arrow(
              method.id(),
              method.source(),
              bucket(source.progress()),
              source.state().item(),
              source.state().rolls(),
              tierView(source.state()),
              orbitView(source.state()),
              method.initialActions().stream().map(Enum::name).toList(),
              method.actions().stream().map(Enum::name).toList(),
              method.status(),
              method.reason(),
              List.copyOf(points));
      arrows.put(method.id(), new CachedArrow(method, result));
      return result;
    }

    void group(
        Map<String, Map<DeferredItem, Fraction>> groups,
        String kind,
        DeferredItem state,
        Fraction mass) {
      var target = byState.get(state);
      if (target == null) throw new IllegalStateException("Missing exact exit state");
      groups
          .computeIfAbsent(kind + "|" + bucket(target.progress()), unused -> new LinkedHashMap<>())
          .merge(state, mass, Fraction::add);
    }
  }
}
