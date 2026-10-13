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
  public static final String VERSION = "basic-action-equivalence-with-input-transport-v1";

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

  public BasicTransitionClasses(
      ItemCatalog catalog,
      BasicCurrencyTransitions primitive,
      RollPartition partition,
      Function<ItemState, GoalEvaluator.Evaluation> evaluate) {
    this.catalog = catalog;
    this.primitive = primitive;
    this.partition = partition;
    this.evaluate = evaluate;
  }

  public Key key(BasicCurrencyState state) {
    var item = state.item();
    var roles = new ArrayList<Role>();
    for (var modifier : item.explicits()) {
      var definition = catalog.modifiers().get(modifier.modifierId());
      if (definition == null) throw new IllegalArgumentException("Unknown modifier");
      var family = definition.affixType() + ":" + new TreeSet<>(definition.familyIds());
      if (!modifier.fractured()) {
        var profile = partition.orbits().profile(modifier.modifierId());
        if (profile.isPresent()) family = definition.affixType() + ":orbit:" + profile.get().id();
      }
      roles.add(
          new Role(family, partition.transitionRegion(definition, modifier), modifier.fractured()));
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
      source = cursor.source;
      action = cursor.action;
      outcomes = Map.copyOf(cursor.outcomes);
      witnesses = Map.copyOf(cursor.witnesses);
      unavailable = cursor.raw.status() == BasicCurrencyTransitions.Status.UNAVAILABLE;
      unresolved =
          cursor.raw.status() == BasicCurrencyTransitions.Status.UNSUPPORTED
              ? Fraction.ONE
              : Fraction.ZERO;
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
