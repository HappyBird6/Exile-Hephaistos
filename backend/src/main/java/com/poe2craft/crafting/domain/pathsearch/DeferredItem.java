package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * A disjoint conditional cell with weighted tiers, independent single-stat rolls and joint
 * without-replacement family assignments. Incoming method laws remain separate.
 */
public record DeferredItem(
    BasicCurrencyState representative,
    Map<String, Band> rolls,
    Map<String, TierCell> tiers,
    Map<String, OrbitCell> orbits) {
  public static final String VERSION = "deferred-joint-family-category-tier-roll-v4";

  public DeferredItem(
      BasicCurrencyState representative, Map<String, Band> rolls, Map<String, TierCell> tiers) {
    this(representative, rolls, tiers, Map.of());
  }

  public DeferredItem(BasicCurrencyState representative, Map<String, Band> rolls) {
    this(representative, rolls, Map.of());
  }

  public record Band(String stat, long min, long max) {
    public Band {
      if (stat == null || stat.isBlank() || min >= max)
        throw new IllegalArgumentException("Non-singleton roll band required");
    }

    public BigInteger size() {
      return BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
    }
  }

  public DeferredItem(BasicCurrencyState state) {
    this(state, Map.of());
  }

  public DeferredItem {
    Objects.requireNonNull(representative);
    rolls = Map.copyOf(rolls);
    tiers = Map.copyOf(tiers);
    orbits = Map.copyOf(orbits);
    var orbitIds = new HashSet<String>();
    for (var entry : orbits.entrySet()) {
      if (!entry.getKey().equals(entry.getValue().profile().id()))
        throw new IllegalArgumentException("Wrong orbit identity");
      for (var modifier : entry.getValue().representatives()) {
        if (!representative.item().explicits().contains(modifier)
            || !orbitIds.add(modifier.modifierId())
            || rolls.containsKey(modifier.modifierId())
            || tiers.containsKey(modifier.modifierId()))
          throw new IllegalArgumentException("Orbit law must match disjoint representatives");
      }
    }
    for (var entry : tiers.entrySet()) {
      var modifier =
          representative.item().explicits().stream()
              .filter(m -> m.modifierId().equals(entry.getKey()))
              .findFirst()
              .orElseThrow();
      if (rolls.containsKey(entry.getKey())
          || !modifier.equals(entry.getValue().representative(modifier.fractured())))
        throw new IllegalArgumentException("Tier law must match its representative");
    }
    for (var entry : rolls.entrySet()) {
      var modifier =
          representative.item().explicits().stream()
              .filter(m -> m.modifierId().equals(entry.getKey()))
              .findFirst()
              .orElseThrow();
      if (modifier.values().size() != 1
          || !Objects.equals(
              modifier.values().get(entry.getValue().stat()), entry.getValue().min()))
        throw new IllegalArgumentException("Roll band must match its representative");
    }
  }

  public ItemState item() {
    return representative.item();
  }

  public BasicCurrencyState.Provenance provenance() {
    return representative.provenance();
  }

  public BigInteger cardinality() {
    return rolls.values().stream()
        .map(Band::size)
        .reduce(BigInteger.ONE, BigInteger::multiply)
        .multiply(
            tiers.values().stream()
                .map(TierCell::cardinality)
                .reduce(BigInteger.ONE, BigInteger::multiply))
        .multiply(
            orbits.values().stream()
                .map(OrbitCell::cardinality)
                .reduce(BigInteger.ONE, BigInteger::multiply));
  }

  /** Random access into a cell permits revision-pinned pages without materializing its product. */
  public BasicCurrencyState concrete(BigInteger offset) {
    if (offset.signum() < 0 || offset.compareTo(cardinality()) >= 0)
      throw new IllegalArgumentException("Cell offset out of range");
    var values = new HashMap<String, Long>();
    var replacements = new HashMap<String, ModifierInstance>();
    var orbitIds = new HashSet<String>();
    var orbitModifiers = new ArrayList<ModifierInstance>();
    for (var cell : new TreeMap<>(orbits).values()) {
      cell.representatives().forEach(m -> orbitIds.add(m.modifierId()));
      var divided = offset.divideAndRemainder(cell.cardinality());
      orbitModifiers.addAll(cell.concrete(divided[1]).modifiers());
      offset = divided[0];
    }
    for (var entry : new TreeMap<>(tiers).entrySet()) {
      var divided = offset.divideAndRemainder(entry.getValue().cardinality());
      boolean fractured =
          item().explicits().stream()
              .filter(m -> m.modifierId().equals(entry.getKey()))
              .findFirst()
              .orElseThrow()
              .fractured();
      replacements.put(entry.getKey(), entry.getValue().concrete(divided[1], fractured).modifier());
      offset = divided[0];
    }
    for (var entry : new TreeMap<>(rolls).entrySet()) {
      var divided = offset.divideAndRemainder(entry.getValue().size());
      values.put(
          entry.getKey(),
          BigInteger.valueOf(entry.getValue().min()).add(divided[1]).longValueExact());
      offset = divided[0];
    }
    var explicit =
        item().explicits().stream()
            .filter(m -> !orbitIds.contains(m.modifierId()))
            .map(
                m ->
                    replacements.containsKey(m.modifierId())
                        ? replacements.get(m.modifierId())
                        : rolls.containsKey(m.modifierId())
                            ? new ModifierInstance(
                                m.modifierId(),
                                Map.of(
                                    rolls.get(m.modifierId()).stat(), values.get(m.modifierId())),
                                m.fractured())
                            : m)
            .toList();
    var fullExplicit = new ArrayList<>(explicit);
    fullExplicit.addAll(orbitModifiers);
    return new BasicCurrencyState(
        new ItemState(
            item().snapshotId(),
            item().baseItemId(),
            item().itemLevel(),
            item().rarity(),
            item().implicits(),
            fullExplicit,
            item().conditions(),
            item().augmentSockets(),
            item().catalystQuality()),
        provenance());
  }

  public String canonicalKey() {
    if (rolls.isEmpty() && tiers.isEmpty() && orbits.isEmpty())
      return representative.canonicalKey();
    try {
      var text =
          "deferred-roll-cell-v1:"
              + representative.canonicalKey()
              + ":"
              + new TreeMap<>(rolls)
              + (tiers.isEmpty() ? "" : ":tiers-v1:" + new TreeMap<>(tiers))
              + (orbits.isEmpty() ? "" : ":orbits-v1:" + new TreeMap<>(orbits));
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction
      concreteProbability(BigInteger offset) {
    if (offset.signum() < 0 || offset.compareTo(cardinality()) >= 0)
      throw new IllegalArgumentException("Cell offset out of range");
    var probability =
        com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction.ONE;
    for (var cell : new TreeMap<>(orbits).values()) {
      var divided = offset.divideAndRemainder(cell.cardinality());
      probability = probability.multiply(cell.concrete(divided[1]).probability());
      offset = divided[0];
    }
    for (var cell : new TreeMap<>(tiers).values()) {
      var divided = offset.divideAndRemainder(cell.cardinality());
      probability = probability.multiply(cell.concrete(divided[1], false).probability());
      offset = divided[0];
    }
    var rollCount =
        rolls.values().stream().map(Band::size).reduce(BigInteger.ONE, BigInteger::multiply);
    return new com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction(
        probability.numerator(), probability.denominator().multiply(rollCount));
  }
}
