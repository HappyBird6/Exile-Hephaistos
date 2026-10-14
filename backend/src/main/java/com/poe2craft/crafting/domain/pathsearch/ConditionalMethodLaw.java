package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;

/** Exact conditional item transport retained as method operators, separate from class marginals. */
public final class ConditionalMethodLaw {
  public static final String VERSION = "conditional-method-operator-v2-certified";
  private final BasicClassIndex index;
  private final int target;
  private final Fraction probability;
  private final CertifiedInterval enclosure;
  private final Map<BasicCurrencyState, Fraction> root;
  private final ConditionalMethodLaw parent;
  private final Template<Integer, WorkbenchCurrency> template;
  private final Function<Integer, Optional<Exit>> classify;
  private final long cycles;
  private final Exit exit;

  public static ConditionalMethodLaw root(
      BasicClassIndex index, Map<BasicCurrencyState, Fraction> input) {
    if (input.isEmpty() || !sum(input).equals(Fraction.ONE))
      throw new IllegalArgumentException("Normalized incoming law required");
    int source = index.classOf(input.keySet().iterator().next());
    if (input.keySet().stream().anyMatch(s -> index.classOf(s) != source))
      throw new IllegalArgumentException("Incoming law spans different classes");
    return new ConditionalMethodLaw(
        index, source, Fraction.ONE, null, Map.copyOf(input), null, null, null, 0, null);
  }

  static ConditionalMethodLaw output(
      ConditionalMethodLaw parent,
      BasicClassIndex index,
      int source,
      Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<Exit>> classify,
      long cycles,
      Optional<Exit> exit,
      int target,
      Fraction probability) {
    if (parent.index != index || parent.target != source)
      throw new IllegalArgumentException("Different incoming law class or job");
    if (cycles < 0
        || probability.equals(Fraction.ZERO)
        || probability.numerator().compareTo(probability.denominator()) > 0)
      throw new IllegalArgumentException("Positive conditional probability required");
    index.key(target);
    return new ConditionalMethodLaw(
        index,
        target,
        probability,
        null,
        null,
        parent,
        template,
        classify,
        cycles,
        exit.orElse(null));
  }

  static ConditionalMethodLaw boundedOutput(
      ConditionalMethodLaw parent,
      BasicClassIndex index,
      int source,
      Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<Exit>> classify,
      long cycles,
      Optional<Exit> exit,
      int target,
      CertifiedInterval enclosure) {
    if (parent.index != index || parent.target != source)
      throw new IllegalArgumentException("Different incoming law class or job");
    if (cycles < 0
        || enclosure.zero()
        || enclosure.upper().numerator().compareTo(enclosure.upper().denominator()) > 0)
      throw new IllegalArgumentException("Positive probability enclosure required");
    index.key(target);
    return new ConditionalMethodLaw(
        index,
        target,
        null,
        enclosure,
        null,
        parent,
        template,
        classify,
        cycles,
        exit.orElse(null));
  }

  public Optional<CertifiedInterval> enclosure() {
    return Optional.ofNullable(enclosure);
  }

  private ConditionalMethodLaw(
      BasicClassIndex index,
      int target,
      Fraction probability,
      CertifiedInterval enclosure,
      Map<BasicCurrencyState, Fraction> root,
      ConditionalMethodLaw parent,
      Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<Exit>> classify,
      long cycles,
      Exit exit) {
    this.index = index;
    this.target = target;
    this.probability = probability;
    this.enclosure = enclosure;
    this.root = root;
    this.parent = parent;
    this.template = template;
    this.classify = classify;
    this.cycles = cycles;
    this.exit = exit;
  }

  public int classId() {
    return target;
  }

  /** Probability relative to the specified parent law; this is not an alternative-policy sum. */
  public Fraction probability() {
    if (probability == null)
      throw new IllegalStateException("Use the enclosure until exact transport completes");
    return probability;
  }

  public Expansion expansion() {
    return new Expansion();
  }

