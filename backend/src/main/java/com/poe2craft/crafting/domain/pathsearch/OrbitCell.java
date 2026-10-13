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
  private final Map<OrbitCell, Fraction> components;

  public OrbitCell(Profile profile, Map<Integer, Integer> counts) {
    components = Map.of();
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

  private OrbitCell(Map<OrbitCell, Fraction> components) {
    var first = components.keySet().iterator().next();
    this.profile = first.profile;
    this.counts = first.counts;
    this.assignments = first.assignments;
    this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    this.cardinality =
        components.keySet().stream()
            .map(OrbitCell::cardinality)
            .reduce(BigInteger.ZERO, BigInteger::add);
  }

  /** Components have disjoint category-count support and equal family occupancy. */
  public static OrbitCell mix(Map<OrbitCell, Fraction> distribution) {
    if (distribution.isEmpty()) throw new IllegalArgumentException("Empty orbit mixture");
    var first = distribution.keySet().iterator().next();
    var flat = new TreeMap<OrbitCell, Fraction>(Comparator.comparing(c -> c.counts.toString()));
    var total = Fraction.ZERO;
    for (var entry : distribution.entrySet()) {
      var cell = entry.getKey();
      if (!cell.profile.equals(first.profile) || cell.occupancy() != first.occupancy())
        throw new IllegalArgumentException("Orbit mixture crosses a family or slot boundary");
      if (entry.getValue().numerator().signum() <= 0)
        throw new IllegalArgumentException("Positive orbit mixture weight required");
      total = total.add(entry.getValue());
      cell.basis()
          .forEach(
              (atom, weight) -> flat.merge(atom, weight.multiply(entry.getValue()), Fraction::add));
    }
    if (!total.equals(Fraction.ONE)) throw new IllegalArgumentException("Orbit mixture lost mass");
    return flat.size() == 1 ? flat.firstKey() : new OrbitCell(flat);
  }

  public boolean mixed() {
    return !components.isEmpty();
  }

  public int occupancy() {
    return counts.values().stream().mapToInt(Integer::intValue).sum();
  }

  public Map<OrbitCell, Fraction> basis() {
    return components.isEmpty() ? Map.of(this, Fraction.ONE) : components;
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
    if (mixed()) throw new IllegalStateException("A mixture has no single category occupancy");
    return counts;
  }

  public List<Assignment> assignments() {
    if (mixed()) throw new IllegalStateException("Expand weighted orbit components first");
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
    if (mixed()) {
      for (var entry : components.entrySet()) {
        if (offset.compareTo(entry.getKey().cardinality()) >= 0) {
          offset = offset.subtract(entry.getKey().cardinality());
          continue;
        }
        var result = entry.getKey().concrete(offset);
        return new Concrete(result.modifiers(), result.probability().multiply(entry.getValue()));
      }
      throw new IllegalStateException("Missing orbit mixture component");
    }
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
            && counts.equals(cell.counts)
            && components.equals(cell.components);
  }

  @Override
  public int hashCode() {
    return components.isEmpty()
        ? Objects.hash(profile.id(), counts)
        : Objects.hash(profile.id(), components);
  }

  @Override
  public String toString() {
    return components.isEmpty()
        ? "OrbitCell[profile=" + profile.id() + ", counts=" + counts + "]"
        : "OrbitCell[profile=" + profile.id() + ", components=" + components + "]";
  }
}
