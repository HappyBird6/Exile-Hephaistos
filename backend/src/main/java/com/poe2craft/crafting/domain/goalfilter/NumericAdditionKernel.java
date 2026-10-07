package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.*;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Selection uses the existing pool, while roll outcomes retain the entire concrete item. */
public final class NumericAdditionKernel {
  public static final String MODEL = "solar-numeric-addition-v1";
  public static final String LEDGER = "uniform-integer-roll-v1";
  private final ItemCatalog catalog;
  private final AdditionTransitions transitions;

  public NumericAdditionKernel(ItemCatalog catalog) {
    this.catalog = catalog;
    transitions = new AdditionTransitions(catalog);
  }

  public static boolean supports(ItemCatalog catalog) {
    return catalog != null
        && catalog.base().id().equals(SolarAmulet.BASE_ID)
        && catalog.modifiers().values().stream().allMatch(d -> d.stats().size() == 1);
  }

  public record Result(
      boolean available, String reason, List<Outcome> outcomes, Fraction unresolved) {}

  /** An interrupted expansion returns the exact complement; it never renormalizes survivors. */
  public Result expand(ItemState item, WorkbenchCurrency action, BooleanSupplier edgeBudget) {
    var selected = transitions.transition(StateBucket.from(item), action, Set.of());
    if (!selected.available())
      return new Result(false, selected.reason(), List.of(), Fraction.ZERO);
    var outcomes = new ArrayList<Outcome>();
    var emitted = Fraction.ZERO;
    for (var candidate : selected.outcomes()) {
      var definition = catalog.find(candidate.addedModifierId()).orElseThrow();
      if (definition.stats().size() != 1)
        throw new IllegalArgumentException("Joint rolls unsupported");
      var range = definition.stats().getFirst();
      var count =
          BigInteger.valueOf(range.max())
              .subtract(BigInteger.valueOf(range.min()))
              .add(BigInteger.ONE);
      var mass =
          new Fraction(
              BigInteger.valueOf(candidate.weight()),
              BigInteger.valueOf(candidate.totalWeight()).multiply(count));
      for (long value = range.min(); ; value++) {
        if (!edgeBudget.getAsBoolean())
          return new Result(true, "BUDGET_EXHAUSTED", List.copyOf(outcomes), complement(emitted));
        var explicit = new ArrayList<>(item.explicits());
        explicit.add(new ModifierInstance(definition.id(), Map.of(range.id(), value)));
        var next =
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                candidate.state().rarity(),
                item.implicits(),
                explicit,
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality());
        outcomes.add(new Outcome(next, mass));
        emitted = emitted.add(mass);
        if (value == range.max()) break;
      }
    }
    if (!emitted.equals(Fraction.ONE)) throw new IllegalStateException("Invalid kernel mass");
    return new Result(true, "", List.copyOf(outcomes), Fraction.ZERO);
  }

  private static Fraction complement(Fraction mass) {
    return new Fraction(mass.denominator().subtract(mass.numerator()), mass.denominator());
  }
}
