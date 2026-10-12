package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.ItemState;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * Exact finite oracle for externally proven joint outcomes, not a production game roll model. Full
 * ItemState keys retain numeric rolls, family identities, rarity, layer and quality. Joint tuples
 * are indivisible: no product-of-marginals or occupied/qualified merging.
 */
public final class ExactNumericDistribution {
  private ExactNumericDistribution() {}

  public static final class Fraction {
    private final BigInteger numerator;
    private final BigInteger denominator;
    public static final Fraction ZERO = of(0, 1);
    public static final Fraction ONE = of(1, 1);

    @com.fasterxml.jackson.annotation.JsonCreator
    public Fraction(
        @com.fasterxml.jackson.annotation.JsonProperty("numerator") BigInteger numerator,
        @com.fasterxml.jackson.annotation.JsonProperty("denominator") BigInteger denominator) {
      this(numerator, denominator, false);
    }

    // Only arithmetic below may assert reducedness, from already canonical operands.
    private Fraction(BigInteger numerator, BigInteger denominator, boolean reduced) {
      if (denominator.signum() <= 0 || numerator.signum() < 0)
        throw new IllegalArgumentException("Invalid probability fraction");
      var divisor = reduced ? BigInteger.ONE : numerator.gcd(denominator);
      this.numerator = numerator.divide(divisor);
      this.denominator = denominator.divide(divisor);
    }

    @com.fasterxml.jackson.annotation.JsonProperty
    public BigInteger numerator() {
      return numerator;
    }

    @com.fasterxml.jackson.annotation.JsonProperty
    public BigInteger denominator() {
      return denominator;
    }

    @Override
    public boolean equals(Object other) {
      return this == other
          || other instanceof Fraction f
              && numerator.equals(f.numerator)
              && denominator.equals(f.denominator);
    }

    @Override
    public int hashCode() {
      return 31 * numerator.hashCode() + denominator.hashCode();
    }

    @Override
    public String toString() {
      return "Fraction[numerator=" + numerator + ", denominator=" + denominator + "]";
    }

    public static Fraction of(long numerator, long denominator) {
      return new Fraction(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public Fraction add(Fraction other) {
      Objects.requireNonNull(other);
      if (numerator.signum() == 0) return other;
      if (other.numerator.signum() == 0) return this;
      if (denominator.equals(other.denominator))
        return new Fraction(numerator.add(other.numerator), denominator);
      // Markov exit laws often share a large denominator. Form their LCM instead
      // of squaring that denominator before reducing every accumulated outcome.
      var common = denominator.gcd(other.denominator);
      var leftScale = other.denominator.divide(common);
      var rightScale = denominator.divide(common);
      var sum = numerator.multiply(leftScale).add(other.numerator.multiply(rightScale));
      // Coprime operands prove that only a factor of the original common divisor remains.
      var reduction = sum.gcd(common);
      return new Fraction(
          sum.divide(reduction), rightScale.multiply(other.denominator.divide(reduction)), true);
    }

    public Fraction multiply(Fraction other) {
      Objects.requireNonNull(other);
      if (numerator.signum() == 0 || other.numerator.signum() == 0) return ZERO;
      if (equals(ONE)) return other;
      if (other.equals(ONE)) return this;
      var left = numerator.gcd(other.denominator);
      var right = other.numerator.gcd(denominator);
      return new Fraction(
          numerator.divide(left).multiply(other.numerator.divide(right)),
          denominator.divide(right).multiply(other.denominator.divide(left)),
          true);
    }

    public Fraction pow(int exponent) {
      if (exponent < 0) throw new IllegalArgumentException("Negative exponent");
      return new Fraction(numerator.pow(exponent), denominator.pow(exponent), true);
    }

    public Fraction complement() {
      return new Fraction(denominator.subtract(numerator), denominator, true);
    }

    public Fraction divide(Fraction other) {
      if (other.numerator.signum() == 0) throw new IllegalArgumentException("Division by zero");
      return multiply(new Fraction(other.denominator, other.numerator, true));
    }
  }

  public record Outcome(ItemState item, Fraction probability) {}

  public record Bounds(Fraction success, Fraction failure, Fraction unresolved, boolean complete) {
    public Fraction upper() {
      return success.add(unresolved);
    }
  }

  /** Budgets produce unresolved mass only inside a validated normalized finite kernel. */
  public static Bounds firstHit(
      ItemState root,
      List<Function<ItemState, List<Outcome>>> steps,
      Function<ItemState, Status> evaluation,
      int maxEvaluations) {
    if (maxEvaluations < 1)
      throw new IllegalArgumentException("Positive evaluation budget required");
    var frontier = new LinkedHashMap<ItemState, Fraction>();
    frontier.put(root, Fraction.ONE);
    var success = Fraction.ZERO;
    var failure = Fraction.ZERO;
    var unresolved = Fraction.ZERO;
    int evaluated = 0;
    for (int depth = 0; depth <= steps.size(); depth++) {
      var next = new LinkedHashMap<ItemState, Fraction>();
      for (var entry : frontier.entrySet()) {
        if (evaluated++ >= maxEvaluations) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        var status = evaluation.apply(entry.getKey());
        if (status == Status.MATCH) {
          success = success.add(entry.getValue());
          continue;
        }
        if (status == Status.UNKNOWN || status == Status.UNSUPPORTED) {
          unresolved = unresolved.add(entry.getValue());
          continue;
        }
        if (depth == steps.size()) {
          failure = failure.add(entry.getValue());
          continue;
        }
        var outcomes = steps.get(depth).apply(entry.getKey());
        var total = Fraction.ZERO;
        for (var outcome : outcomes) total = total.add(outcome.probability());
        if (!total.equals(Fraction.ONE))
          throw new IllegalArgumentException("Kernel mass must equal one");
        for (var outcome : outcomes)
          next.merge(
              outcome.item(), entry.getValue().multiply(outcome.probability()), Fraction::add);
      }
      frontier = next;
    }
    return new Bounds(success, failure, unresolved, unresolved.equals(Fraction.ZERO));
  }
}
