package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.Function;

/** Exact conditional item transport retained as method operators, separate from class marginals. */
public final class ConditionalMethodLaw {
  public static final String VERSION = "conditional-method-operator-v1";
  private final BasicClassIndex index;
  private final int target;
  private final Fraction probability;
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
        index, source, Fraction.ONE, Map.copyOf(input), null, null, null, 0, null);
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
        index, target, probability, null, parent, template, classify, cycles, exit.orElse(null));
  }

  private ConditionalMethodLaw(
      BasicClassIndex index,
      int target,
      Fraction probability,
      Map<BasicCurrencyState, Fraction> root,
      ConditionalMethodLaw parent,
      Template<Integer, WorkbenchCurrency> template,
      Function<Integer, Optional<Exit>> classify,
      long cycles,
      Exit exit) {
    this.index = index;
    this.target = target;
    this.probability = probability;
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
    return probability;
  }

  public Expansion expansion() {
    return new Expansion();
  }

  public final class Expansion {
    private final ArrayDeque<ConditionalMethodLaw> stages = new ArrayDeque<>();
    private Map<BasicCurrencyState, Fraction> current;
    private Transport transport;

    private Expansion() {
      var law = ConditionalMethodLaw.this;
      while (law.parent != null) {
        stages.addFirst(law);
        law = law.parent;
      }
      current = law.root;
    }

    public boolean complete() {
      return stages.isEmpty() && transport == null;
    }

    public void step() {
      if (complete()) return;
      if (transport == null) transport = new Transport(stages.removeFirst(), current);
      if (!transport.complete) transport.step();
      if (transport.complete) {
        current = transport.result;
        transport = null;
      }
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
          if (law.index.classOf(entry.getKey()) == law.target)
            select(entry.getKey(), entry.getValue());
          return;
        }
        activeSelection = null;
      }
      if (normalization == null) {
        if (!selectedMass.equals(law.probability))
          throw new IllegalStateException(
              "Class marginal disagrees with full conditional transport");
        normalization = selected.entrySet().iterator();
      }
      if (normalization.hasNext()) {
        var entry = normalization.next();
        entry.setValue(entry.getValue().divide(law.probability));
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
