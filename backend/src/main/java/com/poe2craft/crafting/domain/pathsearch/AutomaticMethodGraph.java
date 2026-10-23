package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.Exit;
import com.poe2craft.item.ItemState;
import java.util.*;
import java.util.function.Function;

/** Cooperative discovery over conditional item laws; distinct joint laws never share identity. */
public final class AutomaticMethodGraph implements AutoCloseable {
  public static final String VERSION = "goal-directed-milestone-graph-v12-joint-attribute-laws";

  public record Progress(Status goal, List<Status> entries) {
    public Progress {
      entries = List.copyOf(entries);
    }
  }

  public record TierFraction(String numerator, String denominator) {}

  public record TierVariant(
      String modifierId, String stat, long min, long max, TierFraction probability) {}

  public record TierLaw(List<TierVariant> variants) {
    static TierLaw from(TierCell cell) {
      return new TierLaw(
          cell.variants().stream()
              .map(
                  v ->
                      new TierVariant(
                          v.modifierId(),
                          v.stat(),
                          v.min(),
                          v.max(),
                          new TierFraction(
                              v.probability().numerator().toString(),
                              v.probability().denominator().toString())))
              .toList());
    }

    TierCell cell() {
      return new TierCell(
          variants.stream()
              .map(
                  v ->
                      new TierCell.Variant(
                          v.modifierId(),
                          v.stat(),
                          v.min(),
                          v.max(),
                          new com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution
                              .Fraction(
                              new java.math.BigInteger(v.probability().numerator()),
                              new java.math.BigInteger(v.probability().denominator()))))
              .toList());
    }
  }

  public static Map<String, TierLaw> tierView(Map<String, TierCell> tiers) {
    var result = new LinkedHashMap<String, TierLaw>();
    tiers.forEach((id, cell) -> result.put(id, TierLaw.from(cell)));
    return Map.copyOf(result);
  }

  public record MemberView(
      ItemState item,
      Map<String, TierLaw> tiers,
      Map<String, AutomaticCraftingKernel.RollBand> rolls) {}

  public record AttributeOption(
      String modifierId, String stat, long min, long max, int tier, String role) {}

  /** Conditional source projection; row weights remain in the named method's observations. */
  public record Origin(String method, int source, String kind, Map<String, String> retained) {
    public Origin {
      retained = Map.copyOf(retained);
    }
  }

  private record ClassKey(Object item, boolean quality40, int abstractQuality, Progress progress) {}

  public record NodeView(
      int id,
      int depth,
      ItemState item,
      Map<String, TierLaw> tiers,
      Map<String, AutomaticCraftingKernel.RollBand> rolls,
      boolean quality40,
      int abstractQuality,
      Progress progress,
      boolean discoveryFinished,
      boolean directFinished,
      List<MemberView> members,
      Map<String, List<AttributeOption>> attributes,
      Set<Origin> joint) {
    public NodeView {
      tiers = tiers == null ? Map.of() : Map.copyOf(tiers);
      members = members == null ? List.of() : List.copyOf(members);
      attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
      joint = joint == null ? Set.of() : Set.copyOf(joint);
    }

    public NodeView(
        int id,
        int depth,
        ItemState item,
        Map<String, TierLaw> tiers,
        Map<String, AutomaticCraftingKernel.RollBand> rolls,
        boolean quality40,
        int abstractQuality,
        Progress progress,
        boolean discoveryFinished,
        boolean directFinished) {
      this(
          id,
          depth,
          item,
          tiers,
          rolls,
          quality40,
          abstractQuality,
          progress,
          discoveryFinished,
          directFinished,
          List.of(),
          Map.of(),
          Set.of());
    }

    public Map<String, TierCell> tierCells() {
      var result = new LinkedHashMap<String, TierCell>();
      tiers.forEach((id, law) -> result.put(id, law.cell()));
      return Map.copyOf(result);
    }

    public NodeView(
        int id,
        int depth,
        ItemState item,
        Map<String, AutomaticCraftingKernel.RollBand> rolls,
        boolean quality40,
        int abstractQuality,
        Progress progress,
        boolean discoveryFinished,
        boolean directFinished) {
      this(
          id,
          depth,
          item,
          Map.of(),
          rolls,
          quality40,
          abstractQuality,
          progress,
          discoveryFinished,
          directFinished);
    }

    public NodeView(
        int id,
        int depth,
        ItemState item,
        Map<String, AutomaticCraftingKernel.RollBand> rolls,
        boolean quality40,
        Progress progress,
        boolean discoveryFinished,
        boolean directFinished) {
      this(
          id,
          depth,
          item,
          rolls,
          quality40,
          quality40 ? 40 : 0,
          progress,
          discoveryFinished,
          directFinished);
    }
  }

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

