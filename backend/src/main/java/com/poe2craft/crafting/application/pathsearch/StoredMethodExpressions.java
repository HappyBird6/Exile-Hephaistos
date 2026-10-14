package com.poe2craft.crafting.application.pathsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Function;

/** Compact, shared first-hit programs. Registration and requests perform no transition work. */
public final class StoredMethodExpressions {
  public static final String VERSION = "stored-first-hit-expression-v1";

  public record Node(String version, String kind, String value, List<Long> children, int cycles) {
    public Node {
      children = List.copyOf(children);
    }
  }

  private final MethodGraphStore store;
  private final ObjectMapper json = new ObjectMapper();
  private final LinkedHashMap<Long, Node> decoded = new LinkedHashMap<>(16, 0.75f, true);
  private final LinkedHashMap<String, Long> interned = new LinkedHashMap<>(16, 0.75f, true);
  private static final int MAX_CACHE_CONTENT_CHARS = 8192;
  private final int cacheLimit;

  public StoredMethodExpressions(MethodGraphStore store, int cacheLimit) {
    this.store = Objects.requireNonNull(store);
    if (cacheLimit < 0 || cacheLimit > 256)
      throw new IllegalArgumentException("Invalid node cache");
    this.cacheLimit = cacheLimit;
  }

  /**
   * Policy content must include the complete checkpoint/classifier semantics, not a display label.
   */
  public MethodGraphStore.Method define(
      String id,
      long source,
      List<WorkbenchCurrency> initial,
      List<WorkbenchCurrency> cycle,
      String canonicalPolicy) {
    if (cycle.isEmpty() || canonicalPolicy == null || canonicalPolicy.isBlank())
      throw new IllegalArgumentException("Cycle and complete policy required");
    long policy = intern("POLICY", canonicalPolicy, List.of(), 0);
    long before = sequence(initial);
    long repeat = sequence(cycle);
    long program = intern("FIRST_HIT_PROGRAM", "", List.of(policy, before, repeat), 0);
    return new MethodGraphStore.Method(
        id, source, encode(new Node(VERSION, "METHOD", "", List.of(program), 0)));
  }

  private long sequence(List<WorkbenchCurrency> actions) {
    var children = new ArrayList<Long>();
    for (var action : actions) children.add(intern("ACTION", action.name(), List.of(), 0));
    return intern("SEQUENCE", "", children, 0);
  }

  public long request(
      long revision, String method, int cycles, CertifiedInterval.Precision precision) {
    if (!List.of(100, 300, 500).contains(cycles))
      throw new IllegalArgumentException("Unknown horizon");
    long program = program(method);
    long expression = intern("OBSERVATION", precisionKey(precision), List.of(program), cycles);
    var old = store.observation(method, cycles);
    if (old.isPresent() && !Objects.equals(old.get().operator(), expression))
      throw new IllegalStateException("Observation precision or expression changed");
    return store.commit(
        revision,
        new MethodGraphStore.Batch(
            List.of(),
            List.of(),
            List.of(
                new MethodGraphStore.Observation(
                    method, cycles, MethodGraphStore.Status.REQUESTED, expression, null)),
            List.of()));
  }

  public int cachedNodes() {
    return decoded.size();
  }

  private long program(String method) {
    var definition =
        store.method(method).orElseThrow(() -> new IllegalArgumentException("Unknown method"));
    var node = decode(definition.program());
    require(node, "METHOD", 1);
    return node.children().getFirst();
  }

  private static String precisionKey(CertifiedInterval.Precision precision) {
    return precision.fractionalBits() + ":" + precision.percentDecimals();
  }