  public record WeightedState(BasicCurrencyState state, Fraction mass) {}

  public record TransportCheckpoint(
      List<WeightedState> remaining,
      List<WeightedState> next,
      List<WeightedState> selected,
      Fraction absorbed,
      Fraction boundaryMass,
      Fraction nextMass,
      Fraction selectedMass,
      Fraction sourceMass,
      int phase,
      long cycles,
      boolean initializing,
      boolean preparing,
      boolean finishing,
      int inputsConsumed,
      int activeConsumed,
      int normalized,
      boolean selectingActive,
      boolean normalizing,
      BasicCurrencyState cursorSource,
      WorkbenchCurrency cursorAction,
      BasicCurrencyTransitions.PrimitiveCursor.Position position) {}

  public record ExpansionCheckpoint(
      String version,
      int stage,
      List<WeightedState> current,
      Fraction eventProbability,
      TransportCheckpoint transport) {}

  private static List<WeightedState> rows(Map<BasicCurrencyState, Fraction> law) {
    return law.entrySet().stream().map(e -> new WeightedState(e.getKey(), e.getValue())).toList();
  }

  private static Map<BasicCurrencyState, Fraction> law(List<WeightedState> rows) {
    var result = new LinkedHashMap<BasicCurrencyState, Fraction>();
    for (var row : rows)
      if (result.putIfAbsent(row.state(), row.mass()) != null)
        throw new IllegalArgumentException("Duplicate conditional state");
    return result;
  }

  public Expansion restoreExpansion(ExpansionCheckpoint saved) {
    if (!VERSION.equals(saved.version()) || saved.stage() < 0)
      throw new IllegalArgumentException("Conditional checkpoint version mismatch");
    var result = new Expansion();
    if (saved.stage() > result.stages.size())
      throw new IllegalArgumentException("Conditional stage outside operator chain");
    for (int i = 0; i < saved.stage(); i++) result.stages.removeFirst();
    result.stage = saved.stage();
    result.current = law(saved.current());
    result.eventProbability = saved.eventProbability();
    if (!sum(result.current).equals(Fraction.ONE))
      throw new IllegalArgumentException("Conditional input mass mismatch");
    if (saved.transport() != null) {
      var transport = new Transport(result.stages.removeFirst(), result.current);
      var s = saved.transport();
      transport.remaining = law(s.remaining());
      transport.next = law(s.next());
      transport.selected.putAll(law(s.selected()));
      transport.absorbed = s.absorbed();
      transport.boundaryMass = s.boundaryMass();
      transport.nextMass = s.nextMass();
      transport.selectedMass = s.selectedMass();
      transport.sourceMass = s.sourceMass();
      transport.phase = s.phase();
      transport.cycles = s.cycles();
      transport.initializing = s.initializing();
      transport.preparing = s.preparing();
      transport.finishing = s.finishing();
      transport.inputsConsumed = s.inputsConsumed();
      transport.activeConsumed = s.activeConsumed();
      transport.normalized = s.normalized();
      transport.inputs = transport.preparing ? result.current.entrySet().iterator() : null;
      if (transport.inputs != null) skip(transport.inputs, s.inputsConsumed());
      if (s.selectingActive()) {
        transport.activeSelection = transport.remaining.entrySet().iterator();
        skip(transport.activeSelection, s.activeConsumed());
      }
      if (s.normalizing()) {
        transport.normalization = transport.selected.entrySet().iterator();
        skip(transport.normalization, s.normalized());
      }
      if (s.position() != null) {
        transport.cursorSource = s.cursorSource();
        transport.cursorAction = s.cursorAction();
        transport.cursor =
            index.replay(index.classOf(s.cursorSource()), s.cursorSource(), s.cursorAction());
        transport.cursor.restorePosition(s.position());
      }
      result.transport = transport;
    }
    return result;
  }

  private static void skip(Iterator<?> rows, int count) {
    if (count < 0) throw new IllegalArgumentException("Negative conditional position");
    for (int i = 0; i < count; i++) {
      if (!rows.hasNext()) throw new IllegalArgumentException("Conditional position outside law");
      rows.next();
    }
  }

