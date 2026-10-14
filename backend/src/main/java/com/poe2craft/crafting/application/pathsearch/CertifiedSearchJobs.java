package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.CertifiedMethodProtocol.*;

import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Probability;
import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected;
import com.poe2craft.crafting.domain.pathsearch.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/** Process-local certified jobs share the existing service's single worker. */
final class CertifiedSearchJobs {
  private final Map<String, Job> jobs = new LinkedHashMap<>();
  private final Clock clock;
  private final Executor executor;
  private final int runBudget;
  private final BiFunction<
          MethodSearchProtocol.Create, CertifiedInterval.Precision, CertifiedMethodSearchEngine>
      factory;

  CertifiedSearchJobs(
      Clock clock,
      Executor executor,
      int runBudget,
      BiFunction<
              MethodSearchProtocol.Create, CertifiedInterval.Precision, CertifiedMethodSearchEngine>
          factory) {
    this.clock = clock;
    this.executor = executor;
    this.runBudget = runBudget;
    this.factory = factory;
  }

  Started create(String owner, Create request) {
    if (request == null
        || !Integer.valueOf(1).equals(request.version())
        || request.request() == null
        || request.precision() == null) throw new Rejected(422, "INVALID_REQUEST");
    synchronized (jobs) {
      jobs.values()
          .removeIf(
              j -> {
                if (!clock.instant().isAfter(j.expires)) return false;
                dispose(j);
                return true;
              });
      for (var existing : jobs.values()) {
        if (existing.owner.equals(owner)
            && Objects.equals(
                existing.request.request().clientRequestId(),
                request.request().clientRequestId())) {
          if (!existing.request.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
          try (var ignored = existing.acquireRequest()) {
            return new Started(
                existing.snapshot, entryView(existing, existing.entries.get("root"), 0));
          }
        }
      }
      if (jobs.size() >= 2) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var job = new Job(owner, request, factory.apply(request.request(), request.precision()));
      jobs.put(job.id, job);
      try (var ignored = job.acquireRequest()) {
        publish(job);
        submit(job);
        return new Started(job.snapshot, entryView(job, job.entries.get("root"), 0));
      }
    }
  }

  Snapshot get(String owner, String id) {
    return job(owner, id).snapshot;
  }

  boolean hasRequest(String owner, String requestId) {
    synchronized (jobs) {
      return jobs.values().stream()
          .anyMatch(
              j ->
                  !clock.instant().isAfter(j.expires)
                      && j.owner.equals(owner)
                      && j.request.request().clientRequestId().equals(requestId));
    }
  }

  private Job job(String owner, String id) {
    synchronized (jobs) {
      var job = jobs.get(id);
      if (job == null || !job.owner.equals(owner)) throw new Rejected(404, "SEARCH_NOT_FOUND");
      if (clock.instant().isAfter(job.expires)) throw new Rejected(410, "SEARCH_EXPIRED");
      return job;
    }
  }

  Snapshot mutate(String owner, String id, MethodSearchProtocol.Mutation command) {
    if (command == null
        || !Integer.valueOf(2).equals(command.version())
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.expectedRevision() == null
        || !Set.of("CANCEL", "RESUME").contains(Objects.toString(command.operation(), "")))
      throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var old = job.commands.get(command.commandId());
      if (old != null) {
        if (!old.command().equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
        return old.snapshot();
      }
      if (command.expectedRevision() != job.revision) throw new Rejected(409, "REVISION_CONFLICT");
      if (command.operation().equals("RESUME")) {
        if (!Set.of("PAUSED", "CANCELLED").contains(job.status))
          throw new Rejected(409, "SEARCH_NOT_PAUSED");
        if (command.retainedOutcomeLimit() != null) {
          int limit = command.retainedOutcomeLimit();
          if (limit < job.limit || limit > 1000000)
            throw new Rejected(422, "INVALID_MEMORY_BUDGET");
          job.engine.increaseMemoryBudget(limit);
          job.limit = limit;
        }
        job.generation++;
        job.status = "QUEUED";
        job.reason = null;
        publish(job);
        submit(job);
      } else {
        job.generation++;
        job.status = "CANCELLED";
        job.reason = null;
        publish(job);
      }
      job.commands.put(command.commandId(), new Command(command, job.snapshot));
      while (job.commands.size() > 64) job.commands.remove(job.commands.keySet().iterator().next());
      return job.snapshot;
    }
  }

  Graph graph(String owner, String id, long revision, int stateOffset, int methodOffset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var saved = revision(job, revision);
      checkOffset(stateOffset, saved.states.size());
      checkOffset(methodOffset, saved.methods.size());
      int stateEnd = Math.min(stateOffset + 48, saved.states.size());
      int methodEnd = Math.min(methodOffset + 12, saved.methods.size());
      var states =
          saved.states.subList(stateOffset, stateEnd).stream()
              .map(
                  s ->
                      new State(
                          s.id(),
                          s.depth(),
                          s.progress().goal().name(),
                          s.progress().entries().stream().map(Enum::name).toList(),
                          s.complete(),
                          s.representative().representative().item()))
              .toList();
      var methods =
          saved.methods.subList(methodOffset, methodEnd).stream()
              .map(m -> CertifiedMethodProtocol.method(m, job.engine.precision()))
              .toList();
      return new Graph(
          id,
          revision,
          states,
          methods,
          CertifiedMethodProtocol.locals(
              saved.states.subList(stateOffset, stateEnd), saved.methods, job.engine.precision()),
          stateEnd < saved.states.size() ? stateEnd : null,
          methodEnd < saved.methods.size() ? methodEnd : null);
    }
  }

