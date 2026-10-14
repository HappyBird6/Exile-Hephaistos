package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.util.*;

/** Job-local class identities. An integer identifies a proven class, never an incoming item law. */
public final class BasicClassIndex implements AutoCloseable {
  public static final String VERSION = "interned-basic-transition-classes-v1";

  public record Entry(BasicTransitionClasses.Key key, BasicCurrencyState witness) {}

  public interface Storage extends AutoCloseable {
    Integer find(BasicTransitionClasses.Key key);

    int add(BasicTransitionClasses.Key key, BasicCurrencyState witness);

    Entry entry(int id);

    int size();

    long residentEntries();

    @Override
    void close();
  }

  private static final class MemoryStorage implements Storage {
    private final Map<BasicTransitionClasses.Key, Integer> identities = new HashMap<>();
    private final List<Entry> entries = new ArrayList<>();

    public Integer find(BasicTransitionClasses.Key key) {
      return identities.get(key);
    }

    public int add(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
      int id = entries.size();
      identities.put(key, id);
      entries.add(new Entry(key, witness));
      return id;
    }

    public Entry entry(int id) {
      return entries.get(id);
    }

    public int size() {
      return entries.size();
    }

    public long residentEntries() {
      return entries.size();
    }

    public void close() {
      identities.clear();
      entries.clear();
    }
  }

  private final BasicTransitionClasses classes;
  private final Storage storage;

  public BasicClassIndex(BasicTransitionClasses classes) {
    this(classes, new MemoryStorage());
  }

  public BasicClassIndex(BasicTransitionClasses classes, Storage storage) {
    this.classes = Objects.requireNonNull(classes);
    this.storage = Objects.requireNonNull(storage);
  }

  public int intern(BasicCurrencyState actual) {
    return intern(classes.key(actual), actual);
  }

  private int intern(BasicTransitionClasses.Key key, BasicCurrencyState witness) {
    var existing = storage.find(key);
    if (existing != null) return existing;
    return storage.add(key, witness);
  }

  public int size() {
    return storage.size();
  }

  public long residentEntries() {
    return storage.residentEntries();
  }

  public void close() {
    storage.close();
  }

  public BasicTransitionClasses.ProfileMetrics profileMetrics() {
    return classes.profileMetrics();
  }

  public int profileRetainedRows() {
    return classes.profileRetainedRows();
  }

  public int classOf(BasicCurrencyState actual) {
    var id = storage.find(classes.key(actual));
    if (id == null) throw new IllegalArgumentException("Item has no registered transition class");
    return id;
  }

  public BasicTransitionClasses.Key key(int id) {
    return storage.entry(id).key();
  }

  public BasicCurrencyState witness(int id) {
    return storage.entry(id).witness();
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