  public record Identity(
      ItemState item, boolean quality40, int abstractQuality, Map<String, TierCell> tiers) {
    public Identity {
      tiers = tiers == null ? Map.of() : Map.copyOf(tiers);
    }

    public Identity(ItemState item, boolean quality40, int abstractQuality) {
      this(item, quality40, abstractQuality, Map.of());
    }

    public Identity(ItemState item, boolean quality40) {
      this(item, quality40, quality40 ? 40 : 0);
    }
  }

  private final Map<Identity, Node> nodes = new LinkedHashMap<>();
  private final Map<ClassKey, Node> classes = new LinkedHashMap<>();
  private final Map<TerminalAdditionClasses.Key, Node> terminalClasses = new LinkedHashMap<>();
  private final List<Node> orderedNodes = new ArrayList<>();
  private final Map<String, Method> methods = new LinkedHashMap<>();
  private final ArrayDeque<Method> ready = new ArrayDeque<>();
  private int stateLimit, methodLimit;
  private long retainedLimit;
  private long primitiveOutcomeBudget;
  private long operations, revision;
  private long proofRows, proofNanos;
  private boolean limited;
  private boolean discoveryLimited;

  private final class Node {
    final int id;
    final ItemState item;
    final Map<String, TierCell> tiers;
    final boolean quality40;
    final int abstractQuality;
    final Progress progress;
    final List<AutomaticCraftingAction> applicable = new ArrayList<>();
    final Set<AutomaticCraftingAction> allowedByPolicy = new HashSet<>();
    final List<Method> outgoing = new ArrayList<>();
    final Set<Identity> members = new LinkedHashSet<>();
    final Set<Origin> origins = new LinkedHashSet<>();
    int depth, scan, pairs;
    List<GoalDirectedPlanner.Choice> choices;
    boolean discovered;
    Node representative;
    TerminalAdditionClasses.Key terminalKey;

    Node(
        ItemState item,
        boolean quality40,
        int abstractQuality,
        int depth,
        Map<String, TierCell> tiers) {
      this.tiers = Map.copyOf(tiers);
      id = orderedNodes.size();
      this.item = item;
      this.quality40 = quality40;
      this.abstractQuality = Math.max(quality40 ? 40 : 0, abstractQuality);
      this.depth = depth;
      var raw = progress(item, this.abstractQuality);
      progress =
          kernel.workedQuality(item, this.abstractQuality) < 40
                  && AutomaticCraftingKernel.hasQualityCap(item)
                  && raw.goal() == Status.MATCH
              ? new Progress(Status.NO_MATCH, raw.entries())
              : raw;
      discovered = progress.goal() == Status.MATCH;
      members.add(new Identity(item, quality40, this.abstractQuality, tiers));
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
    Method owner;

    boolean done() {
      return owner == null ? finished : owner.done();
    }

    Method(String id, Node source, AutomaticMethodExecution.Template template) {
      this.id = id;
      this.source = source;
      this.template = template;
      execution =
          new AutomaticMethodExecution(
              source.item,
              template,
              kernel,
              (item, quality) -> classify(source, item, quality),
              precision,
              primitiveOutcomeBudget,
              AutomaticMethodExecution.PreparationSuccess.STOP_ON_FIRST_HIT,
              source.quality40,
              source.abstractQuality,
              source.tiers,
              source.id != 0);
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
        initialQuality40,
        initialQuality40 ? 40 : 0);
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
      boolean initialQuality40,
      int initialAbstractQuality) {
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
    intern(root, initialQuality40, initialAbstractQuality, 0);
  }

