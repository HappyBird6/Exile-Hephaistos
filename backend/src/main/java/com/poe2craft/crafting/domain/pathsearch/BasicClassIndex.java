package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;

/** Job-local class identities. An integer identifies a proven class, never an incoming item law. */
public final class BasicClassIndex {
  public static final String VERSION = "interned-basic-transition-classes-v1";

  private record Entry(BasicTransitionClasses.Key key, BasicCurrencyState witness) {}

  private final BasicTransitionClasses classes;
  private final Map<BasicTransitionClasses.Key, Integer> identities = new HashMap<>();
  private final List<Entry> entries = new ArrayList<>();

  public BasicClassIndex(BasicTransitionClasses classes) {
    this.classes = Objects.requireNonNull(classes);
  }

  public int intern(BasicCurrencyState actual) {
    return intern(classes.key(actual), actual);
  }

  private int intern(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
    var existing = identities.get(key);
    if (existing != null) return existing;
    int id = entries.size();
    identities.put(key, id);
    entries.add(new Entry(key, witness));
    return id;
  }

  public int size() {
    return entries.size();
  }

  public int classOf(BasicCurrencyState actual) {
    var id = identities.get(classes.key(actual));
    if (id == null) throw new IllegalArgumentException("Item has no registered transition class");
    return id;
  }

  public BasicTransitionClasses.Key key(int id) {
    return entries.get(id).key();
  }

  public BasicCurrencyState witness(int id) {
    return entries.get(id).witness();
  }

  /** Validate full equality of the class key; hash equality alone never admits an input. */
  public BasicCurrencyTransitions.PrimitiveCursor replay(
      int source, BasicCurrencyState actual, WorkbenchCurrency action) {
    return classes.replay(key(source), actual, action);
  }

  public Cursor cursor(int source, WorkbenchCurrency action) {
    return new Cursor(source, action);
  }

  public final class Cursor {
    private BasicTransitionClasses.IndexedCursor projection;
    private final List<WorkbenchSimulator.Assumption> assumptions;
    private BasicTransitionClasses.Kernel projected;
    private Iterator<Map.Entry<BasicTransitionClasses.Key, Fraction>> pending;
    private final List<ExactMethodRunner.Outcome<Integer>> outcomes = new ArrayList<>();
    private ExactMethodRunner.Kernel<Integer> result;

    private Cursor(int source, WorkbenchCurrency action) {
      projection = classes.indexedCursor(witness(source), action);
      assumptions = projection.assumptions();
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      return assumptions;
    }

    public boolean complete() {
      return result != null;
    }

    /** Each step advances one candidate band/witness, or interns one completed outcome. */
    public void step() {
      if (complete()) return;
      if (projected == null) {
        if (!projection.complete()) {
          projection.step();
          return;
        }
        projected = projection.result();
        projection = null;
        pending = projected.outcomes().entrySet().iterator();
      }
      if (pending.hasNext()) {
        var entry = pending.next();
        int target = intern(entry.getKey(), projected.witnesses().get(entry.getKey()));
        outcomes.add(new ExactMethodRunner.Outcome<>(target, entry.getValue()));
        return;
      }
      result =
          new ExactMethodRunner.Kernel<>(outcomes, projected.unresolved(), projected.unavailable());
      projected = null;
      pending = null;
      outcomes.clear();
    }

    public ExactMethodRunner.Kernel<Integer> result() {
      if (!complete()) throw new IllegalStateException("Partial indexed kernel");
      return result;
    }
  }
}