  public final class Expansion {
    private final ArrayDeque<ConditionalMethodLaw> stages = new ArrayDeque<>();
    private Map<BasicCurrencyState, Fraction> current;
    private Transport transport;
    private Fraction eventProbability;
    private int stage;

    public ExpansionCheckpoint checkpoint() {
      TransportCheckpoint saved = null;
      if (transport != null) {
        var t = transport;
        saved =
            new TransportCheckpoint(
                rows(t.remaining),
                rows(t.next),
                rows(t.selected),
                t.absorbed,
                t.boundaryMass,
                t.nextMass,
                t.selectedMass,
                t.sourceMass,
                t.phase,
                t.cycles,
                t.initializing,
                t.preparing,
                t.finishing,
                t.inputsConsumed,
                t.activeConsumed,
                t.normalized,
                t.activeSelection != null,
                t.normalization != null,
                t.cursorSource,
                t.cursorAction,
                t.cursor == null ? null : t.cursor.position());
      }
      return new ExpansionCheckpoint(VERSION, stage, rows(current), eventProbability, saved);
    }

    private Expansion() {
      var law = ConditionalMethodLaw.this;
      while (law.parent != null) {
        stages.addFirst(law);
        law = law.parent;
      }
      current = law.root;
      if (stages.isEmpty()) eventProbability = Fraction.ONE;
    }

    public long retainedStates() {
      return current.size()
          + (transport == null
              ? 0L
              : (long) transport.remaining.size()
                  + transport.next.size()
                  + transport.selected.size());
    }

    public boolean complete() {
      return stages.isEmpty() && transport == null;
    }

    public void step() {
      if (complete()) return;
      if (transport == null) transport = new Transport(stages.removeFirst(), current);
      if (!transport.complete) transport.step();
      if (transport.complete) {
        if (transport.law == ConditionalMethodLaw.this) eventProbability = transport.selectedMass;
        current = transport.result;
        transport = null;
        stage++;
      }
    }

    public Fraction probability() {
      if (!complete()) throw new IllegalStateException("Partial conditional transport");
      return eventProbability;
    }

    public Map<BasicCurrencyState, Fraction> result() {
      if (!complete()) throw new IllegalStateException("Partial conditional transport");
      return current;
    }
  }

  /** Direct full-state enumeration is used only when the conditional law is requested. */
  private static final class Transport {
    final ConditionalMethodLaw law;
    Map<BasicCurrencyState, Fraction> remaining = new LinkedHashMap<>();
    Map<BasicCurrencyState, Fraction> next = new LinkedHashMap<>();
    final Map<BasicCurrencyState, Fraction> selected = new LinkedHashMap<>();
    Fraction absorbed = Fraction.ZERO;
    Fraction boundaryMass = Fraction.ZERO, nextMass = Fraction.ZERO, selectedMass = Fraction.ZERO;
    Iterator<Map.Entry<BasicCurrencyState, Fraction>> inputs, activeSelection, normalization;
    BasicCurrencyTransitions.PrimitiveCursor cursor;
    BasicCurrencyState cursorSource;
    WorkbenchCurrency cursorAction;
    int inputsConsumed, activeConsumed, normalized;
    Fraction sourceMass;
    int phase;
    long cycles;
    boolean initializing, preparing = true, finishing, complete;
    Map<BasicCurrencyState, Fraction> result;

    Transport(ConditionalMethodLaw law, Map<BasicCurrencyState, Fraction> input) {
      this.law = law;
      initializing = !law.template.initial().isEmpty();
      inputs = input.entrySet().iterator();
    }

