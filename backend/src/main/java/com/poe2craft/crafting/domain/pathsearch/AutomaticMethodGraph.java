package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Cooperative method-depth discovery. Display grouping must not replace these full item states. */
public final class AutomaticMethodGraph implements AutoCloseable {
  public static final String VERSION = "goal-directed-milestone-graph-v8-prepared-renewal";

  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  public record NodeView(
      int id,
      int depth,
      ItemState item,
      Map<String, AutomaticCraftingKernel.RollBand> rolls,
      boolean quality40,
      Progress progress,
      boolean discoveryFinished,
      boolean directFinished) {}

  public record MethodView(
      String id,
      int source,
      AutomaticMethodExecution.Template template,
      String executionMode,
      Map<
              Integer,
              CertifiedMethodRunner.Observation<
                  AutomaticMethodExecution.State, AutomaticCraftingAction>>
          points,
      Set<String> unresolvedReasons,
      Set<Integer> completeObservations,
      Set<ItemState> retryRepresentatives,
      boolean finished) {}

  private final AutomaticCraftingKernel kernel;
  private final Function<ItemState, Progress> evaluate;
  private final CertifiedInterval.Precision precision;
  private List<AutomaticCraftingAction> candidates;

  public record Identity(ItemState item, boolean quality40) {}

  private final Map<Identity, Node> nodes = new LinkedHashMap<>();
  private final List<Node> orderedNodes = new ArrayList<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private int stateLimit, methodLimit;
  private long retainedLimit;
  private long primitiveOutcomeBudget;
  private long operations, revision;
  private boolean limited;
  private boolean discoveryLimited;

  private final class Node {
    final int id;
    final ItemState item;
    final boolean quality40;
    final Progress progress;
    final List<AutomaticCraftingAction> applicable = new ArrayList<>();
    final Set<AutomaticCraftingAction> allowedByPolicy = new HashSet<>();
    final List<Method> outgoing = new ArrayList<>();
    int depth, scan, pairs;
    List<GoalDirectedPlanner.Choice> choices;
    boolean discovered;

    Node(ItemState item, boolean quality40, int depth) {
      id = orderedNodes.size();
      this.item = item;
      this.quality40 = quality40;
      this.depth = depth;
      var raw = evaluate.apply(item);
      progress =
          !quality40 && AutomaticCraftingKernel.hasQualityCap(item) && raw.goal() == Status.MATCH
              ? new Progress(Status.NO_MATCH, raw.entries())
              : raw;
      discovered = progress.goal() == Status.MATCH;
    }
  }

  private final class Method {
    final String id;
    final Node source;
    final AutomaticMethodExecution.Template template;
    AutomaticMethodExecution execution;
    final Map<
            Integer,
            CertifiedMethodRunner.Observation<
                AutomaticMethodExecution.State, AutomaticCraftingAction>>
        points = new LinkedHashMap<>();
    boolean finished;

    Method(String id, Node source, AutomaticMethodExecution.Template template) {
      this.id = id;
      this.source = source;
      this.template = template;
      execution =
          new AutomaticMethodExecution(
              source.item,
              template,
              kernel,
              item -> classify(source, item),
              precision,
              primitiveOutcomeBudget,
              AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT,
              source.quality40);
    }
  }

  public AutomaticMethodGraph(
      ItemState root,
      AutomaticCurrencyPolicy policy,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Progress> evaluate,
      CertifiedInterval.Precision precision,
      int stateLimit,
      int methodLimit,
      long retainedLimit,
      long primitiveOutcomeBudget) {
    this(
        root,
        policy,
        kernel,
        evaluate,
        precision,
        stateLimit,
        methodLimit,
        retainedLimit,
        primitiveOutcomeBudget,
        false);
  }

