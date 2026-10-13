package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalEvaluator;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.*;
import java.util.*;
import java.util.function.Function;

/**
 * Strong basic-action equivalence with separate exact-input transport. A class is not an item
 * distribution: replay always takes the actual incoming item, never an averaged representative.
 */
public final class BasicTransitionClasses {
  public static final String VERSION = "indexed-basic-action-equivalence-with-input-transport-v2";

  public record Role(String family, Object region, boolean fractured) {}

  public record Key(BasicCurrencyState context, List<Role> modifiers, List<Status> observations) {
    public Key {
      modifiers = List.copyOf(modifiers);
      observations = List.copyOf(observations);
    }
  }

  private final ItemCatalog catalog;
  private final BasicCurrencyTransitions primitive;
  private final RollPartition partition;
  private final Function<ItemState, GoalEvaluator.Evaluation> evaluate;
  private final Map<String, String> exactFamilies = new HashMap<>();
  private final Map<String, String> classFamilies = new HashMap<>();

  public BasicTransitionClasses(
      ItemCatalog catalog,
      BasicCurrencyTransitions primitive,
      RollPartition partition,
      Function<ItemState, GoalEvaluator.Evaluation> evaluate) {
    this.catalog = catalog;
    this.primitive = primitive;
    this.partition = partition;
    this.evaluate = evaluate;
    catalog
        .modifiers()
        .values()
        .forEach(
            definition -> {
              var exact = definition.affixType() + ":" + new TreeSet<>(definition.familyIds());
              exactFamilies.put(definition.id(), exact);
              classFamilies.put(
                  definition.id(),
                  partition
                      .orbits()
                      .profile(definition.id())
                      .map(profile -> definition.affixType() + ":orbit:" + profile.id())
                      .orElse(exact));
            });
  }