  Outcomes outcomes(
      String owner, String id, long revision, String methodId, int cycles, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var method = method(revision(job, revision), methodId);
      var point = method.points().get(cycles);
      if (point == null) throw new Rejected(409, "OBSERVATION_NOT_READY");
      int size = point.active().size() + point.exits().values().stream().mapToInt(Map::size).sum();
      checkOffset(offset, size);
      var result = new ArrayList<Outcome>();
      int remainingOffset = offset;
      for (var kind : ExactMethodRunner.Exit.values())
        remainingOffset =
            appendOutcomes(
                result,
                kind.name(),
                point.exits().get(kind),
                remainingOffset,
                job.engine.precision().percentDecimals());
      appendOutcomes(
          result,
          "ACTIVE",
          point.active(),
          remainingOffset,
          job.engine.precision().percentDecimals());
      return new Outcomes(
          id,
          revision,
          methodId,
          cycles,
          List.copyOf(result),
          offset + result.size() < size ? offset + result.size() : null);
    }
  }

  private static int appendOutcomes(
      List<Outcome> result,
      String kind,
      Map<String, CertifiedInterval> law,
      int offset,
      int decimals) {
    if (result.size() == 48) return 0;
    if (offset >= law.size()) return offset - law.size();
    for (var entry : CertifiedLawStore.page(law, offset, 48 - result.size()))
      result.add(new Outcome(kind, entry.getKey(), Bounds.probability(entry.getValue(), decimals)));
    return 0;
  }

  Entry bind(String owner, String id, IndexedMethodProtocol.Bind request) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.revision() == null
        || request.requestId() == null
        || request.requestId().isBlank()
        || request.requestId().length() > 256
        || request.cycles() == null
        || !MethodSearchCalculation.OBSERVATIONS.contains(request.cycles())
        || request.kind() == null
        || request.kind().equals("UNRESOLVED")) throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      var previous = job.bindings.get(request.requestId());
      if (previous != null) {
        if (!previous.equals(request)) throw new Rejected(409, "REQUEST_ID_REUSED");
        return entryView(job, job.entries.get(job.entryIds.get(request.requestId())), 0);
      }
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "SEARCH_CANCELLED");
      if (job.entries.size() >= 65) throw new Rejected(503, "SEARCH_CAPACITY_REACHED");
      var method = method(revision(job, request.revision()), request.methodId());
      if (!method.points().containsKey(request.cycles()))
        throw new Rejected(409, "OBSERVATION_NOT_READY");
      var parent = entryWork(job, request.parentEntryId());
      if (!method.source().equals("class:" + parent.law.classId()))
        throw new Rejected(422, "ENTRY_SOURCE_MISMATCH");
      try {
        var kind =
            request.kind().equals("ACTIVE")
                ? Optional.<ExactMethodRunner.Exit>empty()
                : Optional.of(ExactMethodRunner.Exit.valueOf(request.kind()));
        var law =
            job.engine
                .observation(request.methodId(), request.cycles())
                .condition(parent.law, kind, job.engine.classId(request.targetStateId()));
        var entry = new EntryWork(UUID.randomUUID().toString(), parent.id, law);
        job.entries.put(entry.id, entry);
        job.bindings.put(request.requestId(), request);
        job.entryIds.put(request.requestId(), entry.id);
        return entryView(job, entry, 0);
      } catch (IllegalArgumentException ex) {
        throw new Rejected(422, "INVALID_CONDITIONAL_EXIT");
      }
    }
  }

  Entry entry(String owner, String id, String entryId, int offset) {
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      return entryView(job, entryWork(job, entryId), offset);
    }
  }

  Entry advance(String owner, String id, String entryId, IndexedMethodProtocol.Advance request) {
    if (request == null
        || !Integer.valueOf(2).equals(request.version())
        || request.expectedGeneration() == null
        || request.maxSteps() == null
        || request.maxSteps() < 1
        || request.maxSteps() > 4096
        || request.retainedStateLimit() == null
        || request.retainedStateLimit() < 1
        || request.retainedStateLimit() > 1000000) throw new Rejected(422, "INVALID_REQUEST");
    var job = job(owner, id);
    try (var ignored = job.acquireRequest()) {
      if (request.expectedGeneration() != job.generation)
        throw new Rejected(409, "GENERATION_CONFLICT");
      if (job.status.equals("CANCELLED")) throw new Rejected(409, "SEARCH_CANCELLED");
      if (request.retainedStateLimit() < job.entryLimit)
        throw new Rejected(422, "INVALID_MEMORY_BUDGET");
      job.entryLimit = request.retainedStateLimit();
      var work = entryWork(job, entryId);
      long until = System.nanoTime() + 20000000L;
      for (int n = 0;
          n < request.maxSteps() && !work.expansion.complete() && System.nanoTime() < until;
          n++) {
        if (entryRetained(job) > job.entryLimit) break;
        work.expansion.step();
      }
      return entryView(job, work, 0);
    }
  }

  private static long entryRetained(Job job) {
    return job.entries.values().stream().mapToLong(e -> e.expansion.retainedStates()).sum();
  }

  private Entry entryView(Job job, EntryWork work, int offset) {
    boolean complete = work.expansion.complete();
    var values =
        complete
            ? work.expansion.result().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().canonicalKey()))
                .toList()
            : List
                .<Map.Entry<
                        com.poe2craft.crafting.domain.BasicCurrencyState,
                        com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction>>
                    of();
    checkOffset(offset, values.size());
    int end = Math.min(offset + 48, values.size());
    var states =
        values.subList(offset, end).stream()
            .map(
                e ->
                    new MethodSearchProtocol.ConditionalState(
                        e.getKey().canonicalKey(),
                        e.getKey().item(),
                        "ENTRY",
                        Probability.of(e.getValue())))
            .toList();
    var probability =
        complete
            ? CertifiedInterval.enclose(
                work.expansion.probability(), job.engine.precision().fractionalBits())
            : work.law
                .enclosure()
                .orElseGet(
                    () ->
                        CertifiedInterval.enclose(
                            work.law.probability(), job.engine.precision().fractionalBits()));
    boolean cancelled = job.status.equals("CANCELLED");
    return new Entry(
        work.id,
        "class:" + work.law.classId(),
        work.parent,
        cancelled ? "CANCELLED" : complete ? "COMPLETE" : "PARTIAL",
        cancelled
            ? "SEARCH_CANCELLED"
            : !complete && entryRetained(job) > job.entryLimit ? "MEMORY_BUDGET" : null,
        Bounds.probability(probability, job.engine.precision().percentDecimals()),
        job.generation,
        states,
        end < values.size() ? end : null);
  }

  private static EntryWork entryWork(Job job, String id) {
    var entry = job.entries.get(id);
    if (entry == null) throw new Rejected(404, "ENTRY_NOT_FOUND");
    return entry;
  }

  private static void checkOffset(int offset, int size) {
    if (offset < 0 || offset > size) throw new Rejected(422, "INVALID_OFFSET");
  }

  private static CertifiedMethodSearchEngine.MethodView method(Revision revision, String id) {
    return revision.methods.stream()
        .filter(m -> m.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new Rejected(404, "METHOD_NOT_FOUND"));
  }

  private static Revision revision(Job job, long revision) {
    var found = job.revisions.get(revision);
    if (found == null) throw new Rejected(410, "REVISION_EXPIRED");
    return found;
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
    int used = 0;
    while (used < runBudget) {
      try (var ignored = job.acquire()) {
        if (job.generation != generation || !Set.of("QUEUED", "RUNNING").contains(job.status))
          return;
        if (clock.instant().isAfter(job.expires)) {
          job.status = "PAUSED";
          job.reason = "SEARCH_EXPIRED";
          publish(job);
          return;
        }
        job.status = "RUNNING";
        try {
          long until = System.nanoTime() + 20000000L;
          for (int n = 0;
              n < 128
                  && used < runBudget
                  && System.nanoTime() < until
                  && !job.engine.complete()
                  && !job.engine.memoryLimited();
              n++, used++) job.engine.step();
          if (job.engine.complete()) {
            job.status = "COMPLETED";
            job.reason = null;
            publish(job);
            return;
          }
          if (job.engine.memoryLimited() || used >= runBudget) {
            job.status = "PAUSED";
            job.reason =
                job.engine.storageLimited()
                    ? "STORAGE_BUDGET"
                    : job.engine.memoryLimited() ? "MEMORY_BUDGET" : "COMPUTE_BUDGET";
            publish(job);
            return;
          }
          if (used % 512 == 0) publish(job);
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

  private void publish(Job job) {
    if (job.snapshot != null
        && job.publishedGraphRevision == job.engine.graphRevision()
        && job.snapshot.generation() == job.generation
        && job.snapshot.status().equals(job.status)
        && Objects.equals(job.snapshot.reason(), job.reason)) return;
    job.revision++;
    boolean changed = job.publishedGraphRevision != job.engine.graphRevision();
    var previous = job.revisions.isEmpty() ? null : job.revisions.get(job.revision - 1);
    var states = changed || previous == null ? job.engine.states() : previous.states();
    var methods = changed || previous == null ? job.engine.methods() : previous.methods();
    job.publishedGraphRevision = job.engine.graphRevision();
    var rootStates = states.stream().filter(s -> s.id().equals(job.engine.rootId())).toList();
    var rootMethods = methods.stream().filter(m -> m.source().equals(job.engine.rootId())).toList();
    job.revisions.put(job.revision, new Revision(states, methods));
    while (job.revisions.size() > 4) job.revisions.remove(job.revisions.keySet().iterator().next());
    job.snapshot =
        new Snapshot(
            VERSION,
            job.id,
            job.revision,
            job.generation,
            job.status,
            job.reason,
            job.engine.complete(),
            job.engine.operations(),
            job.engine.rootId(),
            job.engine.precision(),
            CertifiedInterval.VERSION,
            job.engine.calculationVersion(),
            job.engine.templatesVersion(),
            ConditionalMethodLaw.VERSION,
            job.request.request().start().provenance(),
            job.request.request().goal().catalogVersion(),
            job.engine.assumptions(),
            "NOT_REQUESTED",
            states.size(),
            methods.size(),
            new Storage(
                CertifiedLawStore.VERSION,
                job.engine.retainedEntries(),
                job.engine.storage().storedRows(),
                job.engine.storage().packedBytes(),
                32L * 1024 * 1024,
                job.engine.freezeTransientBytes(),
                job.engine.kernelStorage().packedBytes(),
                job.engine.kernelStorage().packedByteLimit(),
                job.engine.storage().materializedRows(),
                job.engine.storage().uniquePages()),
            CertifiedMethodProtocol.locals(rootStates, rootMethods, job.engine.precision()));
  }

  private void dispose(Job job) {
    try (var ignored = job.acquire()) {
      job.generation++;
      job.status = "EXPIRED";
      job.disposed = true;
      job.revisions.clear();
      job.commands.clear();
      job.entries.clear();
      job.bindings.clear();
      job.entryIds.clear();
      job.engine.close();
    }
  }

  void close() {
    synchronized (jobs) {
      jobs.values().forEach(this::dispose);
      jobs.clear();
    }
  }

  private record Revision(
      List<MethodSearchEngine.StateView> states,
      List<CertifiedMethodSearchEngine.MethodView> methods) {}

  private record Command(MethodSearchProtocol.Mutation command, Snapshot snapshot) {}

  private interface Guard extends AutoCloseable {
    @Override
    void close();
  }

  private static final class EntryWork {
    final String id, parent;
    final ConditionalMethodLaw law;
    final ConditionalMethodLaw.Expansion expansion;

    EntryWork(String id, String parent, ConditionalMethodLaw law) {
      this.id = id;
      this.parent = parent;
      this.law = law;
      expansion = law.expansion();
    }
  }

  private final class Job {
    final java.util.concurrent.locks.ReentrantLock lock =
        new java.util.concurrent.locks.ReentrantLock(true);

    Guard acquire() {
      lock.lock();
      return lock::unlock;
    }

    Guard acquireRequest() {
      var guard = acquire();
      if (disposed || clock.instant().isAfter(expires)) {
        guard.close();
        throw new Rejected(410, "SEARCH_EXPIRED");
      }
      return guard;
    }

    final String id = UUID.randomUUID().toString(), owner;
    final Create request;
    final CertifiedMethodSearchEngine engine;
    final Instant expires = clock.instant().plus(Duration.ofMinutes(20));
    final Map<Long, Revision> revisions = new LinkedHashMap<>();
    final Map<String, Command> commands = new LinkedHashMap<>();
    final Map<String, EntryWork> entries = new LinkedHashMap<>();
    final Map<String, IndexedMethodProtocol.Bind> bindings = new LinkedHashMap<>();
    final Map<String, String> entryIds = new LinkedHashMap<>();
    long revision = -1, generation;
    long publishedGraphRevision = -1;
    boolean disposed;
    int limit = 100000, entryLimit = 1;
    String status = "QUEUED", reason;
    volatile Snapshot snapshot;

    Job(String owner, Create request, CertifiedMethodSearchEngine engine) {
      this.owner = owner;
      this.request = request;
      this.engine = engine;
      entries.put("root", new EntryWork("root", null, engine.rootLaw()));
    }
  }
}
