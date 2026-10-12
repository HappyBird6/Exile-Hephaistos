package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.ModifierInstance;
import java.math.BigInteger;
import java.util.*;

/** Disjoint modifier-tier/roll intervals with their exact conditional weights. */
public record TierCell(List<Variant> variants) {
  public record Variant(String modifierId, String stat, long min, long max, Fraction probability) {
    public Variant {
      if (modifierId == null || stat == null || min > max || probability.numerator().signum() <= 0)
        throw new IllegalArgumentException("Positive, nonempty tier interval required");
    }

    public BigInteger size() {
      return BigInteger.valueOf(max).subtract(BigInteger.valueOf(min)).add(BigInteger.ONE);
    }

    ModifierInstance item(long value, boolean fractured) {
      return new ModifierInstance(modifierId, Map.of(stat, value), fractured);
    }
  }

  public TierCell {
    variants =
        variants.stream()
            .sorted(Comparator.comparing(Variant::modifierId).thenComparingLong(Variant::min))
            .toList();
    if (variants.isEmpty()) throw new IllegalArgumentException("Empty tier cell");
    var total = Fraction.ZERO;
    Variant previous = null;
    for (var v : variants) {
      if (previous != null
          && previous.modifierId().equals(v.modifierId())
          && (!previous.stat().equals(v.stat()) || previous.max() >= v.min()))
        throw new IllegalArgumentException("Overlapping tier intervals");
      total = total.add(v.probability());
      previous = v;
    }
    if (!total.equals(Fraction.ONE))
      throw new IllegalArgumentException("Tier law must conserve mass");
  }

  public ModifierInstance representative(boolean fractured) {
    var first = variants.getFirst();
    return first.item(first.min(), fractured);
  }

  public BigInteger cardinality() {
    return variants.stream().map(Variant::size).reduce(BigInteger.ZERO, BigInteger::add);
  }

  public record Concrete(ModifierInstance modifier, Fraction probability) {}

  public Concrete concrete(BigInteger offset, boolean fractured) {
    if (offset.signum() < 0) throw new IllegalArgumentException("Negative tier offset");
    for (var variant : variants) {
      if (offset.compareTo(variant.size()) < 0) {
        var value = BigInteger.valueOf(variant.min()).add(offset).longValueExact();
        var p = variant.probability();
        return new Concrete(
            variant.item(value, fractured),
            new Fraction(p.numerator(), p.denominator().multiply(variant.size())));
      }
      offset = offset.subtract(variant.size());
    }
    throw new IllegalArgumentException("Tier offset out of range");
  }
}