  public AutomaticMethodGraph(
      ItemState root,
      AutomaticCurrencyPolicy policy,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Progress> evaluate,
      CertifiedInterval.Precision precision,
      int stateLimit,
      int methodLimit,
      long retainedLimit,
      long primitiveOutcomeBudget,
      boolean initialQuality40) {
    if (stateLimit < 1 || methodLimit < 1 || retainedLimit < 1 || primitiveOutcomeBudget < 1)
      throw new IllegalArgumentException("Positive server resource budgets required");
    this.kernel = kernel;
    this.evaluate = evaluate;
    this.precision = precision;
    this.stateLimit = stateLimit;
    this.methodLimit = methodLimit;
    this.retainedLimit = retainedLimit;
    this.primitiveOutcomeBudget = primitiveOutcomeBudget;
    candidates = AutomaticCraftingAction.candidates(policy);
    if (!policy.withinExplorationBoundary(root.explicits().size()))
      throw new IllegalArgumentException("Root exceeds the explicit exploration boundary");
    intern(root, initialQuality40, 0);
  }

  private Node intern(ItemState item, boolean quality40, int depth) {
    var key = new Identity(item, quality40);
    var node = nodes.get(key);
    if (node != null) {
      if (depth < node.depth) {
        node.depth = depth;
        relaxDepths(node);
      }
      return node;
    }
    // Absorbed goal outcomes need no successor search. Preserve their exact item identity,
    // but charge them to retained storage rather than the expandable-state budget.
    boolean terminalGoal =
        evaluate.apply(item).goal() == Status.MATCH
            && (quality40 || !AutomaticCraftingKernel.hasQualityCap(item));
    if ((!terminalGoal && expandableStates() >= stateLimit) || retainedStates() >= retainedLimit) {
      limited = true;
      return null;
    }
    node = new Node(item, quality40, depth);
    nodes.put(key, node);
    orderedNodes.add(node);
    revision++;
    return node;
  }

  private void relaxDepths(Node first) {
    var queue = new ArrayDeque<Node>();
    queue.add(first);
    while (!queue.isEmpty()) {
      var source = queue.removeFirst();
      for (var method : source.outgoing)
        for (var point : visiblePoints(method).values()) {
          var targets = new HashSet<AutomaticMethodExecution.State>(point.active().keySet());
          point
              .exits()
              .forEach(
                  (kind, law) -> {
                    if (kind != Exit.DEAD)
                      law.keySet().stream()
                          .filter(
                              state ->
                                  kind != Exit.UNRESOLVED
                                      || (state.temporaryPreparation()
                                          && evaluate.apply(state.item()).goal() == Status.MATCH))
                          .forEach(targets::add);
                  });
          for (var state : targets) {
            var target = nodes.get(new Identity(state.item(), state.quality40()));
            if (target != null && target.depth > source.depth + 1) {
              target.depth = source.depth + 1;
              queue.addLast(target);
            }
          }
        }
    }
  }

