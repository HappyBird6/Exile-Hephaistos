package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Cooperative method-depth discovery. Display grouping must not replace these full item states. */
public final class AutomaticMethodGraph implements AutoCloseable {
  public static final String VERSION = "automatic-full-state-method-graph-v1";

  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  public record NodeView(
      int id,
      int depth,
      ItemState item,
      Progress progress,
      boolean discoveryFinished,
      boolean directFinished) {}

  public record MethodView(
      String id,
      int source,
      AutomaticMethodExecution.Template template,
      Map<
              Integer,
              CertifiedMethodRunner.Observation<
                  AutomaticMethodExecution.State, AutomaticCraftingAction>>
          points,
      Set<String> unresolvedReasons,
      boolean finished) {}

  private final AutomaticCraftingKernel kernel;
  private final Function<ItemState, Progress> evaluate;
  private final CertifiedInterval.Precision precision;
  private final List<AutomaticCraftingAction> candidates;
  private final Map<ItemState, Node> nodes = new LinkedHashMap<>();
  private final List<Node> orderedNodes = new ArrayList<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private int stateLimit, methodLimit;
  private long retainedLimit;
  private long primitiveOutcomeBudget;
  private long operations, revision;
  private boolean limited;

  private final class Node {
    final int id;
    final ItemState item;
    final Progress progress;
    final List<AutomaticCraftingAction> applicable = new ArrayList<>();
    final Set<AutomaticCraftingAction> allowedByPolicy = new HashSet<>();
    final List<Method> outgoing = new ArrayList<>();
    int depth, scan, pairs;
    boolean discovered;

    Node(ItemState item, int depth) {
      id = orderedNodes.size();
      this.item = item;
      this.depth = depth;
      progress = evaluate.apply(item);
      discovered = progress.goal() == Status.MATCH;
    }
  }