    void step() {
      if (preparing) {
        if (inputs.hasNext()) {
          var entry = inputs.next();
          inputsConsumed++;
          int id = law.index.classOf(entry.getKey());
          var terminal = law.classify.apply(id);
          if (terminal.isPresent()) absorb(entry.getKey(), id, entry.getValue(), terminal.get());
          else if (!entry.getValue().equals(Fraction.ZERO)) {
            remaining.put(entry.getKey(), entry.getValue());
            boundaryMass = boundaryMass.add(entry.getValue());
          }
          return;
        }
        preparing = false;
        inputs = null;
        if (remaining.isEmpty() || (!initializing && law.cycles == 0)) finish();
        return;
      }
      if (finishing) {
        finishStep();
        return;
      }
      if (cursor == null) {
        if (remaining.isEmpty()) {
          remaining = next;
          next = new LinkedHashMap<>();
          boundaryMass = nextMass;
          nextMass = Fraction.ZERO;
          phase++;
          var actions = initializing ? law.template.initial() : law.template.cycle();
          if (phase == actions.size()) {
            phase = 0;
            if (initializing) initializing = false;
            else cycles++;
          }
          if (remaining.isEmpty() || (!initializing && phase == 0 && cycles == law.cycles))
            finish();
          return;
        }
        var entry = remaining.entrySet().iterator().next();
        int id = law.index.classOf(entry.getKey());
        var actions = initializing ? law.template.initial() : law.template.cycle();
        var action = Objects.requireNonNull(actions.get(phase).apply(id));
        cursor = law.index.replay(id, entry.getKey(), action);
        cursorSource = entry.getKey();
        cursorAction = action;
        sourceMass = entry.getValue();
        remaining.remove(entry.getKey());
        if (cursor.blocked()) {
          absorb(
              entry.getKey(),
              id,
              sourceMass,
              cursor.status() == BasicCurrencyTransitions.Status.UNAVAILABLE
                  ? Exit.DEAD
                  : Exit.UNRESOLVED);
          cursor = null;
          return;
        }
      }
      var outcome = cursor.next();
      int id = law.index.classOf(outcome.state());
      var mass = sourceMass.multiply(outcome.probability());
      var terminal = law.classify.apply(id);
      if (terminal.isPresent()) absorb(outcome.state(), id, mass, terminal.get());
      else {
        next.merge(outcome.state(), mass, Fraction::add);
        nextMass = nextMass.add(mass);
      }
      if (cursor.complete()) cursor = null;
    }

    void absorb(BasicCurrencyState state, int id, Fraction mass, Exit kind) {
      if (mass.equals(Fraction.ZERO)) return;
      absorbed = absorbed.add(mass);
      if (kind == law.exit && id == law.target) select(state, mass);
    }

    void select(BasicCurrencyState state, Fraction mass) {
      selected.merge(state, mass, Fraction::add);
      selectedMass = selectedMass.add(mass);
    }

    void finish() {
      if (!absorbed.add(boundaryMass).equals(Fraction.ONE))
        throw new IllegalStateException("Conditional transport lost mass");
      finishing = true;
      if (law.exit == null) activeSelection = remaining.entrySet().iterator();
    }

    void finishStep() {
      if (activeSelection != null) {
        if (activeSelection.hasNext()) {
          var entry = activeSelection.next();
          activeConsumed++;
          if (law.index.classOf(entry.getKey()) == law.target)
            select(entry.getKey(), entry.getValue());
          return;
        }
        activeSelection = null;
      }
      if (normalization == null) {
        if (selectedMass.equals(Fraction.ZERO)
            || (law.probability != null
                ? !selectedMass.equals(law.probability)
                : !law.enclosure.contains(selectedMass)))
          throw new IllegalStateException(
              "Class marginal disagrees with full conditional transport");
        normalization = selected.entrySet().iterator();
      }
      if (normalization.hasNext()) {
        var entry = normalization.next();
        normalized++;
        entry.setValue(entry.getValue().divide(selectedMass));
        return;
      }
      result = Collections.unmodifiableMap(selected);
      complete = true;
    }
  }

  private static Fraction sum(Map<?, Fraction> values) {
    return values.values().stream().reduce(Fraction.ZERO, Fraction::add);
  }
}