  private Optional<Exit> classify(Node source, ItemState item) {
    var now = evaluate.apply(item);
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.entries().size() != source.progress.entries().size())
      return Optional.of(Exit.UNRESOLVED);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    for (int i = 0; i < now.entries().size(); i++)
      if (source.progress.entries().get(i) == Status.MATCH && now.entries().get(i) != Status.MATCH)
        return Optional.of(Exit.LOSS);
    if (GoalDirectedPlanner.matched(now) > GoalDirectedPlanner.matched(source.progress)) {
      return Optional.of(Exit.CHECKPOINT);
    }
    return Optional.empty();
  }

  /** One discovery cursor advance plus one method transport slice; expensive methods yield. */
  public void step() {
    if (limited || finished()) return;
    if (discoveryLimited && ready.isEmpty()) {
      limited = true;
      return;
    }
    operations++;
    var source =
        discoveryLimited
            ? null
            : orderedNodes.stream()
                .filter(n -> !n.discovered)
                .min(
                    Comparator.<Node>comparingInt(n -> n.scan)
                        .thenComparingLong(
                            n ->
                                -n.progress.entries().stream()
                                    .filter(s -> s == Status.MATCH)
                                    .count())
                        .thenComparingInt(n -> n.depth))
                .orElse(null);
    if (source != null) discover(source);
    if (limited || ready.isEmpty()) return;
    var method = ready.removeFirst();
    if (method.execution.resourceLimited()) {
      ready.addLast(method);
      if (source == null && ready.stream().allMatch(m -> m.execution.resourceLimited()))
        limited = true;
      return;
    }
    if (!method.execution.hasPrimitiveInFlight()
        && ready.stream().filter(m -> m.execution.hasPrimitiveInFlight()).count() >= 2) {
      ready.addLast(method);
      return;
    }
    if (method.execution.atBoundary()) {
      if (method.execution.absorbed()) {
        for (int horizon : List.of(1, 100, 300, 500))
          if (!method.points.containsKey(horizon) && horizon >= method.execution.cycles())
            publish(
                method, horizon, method.execution.observation().atLaterAbsorbedBoundary(horizon));
        method.finished = true;
        revision++;
        return;
      }
      int cycle = Math.toIntExact(method.execution.cycles());
      if (List.of(1, 100, 300, 500).contains(cycle) && !method.points.containsKey(cycle))
        publish(method, cycle, method.execution.observation());
      if (cycle == 500) {
        method.finished = true;
        revision++;
        return;
      }
      method.execution.beginCycle();
    }
    if (!method.execution.atBoundary()) method.execution.step(64);
    ready.addLast(method);
    if (retainedStates() > retainedLimit) limited = true;
  }

  private void discover(Node source) {
    if (source.choices == null)
      source.choices = new GoalDirectedPlanner(kernel, evaluate, candidates).choices(source.item);
    if (source.scan >= source.choices.size()) {
      source.discovered = true;
      revision++;
      return;
    }
    var choice = source.choices.get(source.scan++);
    add(source, choice.initial(), choice.cycle());
    if (limited || discoveryLimited) source.scan--;
    revision++;
  }

  private void add(
      Node source, List<AutomaticCraftingAction> initial, List<AutomaticCraftingAction> cycle) {
    String id = source.id + ":" + initial + ">" + cycle;
    if (methods.containsKey(id)) return;
    if (methods.size() >= methodLimit) {
      discoveryLimited = true;
      return;
    }
    var method = new Method(id, source, new AutomaticMethodExecution.Template(id, initial, cycle));
    methods.put(id, method);
    source.outgoing.add(method);
    ready.addLast(method);
    revision++;
  }

  private void publish(
      Method method,
      int cycles,
      CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
          point) {
    method.points.put(cycles, point);
    publishTargets(method, point);
    revision++;
  }

  private void publishTargets(
      Method method,
      CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
          point) {
    point
        .exits()
        .forEach(
            (kind, law) -> {
              if (kind == Exit.DEAD || kind == Exit.LOSS) return;
              law.forEach(
                  (state, mass) -> {
                    if (!mass.zero()
                        && (kind != Exit.UNRESOLVED
                            || (state.temporaryPreparation()
                                && evaluate.apply(state.item()).goal() == Status.MATCH)))
                      intern(state.item(), state.quality40(), method.source.depth + 1);
                  });
            });
    point
        .active()
        .forEach(
            (state, mass) -> {
              // This is a law-equivalent retry class, not a particular rolled item.
              if (!mass.zero() && !method.execution.retryRepresentatives().contains(state.item()))
                intern(state.item(), state.quality40(), method.source.depth + 1);
            });
  }

  public void publishProgress() {
    for (var method : methods.values())
      if (!method.finished) publishTargets(method, method.execution.provisional(500));
  }

  private Map<
          Integer,
          CertifiedMethodRunner.Observation<
              AutomaticMethodExecution.State, AutomaticCraftingAction>>
      visiblePoints(Method method) {
    var points = new LinkedHashMap<>(method.points);
    for (int horizon : List.of(1, 100, 300, 500))
      if (!points.containsKey(horizon) && horizon >= method.execution.cycles())
        points.put(horizon, method.execution.provisional(horizon));
    return Map.copyOf(points);
  }

  public boolean finished() {
    return !limited && ready.isEmpty() && orderedNodes.stream().allMatch(n -> n.discovered);
  }

  public boolean complete() {
    return finished()
        && orderedNodes.stream()
            .allMatch(
                n -> n.progress.goal() != Status.UNKNOWN && n.progress.goal() != Status.UNSUPPORTED)
        && methods.values().stream()
            .allMatch(
                m ->
                    m.points.values().stream()
                        .allMatch(
                            p ->
                                p.exits().get(Exit.UNRESOLVED).values().stream()
                                    .allMatch(CertifiedInterval::zero)));
  }

  public void increaseLimits(int states, int methods, long retained) {
    increaseLimits(states, methods, retained, primitiveOutcomeBudget);
  }

  public void increaseLimits(int states, int methods, long retained, long primitiveOutcomes) {
    if (states < stateLimit || methods < methodLimit || retained < retainedLimit)
      throw new IllegalArgumentException("Resource budgets cannot shrink on resume");
    if (primitiveOutcomes < primitiveOutcomeBudget)
      throw new IllegalArgumentException("Primitive budget cannot shrink on resume");
    primitiveOutcomeBudget = primitiveOutcomes;
    this.methods.values().forEach(m -> m.execution.increasePrimitiveBudget(primitiveOutcomes));
    stateLimit = states;
    methodLimit = methods;
    retainedLimit = retained;
    limited = false;
    discoveryLimited = false;
    for (var method : this.methods.values())
      for (var point : method.points.entrySet()) publish(method, point.getKey(), point.getValue());
    if (retainedStates() > retainedLimit) limited = true;
  }

  public boolean resourceLimited() {
    return limited;
  }

  private long expandableStates() {
    return orderedNodes.stream().filter(n -> n.progress.goal() != Status.MATCH).count();
  }

  public boolean canResume() {
    if (limited && expandableStates() >= stateLimit && stateLimit >= 128) return false;
    if (discoveryLimited && methodLimit >= 256) return false;
    if (retainedStates() > retainedLimit && retainedLimit >= 50000) return false;
    return !limited
        || primitiveOutcomeBudget < 10000
        || ready.stream().anyMatch(m -> !m.execution.resourceLimited());
  }

  public long operations() {
    return operations;
  }

  public boolean hasGoal() {
    return orderedNodes.stream().anyMatch(n -> n.progress.goal() == Status.MATCH);
  }

  public long revision() {
    return revision;
  }

  public long retainedStates() {
    return nodes.size()
        + methods.size()
        + methods.values().stream().mapToLong(m -> m.execution.retainedStates()).sum();
  }

  public List<NodeView> nodes() {
    return orderedNodes.stream()
        .map(
            n ->
                new NodeView(
                    n.id,
                    n.depth,
                    n.item,
                    kernel.rollBands(n.item),
                    n.quality40,
                    n.progress,
                    n.discovered,
                    n.discovered && n.outgoing.stream().allMatch(m -> m.finished)))
        .toList();
  }

  private String executionMode(Method method) {
    if (method.execution.cycles() > 1) return "REPEATABLE";
    if (method.execution.atBoundary() && method.execution.absorbed()) return "SINGLE_PASS";
    var first = method.points.get(1);
    return first != null && !first.active().isEmpty() ? "REPEATABLE" : "PENDING";
  }

  private Set<Integer> completeObservations(Method method) {
    var complete = new HashSet<>(method.points.keySet());
    // Older persisted single-pass jobs have only 100/300/500 snapshots. Their
    // absorbed execution still holds the exact first pass, so expose it safely.
    if (method.execution.atBoundary()
        && method.execution.absorbed()
        && method.execution.cycles() <= 1) complete.add(1);
    return Set.copyOf(complete);
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    m.source.id,
                    m.template,
                    executionMode(m),
                    visiblePoints(m),
                    m.execution.unresolvedReasons(),
                    completeObservations(m),
                    m.execution.retryRepresentatives(),
                    m.finished))
        .toList();
  }

  /**
   * Alternatives remain separate choices; only mutually exclusive exits of one method are summed.
   */
  public CertifiedOutcomeStrategy<Integer> strategy(int cycles) {
    if (!List.of(100, 300, 500).contains(cycles))
      throw new IllegalArgumentException("Unsupported observation");
    int bits = precision.fractionalBits();
    var graph = new LinkedHashMap<Integer, CertifiedOutcomeStrategy.State<Integer>>();
    graph.put(-1, new CertifiedOutcomeStrategy.State<>(false, false, List.of()));
    for (var source : orderedNodes) {
      var choices = new ArrayList<CertifiedOutcomeStrategy.Choice<Integer>>();
      boolean open =
          !source.discovered
              || source.progress.goal() == Status.UNKNOWN
              || source.progress.goal() == Status.UNSUPPORTED;
      for (var method : source.outgoing) {
        var point = method.points.get(cycles);
        if (point == null) {
          open = true;
          point = method.execution.provisional(cycles);
        }
        var outcomes = new LinkedHashMap<Integer, CertifiedInterval>();
        var unknown = new CertifiedInterval[] {CertifiedInterval.zero(bits)};
        point
            .exits()
            .forEach(
                (kind, law) ->
                    law.forEach(
                        (state, mass) -> {
                          if (kind == Exit.UNRESOLVED) unknown[0] = unknown[0].add(mass);
                          else if (kind == Exit.DEAD || kind == Exit.LOSS)
                            outcomes.merge(-1, mass, CertifiedInterval::add);
                          else {
                            var target = nodes.get(new Identity(state.item(), state.quality40()));
                            if (target == null) unknown[0] = unknown[0].add(mass);
                            else outcomes.merge(target.id, mass, CertifiedInterval::add);
                          }
                        }));
        point
            .active()
            .forEach(
                (state, mass) -> {
                  var target = nodes.get(new Identity(state.item(), state.quality40()));
                  if (target == null) unknown[0] = unknown[0].add(mass);
                  else outcomes.merge(target.id, mass, CertifiedInterval::add);
                });
        // Exclusive arrivals can round their summed upper bound slightly above one.
        outcomes.replaceAll((target, mass) -> mass.probability());
        choices.add(
            new CertifiedOutcomeStrategy.Choice<>(method.id, outcomes, unknown[0].probability()));
      }
      graph.put(
          source.id,
          new CertifiedOutcomeStrategy.State<>(
              source.progress.goal() == Status.MATCH, open, choices));
    }
    return new CertifiedOutcomeStrategy<>(graph, bits);
  }

  public record Mass(
      Exit exit, AutomaticMethodExecution.State state, CertifiedInterval probability) {}

  public record Use(AutomaticCraftingAction action, CertifiedInterval quantity, boolean initial) {}

  public record Point(long cycles, List<Mass> outcomes, List<Use> uses, CertifiedInterval mass) {
    public Point {
      outcomes = List.copyOf(outcomes);
      uses = List.copyOf(uses);
    }
  }

  public static Point encode(
      CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
          point) {
    var outcomes = new ArrayList<Mass>();
    point
        .exits()
        .forEach(
            (kind, law) -> law.forEach((state, mass) -> outcomes.add(new Mass(kind, state, mass))));
    point.active().forEach((state, mass) -> outcomes.add(new Mass(null, state, mass)));
    var uses = new ArrayList<Use>();
    point.initialUses().forEach((a, mass) -> uses.add(new Use(a, mass, true)));
    point.cycleUses().forEach((a, mass) -> uses.add(new Use(a, mass, false)));
    return new Point(point.cycles(), outcomes, uses, point.mass());
  }

  private static CertifiedMethodRunner.Observation<
          AutomaticMethodExecution.State, AutomaticCraftingAction>
      decode(Point point) {
    var exits =
        new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(Exit.class);
    for (var kind : Exit.values()) exits.put(kind, new LinkedHashMap<>());
    var active = new LinkedHashMap<AutomaticMethodExecution.State, CertifiedInterval>();
    for (var row : point.outcomes())
      (row.exit() == null ? active : exits.get(row.exit())).put(row.state(), row.probability());
    var initial = new LinkedHashMap<AutomaticCraftingAction, CertifiedInterval>();
    var cycle = new LinkedHashMap<AutomaticCraftingAction, CertifiedInterval>();
    for (var row : point.uses())
      (row.initial() ? initial : cycle).put(row.action(), row.quantity());
    return new CertifiedMethodRunner.Observation<>(
        point.cycles(), exits, active, initial, cycle, point.mass());
  }

  public record NodeCursor(
      ItemState item,
      boolean quality40,
      int depth,
      int scan,
      int pairs,
      boolean discovered,
      List<AutomaticCraftingAction> applicable,
      Set<AutomaticCraftingAction> allowed) {}

  public record MethodCursor(
      String id,
      int source,
      AutomaticMethodExecution.Checkpoint execution,
      List<Point> points,
      boolean finished) {}

  public record Checkpoint(
      String version,
      String objective,
      AutomaticCraftingKernel.Identity identity,
      CertifiedInterval.Precision precision,
      int states,
      int methods,
      long retained,
      long primitive,
      long operations,
      long revision,
      boolean limited,
      boolean discoveryLimited,
      List<AutomaticCraftingAction> candidates,
      List<NodeCursor> nodes,
      List<MethodCursor> executions,
      List<String> ready) {}

  public Checkpoint checkpoint(String objective) {
    return new Checkpoint(
        VERSION,
        objective,
        kernel.identity(),
        precision,
        stateLimit,
        methodLimit,
        retainedLimit,
        primitiveOutcomeBudget,
        operations,
        revision,
        limited,
        discoveryLimited,
        candidates,
        orderedNodes.stream()
            .map(
                n ->
                    new NodeCursor(
                        n.item,
                        n.quality40,
                        n.depth,
                        n.scan,
                        n.pairs,
                        n.discovered,
                        List.copyOf(n.applicable),
                        Set.copyOf(n.allowedByPolicy)))
            .toList(),
        methods.values().stream()
            .map(
                m ->
                    new MethodCursor(
                        m.id,
                        m.source.id,
                        m.execution.checkpoint(objective).withIdentity(null),
                        m.points.values().stream().map(AutomaticMethodGraph::encode).toList(),
                        m.finished))
            .toList(),
        ready.stream().map(m -> m.id).toList());
  }

  public static AutomaticMethodGraph restore(
      Checkpoint saved,
      String objective,
      AutomaticCurrencyPolicy policy,
      AutomaticCraftingKernel kernel,
      Function<ItemState, Progress> evaluate) {
    if (!VERSION.equals(saved.version())
        || !objective.equals(saved.objective())
        || !kernel.identity().equals(saved.identity())
        || saved.nodes().isEmpty())
      throw new IllegalArgumentException("Graph requires explicit recalculation");
    var graph =
        new AutomaticMethodGraph(
            saved.nodes().getFirst().item(),
            policy,
            kernel,
            evaluate,
            saved.precision(),
            saved.states(),
            saved.methods(),
            saved.retained(),
            saved.primitive());
    graph.nodes.clear();
    graph.orderedNodes.clear();
    if (saved.candidates().size() != graph.candidates.size()
        || !new HashSet<>(saved.candidates()).equals(new HashSet<>(graph.candidates)))
      throw new IllegalArgumentException("Saved candidate inventory differs from policy");
    graph.candidates = List.copyOf(saved.candidates());
    for (var row : saved.nodes()) {
      var node = graph.intern(row.item(), row.quality40(), row.depth());
      if (node == null) throw new IllegalArgumentException("Invalid saved state budget");
      node.scan = row.scan();
      node.pairs = row.pairs();
      node.discovered = row.discovered();
      node.applicable.addAll(row.applicable());
      node.allowedByPolicy.addAll(row.allowed());
    }
    for (var row : saved.executions()) {
      var source = graph.orderedNodes.get(row.source());
      var method = graph.new Method(row.id(), source, row.execution().template());
      method.execution.close();
      method.execution =
          AutomaticMethodExecution.restore(
              row.execution().withIdentity(saved.identity()),
              kernel,
              objective,
              item -> graph.classify(source, item));
      method.finished = row.finished();
      row.points().forEach(p -> method.points.put(Math.toIntExact(p.cycles()), decode(p)));
      graph.methods.put(method.id, method);
      source.outgoing.add(method);
    }
    for (var id : saved.ready()) graph.ready.addLast(Objects.requireNonNull(graph.methods.get(id)));
    graph.operations = saved.operations();
    graph.revision = saved.revision();
    graph.limited = saved.limited();
    graph.discoveryLimited = saved.discoveryLimited();
    return graph;
  }

  @Override
  public void close() {
    methods.values().forEach(m -> m.execution.close());
    ready.clear();
  }
}