  private final class Method {
    final String id;
    final Node source;
    final AutomaticMethodExecution.Template template;
    final AutomaticMethodExecution execution;
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
              primitiveOutcomeBudget);
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
    intern(root, 0);
  }

  private Node intern(ItemState item, int depth) {
    var node = nodes.get(item);
    if (node != null) {
      node.depth = Math.min(node.depth, depth);
      return node;
    }
    if (nodes.size() >= stateLimit) {
      limited = true;
      return null;
    }
    node = new Node(item, depth);
    nodes.put(item, node);
    orderedNodes.add(node);
    revision++;
    return node;
  }

  private Optional<Exit> classify(Node source, ItemState item) {
    var now = evaluate.apply(item);
    if (now.goal() == Status.MATCH) return Optional.of(Exit.GOAL);
    if (now.goal() == Status.UNKNOWN
        || now.goal() == Status.UNSUPPORTED
        || now.entries().stream().anyMatch(s -> s == Status.UNKNOWN || s == Status.UNSUPPORTED))
      return Optional.of(Exit.UNRESOLVED);
    if (!source.progress.entries().equals(now.entries())) {
      for (int i = 0; i < now.entries().size(); i++)
        if (source.progress.entries().get(i) == Status.MATCH
            && now.entries().get(i) != Status.MATCH) return Optional.of(Exit.LOSS);
      return Optional.of(Exit.CHECKPOINT);
    }
    if (source.item.rarity() != item.rarity()
        || source.item.explicits().size() != item.explicits().size())
      return Optional.of(Exit.CHECKPOINT);
    return Optional.empty();
  }

  /** One discovery cursor advance plus one method transport slice; expensive methods yield. */
  public void step() {
    if (limited || finished()) return;
    operations++;
    var source =
        orderedNodes.stream()
            .filter(n -> !n.discovered)
            .min(Comparator.comparingInt(n -> n.depth))
            .orElse(null);
    if (source != null) discover(source);
    if (limited || ready.isEmpty()) return;
    var method = ready.removeFirst();
    if (method.execution.atBoundary()) {
      if (method.execution.absorbed()) {
        for (int horizon : List.of(100, 300, 500))
          if (!method.points.containsKey(horizon))
            publish(
                method, horizon, method.execution.observation().atLaterAbsorbedBoundary(horizon));
        method.finished = true;
        revision++;
        return;
      }
      int cycle = Math.toIntExact(method.execution.cycles());
      if (List.of(100, 300, 500).contains(cycle) && !method.points.containsKey(cycle))
        publish(method, cycle, method.execution.observation());
      if (cycle == 500) {
        method.finished = true;
        revision++;
        return;
      }
      method.execution.beginCycle();
    }
    if (!method.execution.atBoundary()) method.execution.step(1);
    ready.addLast(method);
    if (method.execution.resourceLimited()) limited = true;
    if (retainedStates() > retainedLimit) limited = true;
  }

  private void discover(Node source) {
    if (source.scan < candidates.size()) {
      var action = candidates.get(source.scan++);
      var law = kernel.expand(source.item, action, 1);
      if (law.status() == AutomaticCraftingKernel.Status.POLICY_REJECTED) return;
      source.allowedByPolicy.add(action);
      if (law.status() == AutomaticCraftingKernel.Status.UNAVAILABLE) return;
      if (!source.applicable.contains(action)) source.applicable.add(action);
      if (action.purpose() == AutomaticCurrencyPolicy.Purpose.ORDINARY)
        add(source, List.of(), List.of(action));
      if (limited) source.scan--;
      revision++;
      return;
    }
    int combinations = source.applicable.size() * candidates.size();
    if (source.pairs >= combinations) {
      source.discovered = true;
      revision++;
      return;
    }
    var first = source.applicable.get(source.pairs / candidates.size());
    var next = candidates.get(source.pairs++ % candidates.size());
    if (!source.allowedByPolicy.contains(next)) return;
    if (next.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL) return;
    if (first.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL) {
      if (!next.currency().replacementModifiers().isEmpty()) {
        add(source, List.of(first), List.of(next));
        if (!limited) add(source, List.of(), List.of(first, next));
      }
    } else if (!first.equals(next)) {
      add(source, List.of(first), List.of(next));
      if (!limited) add(source, List.of(), List.of(first, next));
    }
    if (limited) source.pairs--;
  }

  private void add(
      Node source, List<AutomaticCraftingAction> initial, List<AutomaticCraftingAction> cycle) {
    String id = source.id + ":" + initial + ">" + cycle;
    if (methods.containsKey(id)) return;
    if (methods.size() >= methodLimit) {
      limited = true;
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
    point
        .exits()
        .forEach(
            (kind, law) -> {
              if (kind == Exit.UNRESOLVED || kind == Exit.DEAD) return;
              law.forEach(
                  (state, mass) -> {
                    if (!mass.zero()) intern(state.item(), method.source.depth + 1);
                  });
            });
    point
        .active()
        .forEach(
            (state, mass) -> {
              if (!mass.zero()) intern(state.item(), method.source.depth + 1);
            });
    revision++;
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
    for (var method : this.methods.values())
      for (var point : method.points.entrySet()) publish(method, point.getKey(), point.getValue());
    if (retainedStates() > retainedLimit) limited = true;
  }

  public boolean resourceLimited() {
    return limited;
  }

  public long operations() {
    return operations;
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
                    n.progress,
                    n.discovered,
                    n.discovered && n.outgoing.stream().allMatch(m -> m.finished)))
        .toList();
  }

  public List<MethodView> methods() {
    return methods.values().stream()
        .map(
            m ->
                new MethodView(
                    m.id,
                    m.source.id,
                    m.template,
                    Map.copyOf(m.points),
                    m.execution.unresolvedReasons(),
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
          continue;
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
                          else if (kind == Exit.DEAD)
                            outcomes.merge(-1, mass, CertifiedInterval::add);
                          else {
                            var target = nodes.get(state.item());
                            if (target == null) unknown[0] = unknown[0].add(mass);
                            else outcomes.merge(target.id, mass, CertifiedInterval::add);
                          }
                        }));
        point
            .active()
            .forEach(
                (state, mass) -> {
                  var target = nodes.get(state.item());
                  if (target == null) unknown[0] = unknown[0].add(mass);
                  else outcomes.merge(target.id, mass, CertifiedInterval::add);
                });
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

  @Override
  public void close() {
    methods.values().forEach(m -> m.execution.close());
    ready.clear();
  }
}