  public Progress progress(ItemState item, int abstractQuality) {
    return qualityProgress(kernel, item, abstractQuality, evaluate.apply(item));
  }

  /** The saved-result reader uses the same quality predicate without restoring any execution. */
  public static Progress qualityProgress(
      AutomaticCraftingKernel kernel, ItemState item, int abstractQuality, Progress raw) {
    if (kernel.qualityGoal() == 0) return raw;
    boolean achieved = kernel.workedQuality(item, abstractQuality) >= kernel.qualityGoal();
    boolean supported =
        kernel.baseQualitySupported(item)
            && (kernel.qualityGoal() <= 20
                || com.poe2craft.item.BaseRegistry.maximumQualityBreach(item.baseItemId()));
    var quality = achieved ? Status.MATCH : supported ? Status.NO_MATCH : Status.UNSUPPORTED;
    var entries = new ArrayList<>(raw.entries());
    entries.add(quality);
    return new Progress(
        com.poe2craft.crafting.domain.goalfilter.GoalEvaluator.and(List.of(raw.goal(), quality)),
        entries);
  }

  private Node intern(ItemState item, boolean quality40, int abstractQuality, int depth) {
    return intern(item, quality40, abstractQuality, depth, Map.of());
  }

  private Node intern(
      ItemState item,
      boolean quality40,
      int abstractQuality,
      int depth,
      Map<String, TierCell> tiers) {
    var key = new Identity(item, quality40, Math.max(quality40 ? 40 : 0, abstractQuality), tiers);
    var node = nodes.get(key);
    if (node != null) {
      if (depth < node.depth) {
        node.depth = depth;
        relaxDepths(node);
      }
      return node;
    }
    node = new Node(item, quality40, abstractQuality, depth, tiers);
    var classKey =
        new ClassKey(
            node.progress.goal() == Status.MATCH
                ? kernel.attributeContext(item)
                : kernel.behaviorKey(item),
            quality40,
            node.abstractQuality,
            node.progress);
    if (depth > 0 && classes.containsKey(classKey)) return alias(classes.get(classKey), key, depth);
    if (depth > 0 && node.progress.goal() != Status.MATCH) {
      int work = node.abstractQuality;
      node.choices =
          new GoalDirectedPlanner(kernel, state -> progress(state, work), candidates).choices(item);
      var candidateNode = node;
      long proofStarted = System.nanoTime();
      var keyProof =
          TerminalAdditionClasses.prove(
              item,
              quality40,
              node.progress,
              node.choices,
              kernel,
              target -> classify(candidateNode, target, candidateNode.abstractQuality),
              primitiveOutcomeBudget,
              count -> proofRows += count);
      proofNanos += System.nanoTime() - proofStarted;
      if (keyProof.isPresent()) {
        node.terminalKey = keyProof.get();
        node.representative = terminalClasses.get(node.terminalKey);
        if (node.representative != null) node.terminalKey = node.representative.terminalKey;
      }
    }
    // Absorbed goal outcomes need no successor search. Preserve their exact item identity,
    // but charge them to retained storage rather than the expandable-state budget.
    boolean terminalGoal =
        evaluate.apply(item).goal() == Status.MATCH
            && (kernel.workedQuality(item, node.abstractQuality) >= 40
                || !AutomaticCraftingKernel.hasQualityCap(item));
    if ((!terminalGoal && node.representative == null && expandableStates() >= stateLimit)
        || retainedStates() >= retainedLimit) {
      limited = true;
      return null;
    }
    if (node.representative != null) {
      var representative = alias(node.representative, key, depth);
      if (representative != null) classes.put(classKey, representative);
      return representative;
    }
    if (node.terminalKey != null) terminalClasses.put(node.terminalKey, node);
    if (depth > 0) classes.put(classKey, node);
    nodes.put(key, node);
    orderedNodes.add(node);
    revision++;
    return node;
  }

  private Node alias(Node target, Identity member, int depth) {
    if (retainedStates() >= retainedLimit) {
      limited = true;
      return null;
    }
    target.members.add(member);
    nodes.put(member, target);
    if (depth < target.depth) {
      target.depth = depth;
      relaxDepths(target);
    }
    revision++;
    return target;
  }

