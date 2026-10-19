package com.poe2craft.crafting.application.pathsearch;

import static com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Rejected;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.ItemState;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/** Request-driven bounded slices share one lock; no detached work survives cancellation. */
public final class AutomaticSearchService implements AutoCloseable {
  public static final String VERSION = "automatic-method-job-v1";

  public record Input(MethodSearchProtocol.Create request, boolean abstractQuality40) {}

  public record Runtime(
      AutomaticCraftingKernel kernel,
      Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      String objective) {}

  public record Control(String commandId, long revision, long generation, String operation) {}

  public record ExitView(Integer target, String kind, CertifiedMethodProtocol.Bounds probability) {}

  public record PointView(
      int cycles,
      boolean complete,
      List<ExitView> outcomes,
      Map<String, CertifiedMethodProtocol.Bounds> initialUses,
      Map<String, CertifiedMethodProtocol.Bounds> cycleUses) {}

  public record Arrow(
      String id,
      int source,
      List<AutomaticCraftingAction> initial,
      List<AutomaticCraftingAction> cycle,
      String executionMode,
      List<PointView> points,
      Set<String> reasons,
      boolean finished) {}

  public record Recommendation(
      int node,
      int cycles,
      List<String> local,
      CertifiedMethodProtocol.Bounds strategy,
      String strategyStatus,
      List<String> strategyMethods) {}

  public record View(
      String version,
      String id,
      long revision,
      long generation,
      String status,
      String reason,
      boolean complete,
      boolean canResume,
      long operations,
      long retainedStates,
      List<AutomaticMethodGraph.NodeView> nodes,
      List<Arrow> methods,
      List<Recommendation> recommendations) {}

  public record Disk(
      String version,
      String id,
      String owner,
      MethodSearchProtocol.Create request,
      long revision,
      long generation,
      String status,
      String reason,
      long expires,
      Map<String, Control> commands,
      AutomaticMethodGraph.Checkpoint graph) {}

  private static final int MAX_FILES = 16;
  private final ObjectMapper json;
  private final AutomaticJobRepository repository;
  private final AutomaticCurrencyPolicy policy;
  private final Function<MethodSearchProtocol.Create, Runtime> factory;
  private final Clock clock;
  private final LinkedHashMap<String, Job> jobs = new LinkedHashMap<>(4, .75f, true);

  private static final class Job {
    String id, owner, status = "READY", reason = "";
    MethodSearchProtocol.Create request;
    Runtime runtime;
    AutomaticMethodGraph graph;
    long revision = 1, generation = 1, expires;
    Map<String, Control> commands = new LinkedHashMap<>();
  }

  public AutomaticSearchService(
      ObjectMapper json,
      AutomaticJobRepository repository,
      AutomaticCurrencyPolicy policy,
      Function<MethodSearchProtocol.Create, Runtime> factory,
      Clock clock) {
    this.json = json.copy();
    this.repository = repository;
    this.policy = policy;
    this.factory = factory;
    this.clock = clock;
  }