  public Key key(BasicCurrencyState state) {
    var item = state.item();
    var roles = new ArrayList<Role>();
    for (var modifier : item.explicits()) {
      roles.add(classRole(modifier));
    }
    roles.sort(
        Comparator.comparing(Role::family)
            .thenComparing(r -> r.region().toString())
            .thenComparing(Role::fractured));
    var context =
        new BasicCurrencyState(
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(),
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality()),
            state.provenance());
    var observation = evaluate.apply(item);
    var statuses = new ArrayList<Status>();
    statuses.add(observation.status());
    statuses.add(observation.generalStatus());
    observation
        .groups()
        .forEach(
            g -> {
              statuses.add(g.status());
              g.entries().forEach(e -> statuses.add(e.status()));
            });
    return new Key(context, roles, statuses);
  }

  /** Existing numeric laws may mix only within this exact family and observation region. */
  public Role retainedRole(ModifierInstance modifier) {
    var definition = catalog.modifiers().get(modifier.modifierId());
    if (definition == null) throw new IllegalArgumentException("Unknown modifier");
    return new Role(
        exactFamilies.get(definition.id()),
        partition.transitionRegion(definition, modifier),
        modifier.fractured());
  }

  private Role classRole(ModifierInstance modifier) {
    var definition = catalog.modifiers().get(modifier.modifierId());
    if (definition == null) throw new IllegalArgumentException("Unknown modifier");
    return new Role(
        (modifier.fractured() ? exactFamilies : classFamilies).get(definition.id()),
        partition.transitionRegion(definition, modifier),
        modifier.fractured());
  }

  private BasicCurrencyTransitions.PrimitiveCursor raw(
      BasicCurrencyState source, WorkbenchCurrency action) {
    var cursor = primitive.primitiveCursor(source, action, Set.of());
    if (!cursor.blocked() && !partition.allows(action))
      throw new IllegalArgumentException("Action outside proven partition scope");
    return cursor;
  }

  public Cursor cursor(BasicCurrencyState source, WorkbenchCurrency action) {
    return new Cursor(source, action);
  }

  public final class Kernel {
    private final Key source;
    private final WorkbenchCurrency action;
    private final Map<Key, Fraction> outcomes;
    private final Map<Key, BasicCurrencyState> witnesses;
    private final Fraction unresolved;
    private final boolean unavailable;

    private Kernel(Cursor cursor) {
      this(cursor.source, cursor.action, cursor.outcomes, cursor.witnesses, cursor.raw.status());
    }

    private Kernel(
        Key source,
        WorkbenchCurrency action,
        Map<Key, Fraction> outcomes,
        Map<Key, BasicCurrencyState> witnesses,
        BasicCurrencyTransitions.Status status) {
      this.source = source;
      this.action = action;
      this.outcomes = Map.copyOf(outcomes);
      this.witnesses = Map.copyOf(witnesses);
      unavailable = status == BasicCurrencyTransitions.Status.UNAVAILABLE;
      unresolved =
          status == BasicCurrencyTransitions.Status.UNSUPPORTED ? Fraction.ONE : Fraction.ZERO;
      var mass = outcomes.values().stream().reduce(unresolved, Fraction::add);
      if (!(unavailable ? mass.equals(Fraction.ZERO) : mass.equals(Fraction.ONE)))
        throw new IllegalStateException("Class kernel lost mass");
    }

    public Map<Key, Fraction> outcomes() {
      return outcomes;
    }

    public Map<Key, BasicCurrencyState> witnesses() {
      return witnesses;
    }

    public Fraction unresolved() {
      return unresolved;
    }

    public boolean unavailable() {
      return unavailable;
    }

    /** Exact concrete transport for one actual member; distinct entry laws remain distinct. */
    public BasicCurrencyTransitions.PrimitiveCursor replay(BasicCurrencyState actualInput) {
      if (!source.equals(key(actualInput)))
        throw new IllegalArgumentException("Different input class");
      return raw(actualInput, action);
    }
  }

  public IndexedCursor indexedCursor(BasicCurrencyState source, WorkbenchCurrency action) {
    return new IndexedCursor(source, action);
  }

  /** Aggregate equivalent generated roles before constructing complete item witnesses. */
  public final class IndexedCursor {
    private final Key source;
    private final WorkbenchCurrency action;
    private final BasicCurrencyTransitions.PrimitiveCursor raw;
    private final List<BasicCurrencyTransitions.KernelBranch> branches;
    private final Map<Key, Fraction> outcomes = new LinkedHashMap<>();
    private final Map<Key, BasicCurrencyState> witnesses = new LinkedHashMap<>();
    private final Map<Role, Addition> additions = new LinkedHashMap<>();
    private Iterator<Addition> flush;
    private int branch, candidate;
    private Long value;
    private long materializedWitnesses;

    private final class Addition {
      final int candidate;
      final long value;
      Fraction mass;

      Addition(int candidate, long value, Fraction mass) {
        this.candidate = candidate;
        this.value = value;
        this.mass = mass;
      }
    }

    IndexedCursor(BasicCurrencyState state, WorkbenchCurrency action) {
      source = key(state);
      this.action = action;
      raw = raw(state, action);
      branches = raw.blocked() ? List.of() : raw.identity().orElseThrow().branches();
    }

    public boolean complete() {
      return raw.blocked() || branch == branches.size();
    }

    public long materializedWitnesses() {
      return materializedWitnesses;
    }

    public void step() {
      if (complete()) return;
      if (action == WorkbenchCurrency.ANNULMENT) {
        accept(
            new BasicCurrencyState(branches.get(branch).remaining(), primitive.provenance()),
            Fraction.of(1, branches.size()));
        branch++;
        return;
      }
      if (flush != null) {
        if (flush.hasNext()) {
          var addition = flush.next();
          accept(raw.candidateWitness(branch, addition.candidate, addition.value), addition.mass);
        }
        if (!flush.hasNext()) {
          branch++;
          candidate = 0;
          value = null;
          additions.clear();
          flush = null;
        }
        return;
      }
      var candidates = branches.get(branch).candidates();
      var definition = candidates.get(candidate);
      var range = definition.stats().getFirst();
      if (value == null) value = range.min();
      long last = partition.end(definition, value);
      var role = classRole(new ModifierInstance(definition.id(), Map.of(range.id(), value)));
      var mass = raw.candidateBandProbability(branch, candidate, value, last);
      var previous = additions.get(role);
      if (previous == null) additions.put(role, new Addition(candidate, value, mass));
      else previous.mass = previous.mass.add(mass);
      if (last == range.max()) {
        candidate++;
        value = null;
      } else value = last + 1;
      if (candidate == candidates.size()) flush = additions.values().iterator();
    }

    private void accept(BasicCurrencyState witness, Fraction mass) {
      materializedWitnesses++;
      var target = key(witness);
      outcomes.merge(target, mass, Fraction::add);
      witnesses.putIfAbsent(target, witness);
    }

    public Kernel result() {
      if (!complete()) throw new IllegalStateException("Partial indexed class kernel");
      return new Kernel(source, action, outcomes, witnesses, raw.status());
    }
  }

  public final class Cursor {
    private final Key source;
    private final WorkbenchCurrency action;
    private final BasicCurrencyTransitions.PrimitiveCursor raw;
    private final Map<Key, Fraction> outcomes = new LinkedHashMap<>();
    private final Map<Key, BasicCurrencyState> witnesses = new LinkedHashMap<>();

    private Cursor(BasicCurrencyState state, WorkbenchCurrency action) {
      source = key(state);
      this.action = action;
      raw = raw(state, action);
    }

    public boolean complete() {
      return raw.blocked() || raw.complete();
    }

    public void step() {
      if (complete()) return;
      var result = raw.nextGrouped(partition::end).outcome();
      var target = key(result.state());
      outcomes.merge(target, result.probability(), Fraction::add);
      witnesses.putIfAbsent(target, result.state());
    }

    public Kernel result() {
      if (!complete()) throw new IllegalStateException("Partial class kernel");
      return new Kernel(this);
    }
  }
}