  /** The caller supplies full calculation states and the matching versioned primitive kernel. */
  public <S> Evaluation<S> evaluate(
      String method,
      int cycles,
      S root,
      Transition<S, WorkbenchCurrency> transition,
      Function<String, Function<S, Optional<Exit>>> policies,
      CertifiedInterval.Precision precision) {
    var requested =
        store
            .observation(method, cycles)
            .orElseThrow(() -> new IllegalStateException("Observation not requested"));
    if (requested.operator() == null)
      throw new IllegalStateException("Missing observation expression");
    var observation = node(requested.operator());
    require(observation, "OBSERVATION", 1);
    if (observation.cycles() != cycles
        || observation.children().getFirst() != program(method)
        || !observation.value().equals(precisionKey(precision)))
      throw new IllegalStateException("Observation expression mismatch");
    var program = node(observation.children().getFirst());
    require(program, "FIRST_HIT_PROGRAM", 3);
    var policy = node(program.children().get(0));
    require(policy, "POLICY", 0);
    var classifier = Objects.requireNonNull(policies.apply(policy.value()));
    var initial = actions(program.children().get(1));
    var cycle = actions(program.children().get(2));
    var template =
        new Template<S, WorkbenchCurrency>(
            VERSION + ":" + method,
            initial.stream().<Function<S, WorkbenchCurrency>>map(a -> s -> a).toList(),
            cycle.stream().<Function<S, WorkbenchCurrency>>map(a -> s -> a).toList());
    return new Evaluation<>(
        new CertifiedMethodRunner<>(root, template, transition, classifier, precision), cycles);
  }

  private List<WorkbenchCurrency> actions(long reference) {
    var sequence = node(reference);
    require(sequence, "SEQUENCE", sequence.children().size());
    return sequence.children().stream()
        .map(
            id -> {
              var action = node(id);
              require(action, "ACTION", 0);
              return WorkbenchCurrency.valueOf(action.value());
            })
        .toList();
  }

  private static void require(Node node, String kind, int children) {
    if (!VERSION.equals(node.version())
        || !kind.equals(node.kind())
        || node.children().size() != children
        || (!kind.equals("OBSERVATION") && node.cycles() != 0))
      throw new IllegalStateException("Incompatible method expression");
  }

  private long intern(String kind, String value, List<Long> children, int cycles) {
    String content = encode(new Node(VERSION, kind, value, children, cycles));
    var known = interned.get(content);
    if (known != null) return known;
    try {
      String digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(content.getBytes(StandardCharsets.UTF_8)));
      long id = store.internOperator(digest, content);
      if (cacheLimit > 0 && content.length() <= MAX_CACHE_CONTENT_CHARS) {
        interned.put(content, id);
        if (interned.size() > cacheLimit) interned.pollFirstEntry();
      }
      return id;
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private String encode(Node node) {
    try {
      return json.writeValueAsString(node);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Invalid expression", error);
    }
  }

  private Node decode(String content) {
    try {
      return json.readValue(content, Node.class);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Invalid stored expression", error);
    }
  }

  private Node node(long id) {
    var found = decoded.get(id);
    if (found != null) return found;
    String content = store.operator(id);
    var node = decode(content);
    if (cacheLimit > 0 && content.length() <= MAX_CACHE_CONTENT_CHARS) {
      decoded.put(id, node);
      if (decoded.size() > cacheLimit) decoded.pollFirstEntry();
    }
    return node;
  }

  public static final class Evaluation<S> {
    private final CertifiedMethodRunner<S, WorkbenchCurrency> runner;
    private final int cycles;

    private Evaluation(CertifiedMethodRunner<S, WorkbenchCurrency> runner, int cycles) {
      this.runner = runner;
      this.cycles = cycles;
    }

    public boolean complete() {
      return runner.atBoundary() && (runner.absorbed() || runner.cycles() == cycles);
    }

    public void step() {
      if (complete()) return;
      if (runner.atBoundary()) runner.beginCycle();
      else runner.step();
    }

    public long retainedStates() {
      return runner.retainedStates();
    }

    public CertifiedMethodRunner.Observation<S, WorkbenchCurrency> result() {
      if (!complete()) throw new IllegalStateException("Observation incomplete");
      var point = runner.observation();
      return point.cycles() == cycles ? point : point.atLaterAbsorbedBoundary(cycles);
    }
  }
}
