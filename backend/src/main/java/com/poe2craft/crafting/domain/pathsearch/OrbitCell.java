package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/** Joint, without-replacement law over exchangeable goal-irrelevant families. */
public final class OrbitCell {
  public record Family(Set<String> ids, Map<Integer, TierCell> categories) {
    public Family {
      ids = Set.copyOf(ids);
      categories = Map.copyOf(categories);
    }
  }

  public record Profile(
      String id,
      ModifierDefinition.AffixType affix,
      List<Family> families,
      Map<Integer, Long> weights) {
    public Profile {
      families = List.copyOf(families);
      weights = Map.copyOf(weights);
    }
  }

  public record Assignment(List<TierCell> families) {
    public Assignment {
      families = List.copyOf(families);
    }

    public BigInteger cardinality() {
      return families.stream()
          .map(TierCell::cardinality)
          .reduce(BigInteger.ONE, BigInteger::multiply);
    }
  }

  public record Concrete(List<ModifierInstance> modifiers, Fraction probability) {}

  private final Profile profile;
  private final Map<Integer, Integer> counts;
  private final List<Assignment> assignments;
  private final BigInteger cardinality;

  public OrbitCell(Profile profile, Map<Integer, Integer> counts) {
    this.profile = Objects.requireNonNull(profile);
    this.counts = Collections.unmodifiableMap(new TreeMap<>(counts));
    int occupied = counts.values().stream().mapToInt(Integer::intValue).sum();
    if (occupied < 1
        || occupied > profile.families().size()
        || counts.values().stream().anyMatch(n -> n < 1)
        || !profile.weights().keySet().containsAll(counts.keySet()))
      throw new IllegalArgumentException("Invalid family occupancy");
    var options = new ArrayList<Assignment>();
    enumerate(0, new TreeMap<>(counts), new ArrayList<>(), options);
    if (options.isEmpty()) throw new IllegalArgumentException("Empty orbit");
    assignments = List.copyOf(options);
    cardinality =
        assignments.stream().map(Assignment::cardinality).reduce(BigInteger.ZERO, BigInteger::add);
  }

  private void enumerate(
      int family,
      Map<Integer, Integer> remaining,
      List<TierCell> selected,
      List<Assignment> result) {
    if (remaining.values().stream().allMatch(n -> n == 0)) {
      result.add(new Assignment(selected));
      return;
    }
    if (family == profile.families().size()) return;
    enumerate(family + 1, remaining, selected, result);
    for (int category : counts.keySet()) {
      int left = remaining.get(category);
      if (left == 0) continue;
      var cell = profile.families().get(family).categories().get(category);
      if (cell == null) throw new IllegalArgumentException("Asymmetric category support");
      remaining.put(category, left - 1);
      selected.add(cell);
      enumerate(family + 1, remaining, selected, result);
      selected.removeLast();
      remaining.put(category, left);
    }
  }

  public Profile profile() {
    return profile;
  }

  public Map<Integer, Integer> counts() {
    return counts;
  }

  public List<Assignment> assignments() {
    return assignments;
  }

  public BigInteger cardinality() {
    return cardinality;
  }

  public List<ModifierInstance> representatives() {
    return assignments.getFirst().families().stream().map(c -> c.representative(false)).toList();
  }

  public Concrete concrete(BigInteger offset) {
    if (offset.signum() < 0 || offset.compareTo(cardinality) >= 0)
      throw new IllegalArgumentException("Orbit offset out of range");
    for (var assignment : assignments) {
      if (offset.compareTo(assignment.cardinality()) >= 0) {
        offset = offset.subtract(assignment.cardinality());
        continue;
      }
      var modifiers = new ArrayList<ModifierInstance>();
      var probability = Fraction.of(1, assignments.size());
      for (var cell : assignment.families()) {
        var divided = offset.divideAndRemainder(cell.cardinality());
        var concrete = cell.concrete(divided[1], false);
        modifiers.add(concrete.modifier());
        probability = probability.multiply(concrete.probability());
        offset = divided[0];
      }
      return new Concrete(List.copyOf(modifiers), probability);
    }
    throw new IllegalStateException("Missing orbit assignment");
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof OrbitCell cell
            && profile.equals(cell.profile)
            && counts.equals(cell.counts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profile.id(), counts);
  }

  @Override
  public String toString() {
    return "OrbitCell[profile=" + profile.id() + ", counts=" + counts + "]";
  }
}