  private void origin(
      Node target, Method method, AutomaticMethodExecution.State result, String kind) {
    if (target == null) return;
    var retained = new TreeMap<String, String>();
    for (var id : result.retainedFromRoot()) {
      var before =
          method.source.item.explicits().stream()
              .filter(m -> m.modifierId().equals(id))
              .findFirst()
              .orElseThrow();
      var role = kernel.attributeKey(before);
      target.item.explicits().stream()
          .filter(m -> kernel.attributeKey(m).equals(role))
          .findFirst()
          .ifPresent(m -> retained.put(m.modifierId(), id));
    }
    if (target.origins.add(new Origin(method.id, method.source.id, kind, retained))) revision++;
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
            var target =
                nodes.get(
                    new Identity(
                        state.item(), state.quality40(), state.abstractQuality(), state.tiers()));
            if (target != null && target.depth > source.depth + 1) {
              target.depth = source.depth + 1;
              queue.addLast(target);
            }
          }
        }
    }
  }

  private Optional<Exit> classify(Node source, ItemState item, int quality) {
    var now = progress(item, quality);
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
      if (cycle >= 100) {
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
      source.choices =
          new GoalDirectedPlanner(
                  kernel, item -> progress(item, source.abstractQuality), candidates)
              .choices(source.item);
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
    Method owner = null;
    if (source.representative != null) {
      add(source.representative, initial, cycle);
      owner = methods.get(source.representative.id + ":" + initial + ">" + cycle);
      if (owner == null) return;
    }
    if (owner == null && executionCount() >= methodLimit) {
      discoveryLimited = true;
      return;
    }
    var method = new Method(id, source, new AutomaticMethodExecution.Template(id, initial, cycle));
    methods.put(id, method);
    source.outgoing.add(method);
    if (owner == null) ready.addLast(method);
    else {
      method.execution.close();
      method.owner = owner;
      method.execution = owner.execution;
      visiblePoints(method).values().forEach(point -> publishTargets(method, point));
    }
    revision++;
  }

  private void publish(
      Method method,
      int cycles,
      CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
          point) {
    method.points.put(cycles, point);
    publishTargets(method, point);
    for (var alias : methods.values())
      if (alias.owner == method) publishTargets(alias, transport(alias, point));
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
                      origin(
                          intern(
                              state.item(),
                              state.quality40(),
                              state.abstractQuality(),
                              method.source.depth + 1,
                              state.tiers()),
                          method,
                          state,
                          kind.name());
                  });
            });
    // Failed attempts belong to this finite first-hit calculation. Keep their complete
    // states inside the execution, rather than launching new crafting paths from them.
    if (method.template.boundedChaos()) return;
    point
        .active()
        .forEach(
            (state, mass) -> {
              // This is a law-equivalent retry class, not a particular rolled item.
              if (!mass.zero() && !method.execution.retryRepresentatives().contains(state.item()))
                origin(
                    intern(
                        state.item(),
                        state.quality40(),
                        state.abstractQuality(),
                        method.source.depth + 1,
                        state.tiers()),
                    method,
                    state,
                    "ACTIVE");
            });
  }

  public void publishProgress() {
    for (var method : methods.values())
      if (!method.done())
        publishTargets(method, transport(method, method.execution.provisional(100)));
  }

  private Map<
          Integer,
          CertifiedMethodRunner.Observation<
              AutomaticMethodExecution.State, AutomaticCraftingAction>>
      visiblePoints(Method method) {
    if (method.owner != null) {
      var result =
          new LinkedHashMap<
              Integer,
              CertifiedMethodRunner.Observation<
                  AutomaticMethodExecution.State, AutomaticCraftingAction>>();
      visiblePoints(method.owner)
          .forEach((cycles, point) -> result.put(cycles, transport(method, point)));
      return Map.copyOf(result);
    }
    var points = new LinkedHashMap<>(method.points);
    for (int horizon : List.of(1, 100))
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
    this.methods.values().stream()
        .filter(m -> m.owner == null)
        .forEach(m -> m.execution.increasePrimitiveBudget(primitiveOutcomes));
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
    return orderedNodes.stream()
        .filter(n -> n.progress.goal() != Status.MATCH && n.representative == null)
        .count();
  }

  public boolean canResume() {
    if (limited && expandableStates() >= stateLimit && stateLimit >= 128) return false;
    if (discoveryLimited && methodLimit >= 256) return false;
    if (retainedStates() >= retainedLimit && retainedLimit >= 50000) return false;
    return !limited
        || primitiveOutcomeBudget < 10000
        || ready.stream().anyMatch(m -> !m.execution.resourceLimited());
  }

  public record ResourceUsage(
      long computationStates,
      int stateLimit,
      long methodExecutions,
      int methodLimit,
      int concreteStates,
      int concreteMethods,
      long retainedRecords,
      long retainedLimit,
      long primitiveOutcomeBudget,
      List<String> blockers) {}

  public ResourceUsage resources() {
    var blockers = new ArrayList<String>();
    if (limited && expandableStates() >= stateLimit) blockers.add("STATES");
    if (discoveryLimited) blockers.add("METHODS");
    if (limited && retainedStates() >= retainedLimit) blockers.add("RETAINED_RECORDS");
    if (limited && ready.stream().anyMatch(m -> m.execution.resourceLimited()))
      blockers.add("PRIMITIVE_OUTCOMES");
    return new ResourceUsage(
        expandableStates(),
        stateLimit,
        executionCount(),
        methodLimit,
        orderedNodes.size(),
        methods.size(),
        retainedStates(),
        retainedLimit,
        primitiveOutcomeBudget,
        List.copyOf(blockers));
  }

  public long executionCount() {
    return methods.values().stream().filter(m -> m.owner == null).count();
  }

  public long computationStates() {
    return expandableStates();
  }

  public long proofRows() {
    return proofRows;
  }

  public long proofNanos() {
    return proofNanos;
  }

  private CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
      transport(
          Method method,
          CertifiedMethodRunner.Observation<AutomaticMethodExecution.State, AutomaticCraftingAction>
              point) {
    if (method.owner == null) return point;
    var exits =
        new EnumMap<Exit, Map<AutomaticMethodExecution.State, CertifiedInterval>>(Exit.class);
    point.exits().forEach((kind, law) -> exits.put(kind, transportLaw(method, law)));
    return new CertifiedMethodRunner.Observation<>(
        point.cycles(),
        exits,
        transportLaw(method, point.active()),
        point.initialUses(),
        point.cycleUses(),
        point.mass());
  }

  private Map<AutomaticMethodExecution.State, CertifiedInterval> transportLaw(
      Method method, Map<AutomaticMethodExecution.State, CertifiedInterval> law) {
    var result = new LinkedHashMap<AutomaticMethodExecution.State, CertifiedInterval>();
    law.forEach(
        (state, mass) ->
            result.merge(
                new AutomaticMethodExecution.State(
                    TerminalAdditionClasses.transport(
                        method.owner.source.item, method.source.item, state.item()),
                    state.temporaryPreparation(),
                    state.quality40(),
                    state.abstractQuality(),
                    transportedTiers(method, state),
                    transportedRetention(method, state)),
                mass,
                CertifiedInterval::add));
    return result;
  }

  private Set<String> transportedRetention(Method method, AutomaticMethodExecution.State state) {
    var result = new HashSet<String>();
    for (var id : state.retainedFromRoot()) {
      var before =
          method.owner.source.item.explicits().stream()
              .filter(m -> m.modifierId().equals(id))
              .findFirst()
              .orElseThrow();
      method.source.item.explicits().stream()
          .filter(m -> kernel.attributeKey(m).equals(kernel.attributeKey(before)))
          .findFirst()
          .ifPresent(m -> result.add(m.modifierId()));
    }
    return Set.copyOf(result);
  }

  private Map<String, TierCell> transportedTiers(
      Method method, AutomaticMethodExecution.State state) {
    var tiers = new HashMap<>(state.tiers());
    method.owner.source.item.explicits().forEach(m -> tiers.remove(m.modifierId()));
    tiers.putAll(method.source.tiers);
    return Map.copyOf(tiers);
  }

  public long operations() {
    return operations;
  }

  /** Model declarations follow the executed laws, not the presence of a source unit label. */
  public List<String> rollModels() {
    return methods.values().stream()
        .filter(m -> m.owner == null)
        .flatMap(m -> m.execution.assumptions().stream())
        .map(com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption::id)
        .filter(com.poe2craft.crafting.domain.CoupledStatRollModel.ID::equals)
        .distinct()
        .sorted()
        .toList();
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
        + methods.values().stream()
            .filter(m -> m.owner == null)
            .mapToLong(m -> m.execution.retainedStates())
            .sum()
        + orderedNodes.stream().mapToLong(n -> n.origins.size()).sum()
        + terminalClasses.keySet().stream()
            .flatMap(k -> k.laws().stream())
            .mapToLong(l -> l.outcomes().size())
            .sum();
  }

  public List<NodeView> nodes() {
    var supports = attributeSupports();
    return orderedNodes.stream()
        .map(
            n ->
                new NodeView(
                    n.id,
                    n.depth,
                    n.item,
                    tierView(n.tiers),
                    kernel.rollBands(n.item),
                    n.quality40,
                    n.abstractQuality,
                    n.progress,
                    n.discovered,
                    n.discovered && n.outgoing.stream().allMatch(Method::done),
                    n.members.stream()
                        .map(
                            m ->
                                new MemberView(
                                    m.item(), tierView(m.tiers()), kernel.rollBands(m.item())))
                        .toList(),
                    supports.get(n.id),
                    Set.copyOf(n.origins)))
        .toList();
  }

  private Map<Integer, Map<String, List<AttributeOption>>> attributeSupports() {
    return attributeSupports(
        orderedNodes.stream()
            .map(n -> new AttributeSource(n.id, n.item, n.members, n.origins))
            .toList(),
        kernel);
  }

  private record AttributeSource(
      int id, ItemState item, Collection<Identity> members, Set<Origin> origins) {}

  /** Read-only projection uses saved members and lineage without restoring any execution. */
  public static Map<Integer, Map<String, List<AttributeOption>>> savedAttributeSupports(
      List<NodeCursor> nodes, AutomaticCraftingKernel kernel) {
    var sources = new ArrayList<AttributeSource>();
    for (int i = 0; i < nodes.size(); i++) {
      var n = nodes.get(i);
      if (n.members().isEmpty()) throw new IllegalArgumentException("Missing saved joint members");
      sources.add(new AttributeSource(i, n.item(), n.members(), n.joint()));
    }
    return attributeSupports(sources, kernel);
  }

  private static Map<Integer, Map<String, List<AttributeOption>>> attributeSupports(
      List<AttributeSource> sources, AutomaticCraftingKernel kernel) {
    var supports = new LinkedHashMap<Integer, Map<String, LinkedHashSet<AttributeOption>>>();
    for (var n : sources) {
      var fields = new LinkedHashMap<String, LinkedHashSet<AttributeOption>>();
      for (var primary : n.item.explicits())
        fields.put(primary.modifierId(), new LinkedHashSet<>());
      for (var member : n.members)
        for (var modifier : member.item().explicits()) {
          var primary =
              n.item.explicits().stream()
                  .filter(m -> kernel.attributeKey(m).equals(kernel.attributeKey(modifier)))
                  .findFirst()
                  .orElseThrow();
          var options = fields.get(primary.modifierId());
          var cell = member.tiers().get(modifier.modifierId());
          if (cell != null)
            for (var v : cell.variants())
              options.add(
                  new AttributeOption(
                      v.modifierId(),
                      v.stat(),
                      v.min(),
                      v.max(),
                      kernel.modifierTier(v.modifierId()),
                      kernel.attributeKey(modifier).toString()));
          else
            modifier
                .values()
                .forEach(
                    (stat, value) -> {
                      var band = kernel.rollBands(member.item()).get(modifier.modifierId());
                      options.add(
                          new AttributeOption(
                              modifier.modifierId(),
                              stat,
                              value,
                              band != null && band.stat().equals(stat) ? band.max() : value,
                              kernel.modifierTier(modifier.modifierId()),
                              kernel.attributeKey(modifier).toString()));
                    });
        }
      supports.put(n.id, fields);
    }
    boolean changed;
    do {
      changed = false;
      for (var n : sources)
        for (var origin : n.origins)
          for (var mapping : origin.retained().entrySet()) {
            var values = supports.get(origin.source()).get(mapping.getValue());
            if (values == null) throw new IllegalStateException("Invalid joint source projection");
            if (supports.get(n.id).get(mapping.getKey()).addAll(values)) changed = true;
          }
    } while (changed);
    var result = new LinkedHashMap<Integer, Map<String, List<AttributeOption>>>();
    supports.forEach(
        (id, fields) -> {
          var projected = new LinkedHashMap<String, List<AttributeOption>>();
          fields.forEach((field, options) -> projected.put(field, List.copyOf(options)));
          result.put(id, Map.copyOf(projected));
        });
    return result;
  }

  private String executionMode(Method method) {
    if (method.template.cycle().size() == 1
        && method.template.cycle().getFirst().currency() == WorkbenchCurrency.CHAOS)
      return "REPEATABLE";
    if (method.execution.cycles() > 1) return "REPEATABLE";
    if (method.execution.atBoundary() && method.execution.absorbed()) return "SINGLE_PASS";
    var first = method.points.get(1);
    return first != null && !first.active().isEmpty() ? "REPEATABLE" : "PENDING";
  }

  private Set<Integer> completeObservations(Method method) {
    var complete = new HashSet<>((method.owner == null ? method : method.owner).points.keySet());
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
                    retryStates(m),
                    m.done()))
        .toList();
  }

  private Set<ItemState> retryStates(Method method) {
    var result = new HashSet<>(method.execution.retryRepresentatives());
    if (method.template.boundedChaos())
      visiblePoints(method)
          .values()
          .forEach(point -> point.active().keySet().forEach(state -> result.add(state.item())));
    return Set.copyOf(result);
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
        var point = (method.owner == null ? method : method.owner).points.get(cycles);
        if (point != null) point = transport(method, point);
        if (point == null) {
          open = true;
          point = transport(method, method.execution.provisional(cycles));
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
                            var target =
                                nodes.get(
                                    new Identity(
                                        state.item(),
                                        state.quality40(),
                                        state.abstractQuality(),
                                        state.tiers()));
                            if (target == null) unknown[0] = unknown[0].add(mass);
                            else outcomes.merge(target.id, mass, CertifiedInterval::add);
                          }
                        }));
        point
            .active()
            .forEach(
                (state, mass) -> {
                  // The displayed method permits at most this horizon's Chaos uses.
                  // Do not turn its failed remainder into another horizon, which would
                  // silently report unlimited retries in the route recommendation.
                  if (method.template.boundedChaos()) {
                    outcomes.merge(-1, mass, CertifiedInterval::add);
                    return;
                  }
                  var target =
                      nodes.get(
                          new Identity(
                              state.item(),
                              state.quality40(),
                              state.abstractQuality(),
                              state.tiers()));
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

  public static CertifiedMethodRunner.Observation<
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
      Map<String, TierCell> tiers,
      boolean quality40,
      int abstractQuality,
      int depth,
      int scan,
      int pairs,
      boolean discovered,
      List<AutomaticCraftingAction> applicable,
      Set<AutomaticCraftingAction> allowed,
      List<Identity> members,
      Set<Origin> joint) {
    public NodeCursor {
      tiers = tiers == null ? Map.of() : Map.copyOf(tiers);
      members = members == null ? List.of() : List.copyOf(members);
      joint = joint == null ? Set.of() : Set.copyOf(joint);
    }

    public NodeCursor(
        ItemState item,
        Map<String, TierCell> tiers,
        boolean quality40,
        int abstractQuality,
        int depth,
        int scan,
        int pairs,
        boolean discovered,
        List<AutomaticCraftingAction> applicable,
        Set<AutomaticCraftingAction> allowed) {
      this(
          item,
          tiers,
          quality40,
          abstractQuality,
          depth,
          scan,
          pairs,
          discovered,
          applicable,
          allowed,
          List.of(),
          Set.of());
    }

    public NodeCursor(
        ItemState item,
        boolean quality40,
        int abstractQuality,
        int depth,
        int scan,
        int pairs,
        boolean discovered,
        List<AutomaticCraftingAction> applicable,
        Set<AutomaticCraftingAction> allowed) {
      this(
          item,
          Map.of(),
          quality40,
          abstractQuality,
          depth,
          scan,
          pairs,
          discovered,
          applicable,
          allowed);
    }

    public NodeCursor(
        ItemState item,
        boolean quality40,
        int depth,
        int scan,
        int pairs,
        boolean discovered,
        List<AutomaticCraftingAction> applicable,
        Set<AutomaticCraftingAction> allowed) {
      this(
          item, quality40, quality40 ? 40 : 0, depth, scan, pairs, discovered, applicable, allowed);
    }
  }

  public record MethodCursor(
      String id,
      int source,
      AutomaticMethodExecution.Checkpoint execution,
      List<Point> points,
      boolean finished,
      String owner) {
    public MethodCursor(
        String id,
        int source,
        AutomaticMethodExecution.Checkpoint execution,
        List<Point> points,
        boolean finished) {
      this(id, source, execution, points, finished, null);
    }
  }

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
                        n.tiers,
                        n.quality40,
                        n.abstractQuality,
                        n.depth,
                        n.scan,
                        n.pairs,
                        n.discovered,
                        List.copyOf(n.applicable),
                        Set.copyOf(n.allowedByPolicy),
                        List.copyOf(n.members),
                        Set.copyOf(n.origins)))
            .toList(),
        methods.values().stream()
            .map(
                m ->
                    new MethodCursor(
                        m.id,
                        m.source.id,
                        m.owner == null
                            ? m.execution.checkpoint(objective).withIdentity(null)
                            : null,
                        m.points.values().stream().map(AutomaticMethodGraph::encode).toList(),
                        m.finished,
                        m.owner == null ? null : m.owner.id))
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
    graph.classes.clear();
    graph.terminalClasses.clear();
    graph.orderedNodes.clear();
    if (saved.candidates().size() != graph.candidates.size()
        || !new HashSet<>(saved.candidates()).equals(new HashSet<>(graph.candidates)))
      throw new IllegalArgumentException("Saved candidate inventory differs from policy");
    graph.candidates = List.copyOf(saved.candidates());
    for (var row : saved.nodes()) {
      var node =
          graph.intern(
              row.item(), row.quality40(), row.abstractQuality(), row.depth(), row.tiers());
      if (node == null) throw new IllegalArgumentException("Invalid saved state budget");
      if (node.id != graph.orderedNodes.size() - 1)
        throw new IllegalArgumentException("Duplicate canonical saved state");
      for (var member : row.members()) {
        node.members.add(member);
        graph.nodes.put(member, node);
      }
      node.origins.addAll(row.joint());
      node.scan = row.scan();
      node.pairs = row.pairs();
      node.discovered = row.discovered();
      node.applicable.addAll(row.applicable());
      node.allowedByPolicy.addAll(row.allowed());
    }
    for (var row : saved.executions()) {
      var source = graph.orderedNodes.get(row.source());
      var owner =
          row.owner() == null ? null : Objects.requireNonNull(graph.methods.get(row.owner()));
      var template =
          owner == null
              ? row.execution().template()
              : new AutomaticMethodExecution.Template(
                  row.id(), owner.template.initial(), owner.template.cycle());
      var method = graph.new Method(row.id(), source, template);
      method.execution.close();
      method.owner = owner;
      method.execution =
          owner != null
              ? owner.execution
              : AutomaticMethodExecution.restore(
                  row.execution().withIdentity(saved.identity()),
                  kernel,
                  objective,
                  (item, quality) -> graph.classify(source, item, quality));
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
    methods.values().stream().filter(m -> m.owner == null).forEach(m -> m.execution.close());
    ready.clear();
  }
}
