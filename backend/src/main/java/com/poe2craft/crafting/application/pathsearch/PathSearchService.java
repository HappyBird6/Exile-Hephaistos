package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.item.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** Bounded, process-local jobs. A monitor is the publication/cancel linearization boundary. */
public final class PathSearchService implements AutoCloseable {
  public static final String PREDICATE = "goal-filter-full-item-v1",
      SEARCH = "solar-streaming-renewal-v1";
  private final ItemCatalog catalog;
  private final GoalFilterService goals;
  private final BasicCurrencyTransitions transitions;
  private final ObjectMapper json;
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final Map<String, Job> jobs = new LinkedHashMap<>();
  private final Map<String, String> requestIds = new HashMap<>();
  private static final int MAX_JOBS = 16, MAX_REVISIONS = 8, PAGE_EDGES = 48, MAX_COMMANDS = 64;
  private static final Duration TTL = Duration.ofMinutes(15);

  public PathSearchService(
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
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_JOBS),
            r -> {
              var t = new Thread(r, "crafting-path-search");
              t.setDaemon(true);
              return t;
            }),
        50000);
  }

  public PathSearchService(
      ItemCatalog catalog,
      GoalFilterService goals,
      String identity,
      ObjectMapper json,
      Clock clock,
      Executor executor,
      int runBudget) {
    if (runBudget < 1) throw new IllegalArgumentException("Positive work budget required");
    this.catalog = catalog;
    this.goals = goals;
    this.transitions = new BasicCurrencyTransitions(catalog, identity);
    this.json = json.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    this.clock = clock;
    this.executor = executor;
    this.runBudget = runBudget;
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public Snapshot create(String owner, Create request) {
    try {
      request = json.readValue(encode(request), Create.class);
    } catch (java.io.IOException e) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
    validate(request);
    var predicate = goals.compile(request.start().item(), request.goal());
    return create(owner, request, predicate, null, fingerprint(request));
  }

  private Snapshot create(
      String owner,
      Create request,
      Function<ItemState, GoalFilter.Status> predicate,
      Recovery recovery,
      String fingerprint) {
    synchronized (jobs) {
      expire();
      String key = owner + ":" + request.clientRequestId();
      var old = requestIds.get(key);
      if (old != null) {
        var existing = jobs.get(old);
        if (!existing.fingerprint.equals(fingerprint)) throw new Rejected(409, "REQUEST_ID_REUSED");
        synchronized (existing) {
          return existing.snapshot;
        }
      }
      if (jobs.size() >= MAX_JOBS) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var job = new Job(owner, request, fingerprint, predicate, recovery);
      jobs.put(job.id, job);
      requestIds.put(key, job.id);
      synchronized (job) {
        publish(job);
        var accepted = job.snapshot;
        if (job.blocked == null) submit(job);
        return accepted;
      }
    }
  }

  public Snapshot get(String owner, String id) {
    var job = job(owner, id);
    synchronized (job) {
      return job.snapshot;
    }
  }

  public Snapshot mutate(String owner, String id, Mutation request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.commandId() == null
        || request.commandId().isBlank()
        || request.expectedRevision() == null
        || request.expectedRevision() < 0
        || !Set.of("CANCEL", "RESUME").contains(request.operation()))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    synchronized (job) {
      var old = job.commands.get(request.commandId());
      if (old != null) {
        if (!old.request.equals(request)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.response;
      }
      if (job.commands.size() >= MAX_COMMANDS) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      if (request.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (request.operation().equals("CANCEL")) {
        if (Set.of("QUEUED", "RUNNING", "PAUSED").contains(job.status)) {
          job.generation++;
          job.status = "CANCELLED";
          publish(job);
        }
      } else {
        if (!job.snapshot.resumable()) throw new Rejected(409, "JOB_NOT_RESUMABLE");
        job.generation++;
        job.status = "QUEUED";
        publish(job);
        submit(job);
      }
      var response = job.snapshot;
      job.commands.put(request.commandId(), new Command(request, response));
      return response;
    }
  }

  public GraphPage graph(String owner, String id, long revision, String cursor) {
    var job = job(owner, id);
    synchronized (job) {
      var saved = job.revisions.get(revision);
      if (saved == null) throw new Rejected(410, "REVISION_EXPIRED");
      int offset = 0;
      if (cursor != null) {
        var position = saved.cursors.get(cursor);
        if (position == null) throw new Rejected(422, "INVALID_CURSOR");
        offset = position;
      }
      return saved.pages.get(offset);
    }
  }

  public Snapshot recover(String owner, String id, Recover request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.parentRevision() == null
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()) throw new Rejected(422, "INVALID_REQUEST");
    observations(request.observations());
    var parent = job(owner, id);
    Create frozen;
    Recovery metadata;
    ItemState checkpoint;
    synchronized (parent) {
      var revision = parent.revisions.get(request.parentRevision());
      if (revision == null) throw new Rejected(410, "REVISION_EXPIRED");
      var failure = revision.executions.get(request.failureExecutionId());
      var target = revision.nodes.get(request.checkpointStateId());
      if (failure == null
          || target == null
          || !revision.nodes.get(failure.stateId()).goalStatus().equals("NO_MATCH"))
        throw new Rejected(422, "INVALID_RECOVERY_REFERENCE");
      // Reverse walk only actual retained edges of this policy. Sharing a job/state is
      // insufficient.
      var seen = new HashSet<String>();
      var todo = new ArrayDeque<String>();
      todo.add(failure.id());
      boolean ancestor = false;
      while (!todo.isEmpty()) {
        var execution = todo.removeFirst();
        if (!seen.add(execution)) continue;
        if (revision.executions.get(execution).stateId().equals(target.id())) {
          ancestor = true;
          break;
        }
        for (var edge : revision.edges)
          if (edge.to().equals(execution)
              && revision.executions.get(edge.from()).policyId().equals(failure.policyId()))
            todo.add(edge.from());
      }
      if (!ancestor) throw new Rejected(422, "INVALID_RECOVERY_CHECKPOINT");
      checkpoint = target.item();
      var state =
          new BasicCurrencyState(revision.nodes.get(failure.stateId()).item(), provenance());
      frozen =
          new Create(
              1,
              request.clientRequestId(),
              state,
              parent.request.goal(),
              List.of(),
              request.observations());
      metadata = new Recovery(id, request.parentRevision(), failure.id(), target.id(), true, false);
    }
    validate(frozen);
    var targetState = checkpoint;
    return create(
        owner,
        frozen,
        item -> item.equals(targetState) ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH,
        metadata,
        digest(fingerprint(frozen) + encode(metadata)));
  }

  private void submit(Job job) {
    long generation = job.generation;
    try {
      executor.execute(() -> run(job, generation));
    } catch (RejectedExecutionException e) {
      job.status = "PAUSED";
      job.reason = "SEARCH_CAPACITY_REACHED";
      publish(job);
    }
  }

  private void run(Job job, long generation) {
    synchronized (job) {
      if (!live(job, generation)) return;
      job.status = "RUNNING";
      publish(job);
    }
    try {
      for (int work = 0; work < runBudget; work++) {
        synchronized (job) {
          if (!live(job, generation)) return;
          job.calculation.step();
          if (job.calculation.complete()) {
            job.status = job.calculation.outputLimited() ? "PAUSED" : "COMPLETED";
            job.reason = job.calculation.outputLimited() ? "FRACTION_OUTPUT_LIMIT" : null;
            publish(job);
            return;
          }
          if ((work + 1) % 1000 == 0) publish(job);
        }
      }
      synchronized (job) {
        if (live(job, generation)) {
          job.status = "PAUSED";
          job.reason = "WORK_BUDGET_EXHAUSTED";
          publish(job);
        }
      }
    } catch (RuntimeException e) {
      synchronized (job) {
        if (live(job, generation)) {
          job.status = "FAILED";
          job.reason = "SEARCH_FAILED";
          publish(job);
        }
      }
    }
  }

  private boolean live(Job job, long generation) {
    return !Thread.currentThread().isInterrupted()
        && job.generation == generation
        && clock.instant().isBefore(job.expires)
        && Set.of("QUEUED", "RUNNING").contains(job.status);
  }

  private void publish(Job job) {
    job.revision++;
    var c = job.calculation;
    var recommendations = job.blocked == null ? c.recommendations() : List.<Recommendation>of();
    var saved = new Revision(job.id, job.revision, c);
    job.revisions.put(job.revision, saved);
    while (job.revisions.size() > MAX_REVISIONS)
      job.revisions.remove(job.revisions.keySet().iterator().next());
    var capabilities =
        new Capabilities(
            job.evaluation,
            job.blocked == null ? "SUPPORTED" : "UNSUPPORTED",
            job.blocked,
            PathSearchCalculation.ACTIONS.stream()
                .flatMap(List::stream)
                .map(Enum::name)
                .distinct()
                .toList(),
            List.of("AND", "COUNT"),
            true,
            true);
    job.snapshot =
        new Snapshot(
            1,
            job.id,
            job.request.clientRequestId(),
            job.fingerprint,
            job.revision,
            job.status,
            job.expires.toString(),
            capabilities,
            new Provenance(
                provenance(),
                job.request.goal().catalogVersion(),
                PREDICATE,
                SEARCH,
                PathSearchCalculation.CANDIDATES,
                BasicCurrencyTransitions.INTERPRETATION),
            new Scope(
                "FINITE_POLICY_CANDIDATES", job.blocked == null ? 6 : 0, 6, job.blocked == null),
            recommendations,
            job.blocked == null
                ? PathSearchCalculation.rankings(recommendations, c.observations)
                : List.of(),
            saved.pages.getFirst(),
            job.recovery,
            job.blocked == null
                && !c.complete()
                && Set.of("PAUSED", "CANCELLED").contains(job.status),
            job.blocked == null ? job.reason : job.blocked);
  }

  private void validate(Create r) {
    if (r == null
        || !Integer.valueOf(1).equals(r.version())
        || r.clientRequestId() == null
        || r.clientRequestId().isBlank()
        || r.clientRequestId().length() > 256
        || r.start() == null
        || r.goal() == null
        || r.activeOmens() == null
        || !r.activeOmens().isEmpty()) throw new Rejected(422, "INVALID_REQUEST");
    observations(r.observations());
    if (!provenance().equals(r.start().provenance()))
      throw new Rejected(422, "RULESET_IDENTITY_MISMATCH");
    if (r.start().item().baseItemId().equals(catalog.base().id())
        && new ItemStateValidator(catalog)
            .validate(r.start().item()).stream()
                .anyMatch(v -> v.code() != ItemStateValidator.Code.UNSUPPORTED_STATE))
      throw new Rejected(422, "INVALID_ITEM");
    goals.evaluate(r.start().item(), r.goal());
  }

  private static List<String> observations(List<String> values) {
    if (values == null
        || values.isEmpty()
        || values.size() > 32
        || new HashSet<>(values).size() != values.size())
      throw new Rejected(422, "INVALID_OBSERVATIONS");
    try {
      for (String value : values)
        if (value == null || !value.matches("0|[1-9][0-9]*")) throw new NumberFormatException();
      return values.stream().map(Long::parseLong).sorted().map(Object::toString).toList();
    } catch (NumberFormatException e) {
      throw new Rejected(422, "INVALID_OBSERVATIONS");
    }
  }

  private String blocked(Create request) {
    var item = request.start().item();
    if (!SolarAmulet.BASE_ID.equals(item.baseItemId())
        || item.rarity() != ItemState.Rarity.RARE
        || item.explicits().size() != 1
        || item.explicits().getFirst().fractured()
        || item.catalystQuality() != null
        || !item.conditions().isEmpty()) return "START_STATE_NOT_SUPPORTED";
    if (request.goal().groups().stream()
        .anyMatch(
            g ->
                !g.disabled()
                    && g.type() != GoalFilter.Type.AND
                    && g.type() != GoalFilter.Type.COUNT)) return "GOAL_GROUP_NOT_SUPPORTED";
    var evaluated = goals.evaluate(item, request.goal()).status();
    return evaluated == GoalFilter.Status.UNKNOWN || evaluated == GoalFilter.Status.UNSUPPORTED
        ? "GOAL_EVALUATION_NOT_SUPPORTED"
        : null;
  }

  private String fingerprint(Create r) {
    return digest(
        encode(new Create(r.version(), "", r.start(), r.goal(), r.activeOmens(), r.observations()))
            + PREDICATE
            + SEARCH
            + PathSearchCalculation.CANDIDATES);
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (java.io.IOException e) {
      throw new Rejected(422, "INVALID_REQUEST");
    }
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
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
      synchronized (job) {
        job.generation++;
        jobs.remove(job.id);
        requestIds.remove(job.owner + ":" + job.request.clientRequestId());
      }
    }
  }

  @Override
  public void close() {
    if (executor instanceof ExecutorService service) service.shutdownNow();
  }

  private final class Job {
    final String id = UUID.randomUUID().toString(), owner, fingerprint, blocked, evaluation;
    final Create request;
    final Recovery recovery;
    final Instant expires = clock.instant().plus(TTL);
    final PathSearchCalculation calculation;
    final LinkedHashMap<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new HashMap<>();
    long generation, revision = -1;
    String status, reason;
    Snapshot snapshot;

    Job(
        String owner,
        Create request,
        String fingerprint,
        Function<ItemState, GoalFilter.Status> predicate,
        Recovery recovery) {
      this.owner = owner;
      this.request = request;
      this.fingerprint = fingerprint;
      this.recovery = recovery;
      blocked = blocked(request);
      var rootStatus = predicate.apply(request.start().item());
      evaluation =
          rootStatus == GoalFilter.Status.UNKNOWN
              ? "UNKNOWN"
              : rootStatus == GoalFilter.Status.UNSUPPORTED ? "UNSUPPORTED" : "SUPPORTED";
      status = blocked == null ? "QUEUED" : "UNSUPPORTED";
      calculation =
          new PathSearchCalculation(
              request.start(), predicate, observations(request.observations()), transitions);
    }
  }

  private record Command(Mutation request, Snapshot response) {}

  private static final class Revision {
    final Map<String, Node> nodes;
    final Map<String, Execution> executions;
    final List<Edge> edges;
    final List<GraphPage> pages = new ArrayList<>();
    final Map<String, Integer> cursors = new HashMap<>();

    Revision(String job, long revision, PathSearchCalculation c) {
      nodes = Map.copyOf(c.nodes);
      executions = Map.copyOf(c.executions);
      edges = List.copyOf(c.edges.values());
      int count = Math.max(1, (edges.size() + PAGE_EDGES - 1) / PAGE_EDGES);
      var tokens = new ArrayList<String>();
      for (int i = 0; i < count; i++) {
        String token = UUID.randomUUID().toString();
        tokens.add(token);
        cursors.put(token, i);
      }
      for (int i = 0; i < count; i++)
        pages.add(
            new GraphPage(
                1,
                job,
                revision,
                i == 0 ? List.copyOf(c.nodes.values()) : List.of(),
                i == 0 ? List.copyOf(c.executions.values()) : List.of(),
                edges.subList(i * PAGE_EDGES, Math.min(edges.size(), (i + 1) * PAGE_EDGES)),
                i == count - 1 ? List.copyOf(c.expansions.values()) : List.of(),
                i + 1 < count ? tokens.get(i + 1) : null));
    }
  }
}