  private static String ownerKey(String owner) {
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(owner.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private Disk read(String id) {
    try {
      return json.readValue(repository.read(id), Disk.class);
    } catch (java.io.IOException e) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
  }

  private void cache(Job job) {
    jobs.put(job.id, job);
    while (jobs.size() > 2) {
      var id = jobs.keySet().iterator().next();
      jobs.remove(id).graph.close();
    }
  }

  private Job restore(Disk disk) {
    if (!VERSION.equals(disk.version())) throw new Rejected(409, "RECALCULATION_REQUIRED");
    var job = new Job();
    job.id = disk.id();
    job.owner = disk.owner();
    job.request = disk.request();
    job.revision = disk.revision();
    job.generation = disk.generation();
    job.status = disk.status();
    job.reason = disk.reason();
    job.expires = disk.expires();
    job.commands.putAll(disk.commands());
    try {
      job.runtime = factory.apply(job.request);
      job.graph =
          AutomaticMethodGraph.restore(
              disk.graph(),
              job.runtime.objective(),
              policy,
              job.runtime.kernel(),
              job.runtime.evaluate());
    } catch (IllegalArgumentException e) {
      throw new Rejected(409, "RECALCULATION_REQUIRED");
    }
    return job;
  }

  private Job load(String owner, String id) {
    if (!id.matches("[0-9a-f-]{36}") || !repository.ids().contains(id))
      throw new Rejected(404, "JOB_NOT_FOUND");
    var job = jobs.get(id);
    if (job == null) {
      var saved = read(id);
      if (!saved.owner().equals(ownerKey(owner))) throw new Rejected(404, "JOB_NOT_FOUND");
      job = restore(saved);
      cache(job);
    }
    if (!job.owner.equals(ownerKey(owner)) || job.expires < clock.millis())
      throw new Rejected(404, "JOB_NOT_FOUND");
    return job;
  }

  private Disk disk(Job job) {
    return new Disk(
        VERSION,
        job.id,
        job.owner,
        job.request,
        job.revision,
        job.generation,
        job.status,
        job.reason,
        job.expires,
        Map.copyOf(job.commands),
        job.graph.checkpoint(job.runtime.objective()));
  }

  private void save(Job job) {
    try {
      repository.save(job.id, json.writeValueAsBytes(disk(job)));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot serialize automatic job", e);
    }
  }

  public synchronized View create(String owner, MethodSearchProtocol.Create request) {
    return create(owner, new Input(request, false));
  }

  public synchronized View create(String owner, Input input) {
    if (input == null) throw new Rejected(422, "INVALID_REQUEST");
    var request = input.request();
    if (request == null
        || request.clientRequestId() == null
        || request.clientRequestId().isBlank()
        || request.clientRequestId().length() > 256) throw new Rejected(422, "INVALID_REQUEST");
    for (var id : repository.ids()) {
      var saved = read(id);
      if (saved.expires() < clock.millis()) {
        repository.delete(id);
        var old = jobs.remove(id);
        if (old != null) old.graph.close();
        continue;
      }
      if (saved.owner().equals(ownerKey(owner))
          && saved.request().clientRequestId().equals(request.clientRequestId())) {
        if (!saved.request().equals(request)
            || saved.graph().nodes().getFirst().quality40() != input.abstractQuality40())
          throw new Rejected(409, "REQUEST_ID_REUSED");
        return view(load(owner, id));
      }
    }
    if (repository.ids().size() >= MAX_FILES) throw new Rejected(429, "JOB_CAPACITY");
    var job = new Job();
    job.id = UUID.randomUUID().toString();
    job.owner = ownerKey(owner);
    job.request = request;
    job.runtime = factory.apply(request);
    job.expires = clock.millis() + Duration.ofHours(24).toMillis();
    job.graph =
        new AutomaticMethodGraph(
            request.start().item(),
            policy,
            job.runtime.kernel(),
            job.runtime.evaluate(),
            CertifiedInterval.Precision.standard(),
            64,
            128,
            20000,
            5000,
            input.abstractQuality40());
    save(job);
    cache(job);
    return view(job);
  }

  public synchronized View get(String owner, String id) {
    return view(load(owner, id));
  }

  public synchronized MethodSearchProtocol.Create original(String owner, String id) {
    var saved = read(id);
    if (!saved.owner().equals(ownerKey(owner))) throw new Rejected(404, "JOB_NOT_FOUND");
    return saved.request();
  }

  public synchronized Input originalInput(String owner, String id) {
    var request = original(owner, id);
    return new Input(request, read(id).graph().nodes().getFirst().quality40());
  }

  public synchronized View control(String owner, String id, Control command) {
    var job = load(owner, id);
    if (command == null
        || command.commandId() == null
        || command.commandId().isBlank()
        || command.commandId().length() > 256
        || command.operation() == null
        || !Set.of("ADVANCE", "RESUME", "CANCEL").contains(command.operation()))
      throw new Rejected(422, "INVALID_COMMAND");
    var previous = job.commands.get(command.commandId());
    if (previous != null) {
      if (!previous.equals(command)) throw new Rejected(409, "COMMAND_ID_REUSED");
      return view(job);
    }
    if (command.generation() != job.generation || command.revision() != job.revision)
      throw new Rejected(409, "STALE_REVISION");
    var before = read(id);
    try {
      switch (command.operation()) {
        case "CANCEL" -> {
          job.status = "CANCELLED";
          job.reason = "";
          job.generation++;
        }
        case "RESUME" -> {
          if (job.graph.finished()) break;
          var saved = job.graph.checkpoint(job.runtime.objective());
          if (job.status.equals("PAUSED")
              && (job.reason.equals("STORAGE_BUDGET") || !job.graph.canResume()))
            throw new Rejected(409, "MAXIMUM_RESOURCE_BUDGET");
          job.graph.increaseLimits(
              Math.min(128, saved.states() * 2),
              Math.min(256, saved.methods() * 2),
              Math.min(50000, saved.retained() * 2),
              Math.min(10000, saved.primitive() * 2));
          job.generation++;
          job.status = "READY";
          job.reason = "";
        }
        case "ADVANCE" -> {
          if (!job.status.equals("READY")) break;
          long until = System.nanoTime() + 300_000_000L;
          for (int i = 0;
              i < 2000
                  && System.nanoTime() < until
                  && !job.graph.finished()
                  && !job.graph.resourceLimited();
              i++) job.graph.step();
          job.graph.publishProgress();
          if (job.graph.resourceLimited()) {
            job.status = "PAUSED";
            job.reason = "RESOURCE_BUDGET";
          } else if (job.graph.finished()) {
            job.status = job.graph.complete() ? "COMPLETE" : "PARTIAL";
            job.reason =
                job.graph.complete()
                    ? (job.graph.hasGoal()
                        ? "SUPPORTED_PLANS_COMPLETE"
                        : "NO_PLAN_IN_SUPPORTED_SCOPE")
                    : "UNRESOLVED_MODEL_OR_POLICY";
          }
        }
        default -> throw new IllegalStateException();
      }
      job.revision++;
      job.commands.put(command.commandId(), command);
      if (job.commands.size() > 256) job.commands.remove(job.commands.keySet().iterator().next());
      save(job);
    } catch (AutomaticJobRepository.BudgetExceeded error) {
      job.graph.close();
      jobs.remove(id);
      var paused = restore(before);
      paused.status = "PAUSED";
      paused.reason = "STORAGE_BUDGET";
      paused.revision++;
      paused.commands.put(command.commandId(), command);
      save(paused);
      cache(paused);
      return view(paused);
    } catch (RuntimeException error) {
      job.graph.close();
      jobs.remove(id);
      cache(restore(before));
      throw error;
    }
    return view(job);
  }

  private static Map<String, CertifiedMethodProtocol.Bounds> uses(
      Map<AutomaticCraftingAction, CertifiedInterval> input) {
    var result = new LinkedHashMap<String, CertifiedMethodProtocol.Bounds>();
    AutomaticMethodExecution.materialUses(input)
        .forEach((key, value) -> result.put(key, CertifiedMethodProtocol.Bounds.quantity(value)));
    return result;
  }

  private View view(Job job) {
    var nodes = job.graph.nodes();
    var methodViews = job.graph.methods();
    var ids = new HashMap<AutomaticMethodGraph.Identity, Integer>();
    nodes.forEach(n -> ids.put(new AutomaticMethodGraph.Identity(n.item(), n.quality40()), n.id()));
    var arrows = new ArrayList<Arrow>();
    for (var method : methodViews) {
      var points = new ArrayList<PointView>();
      for (var entry : method.points().entrySet()) {
        var rows = new ArrayList<ExitView>();
        var grouped = new LinkedHashMap<String, CertifiedInterval>();
        for (var mass : AutomaticMethodGraph.encode(entry.getValue()).outcomes()) {
          var target =
              ids.get(
                  new AutomaticMethodGraph.Identity(mass.state().item(), mass.state().quality40()));
          if (mass.exit() == Exit.LOSS || mass.exit() == Exit.DEAD) target = null;
          String kind = mass.exit() == null ? "ACTIVE" : mass.exit().name();
          if ((mass.exit() == Exit.UNRESOLVED && !mass.state().temporaryPreparation())
              || (target == null && mass.exit() != Exit.DEAD && mass.exit() != Exit.LOSS)) {
            target = null;
            kind = "UNRESOLVED";
          }
          var key = (target == null ? "null" : target.toString()) + ":" + kind;
          grouped.merge(key, mass.probability(), CertifiedInterval::add);
        }
        grouped.forEach(
            (key, value) -> {
              var pair = key.split(":");
              rows.add(
                  new ExitView(
                      pair[0].equals("null") ? null : Integer.valueOf(pair[0]),
                      pair[1],
                      CertifiedMethodProtocol.Bounds.probability(value, 2)));
            });
        points.add(
            new PointView(
                entry.getKey(),
                method.completeObservations().contains(entry.getKey()),
                rows,
                uses(entry.getValue().initialUses()),
                uses(entry.getValue().cycleUses())));
      }
      points.sort(Comparator.comparingInt(PointView::cycles));
      arrows.add(
          new Arrow(
              method.id(),
              method.source(),
              method.template().initial(),
              method.template().cycle(),
              method.executionMode(),
              points,
              method.unresolvedReasons(),
              method.finished()));
    }
    var recommendations = new ArrayList<Recommendation>();
    for (int cycles : List.of(100, 300, 500)) {
      var strategy = job.graph.strategy(cycles);
      for (int i = 0; i < 16; i++) strategy.refine();
      for (var node : nodes) {
        var scores = new LinkedHashMap<String, CertifiedInterval>();
        if (node.directFinished())
          for (var method : methodViews)
            if (method.source() == node.id() && method.points().containsKey(cycles)) {
              var point = method.points().get(cycles);
              var score = CertifiedInterval.zero(64);
              for (var kind : List.of(Exit.GOAL, Exit.CHECKPOINT))
                for (var mass : point.exits().get(kind).values()) score = score.add(mass);
              var unknown = CertifiedInterval.zero(64);
              for (var mass : point.exits().get(Exit.UNRESOLVED).values())
                unknown = unknown.add(mass);
              score =
                  new CertifiedInterval(
                      64, score.lowerUnits(), score.upperUnits().add(unknown.upperUnits()));
              scores.put(method.id(), score.probability());
            }
        var local =
            scores.entrySet().stream()
                .filter(
                    e ->
                        scores.entrySet().stream()
                            .filter(other -> !other.getKey().equals(e.getKey()))
                            .allMatch(
                                other ->
                                    e.getValue()
                                            .lowerUnits()
                                            .compareTo(other.getValue().upperUnits())
                                        >= 0))
                .map(Map.Entry::getKey)
                .toList();
        var result = strategy.result(node.id());
        recommendations.add(
            new Recommendation(
                node.id(),
                cycles,
                local,
                CertifiedMethodProtocol.Bounds.probability(result.value(), 2),
                result.status(),
                result.methodIds()));
      }
    }
    return new View(
        VERSION,
        job.id,
        job.revision,
        job.generation,
        job.status,
        job.reason,
        job.graph.complete(),
        !job.reason.equals("STORAGE_BUDGET") && job.graph.canResume(),
        job.graph.operations(),
        job.graph.retainedStates(),
        nodes,
        arrows,
        recommendations);
  }

  @Override
  public synchronized void close() {
    jobs.values().forEach(j -> j.graph.close());
    jobs.clear();
  }
}
