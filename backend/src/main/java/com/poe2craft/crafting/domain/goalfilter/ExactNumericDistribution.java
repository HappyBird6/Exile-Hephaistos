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
    private record Value(BigInteger numerator, BigInteger denominator, boolean reduced) {}

    // Atomic immutable snapshots permit concurrent canonical reads while an exact sum is lazy.
    private volatile Value value;
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
      value = new Value(numerator.divide(divisor), denominator.divide(divisor), true);
    }

    private Fraction(Value exactSum) {
      value = exactSum;
    }

    private Value canonical() {
      var current = value;
      if (current.reduced()) return current;
      var divisor = current.numerator().gcd(current.denominator());
      var result =
          new Value(
              current.numerator().divide(divisor), current.denominator().divide(divisor), true);
      value = result;
      return result;
    }

    @com.fasterxml.jackson.annotation.JsonProperty
    public BigInteger numerator() {
      return canonical().numerator();
    }

    @com.fasterxml.jackson.annotation.JsonProperty
    public BigInteger denominator() {
      return canonical().denominator();
    }

    @Override
    public boolean equals(Object other) {
      return this == other || other instanceof Fraction f && canonical().equals(f.canonical());
    }

    @Override
    public int hashCode() {
      var v = canonical();
      return 31 * v.numerator().hashCode() + v.denominator().hashCode();
    }

    @Override
    public String toString() {
      var v = canonical();
      return "Fraction[numerator=" + v.numerator() + ", denominator=" + v.denominator() + "]";
    }

    public static Fraction of(long numerator, long denominator) {
      return new Fraction(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public Fraction add(Fraction other) {
      Objects.requireNonNull(other);
      var a = value;
      var b = other.value;
      var numerator = a.numerator();
      var denominator = a.denominator();
      if (numerator.signum() == 0) return other;
      if (b.numerator().signum() == 0) return this;
      if (denominator.equals(b.denominator()))
        return new Fraction(new Value(numerator.add(b.numerator()), denominator, false));
      // Markov exit laws often share a large denominator. Form their LCM instead
      // of squaring that denominator before reducing every accumulated outcome.
      var common = denominator.gcd(b.denominator());
      var leftScale = b.denominator().divide(common);
      var rightScale = denominator.divide(common);
      var sum = numerator.multiply(leftScale).add(b.numerator().multiply(rightScale));
      return new Fraction(new Value(sum, rightScale.multiply(b.denominator()), false));
    }

    public Fraction multiply(Fraction other) {
      Objects.requireNonNull(other);
      var a = canonical();
      var b = other.canonical();
      var numerator = a.numerator();
      var denominator = a.denominator();
      if (numerator.signum() == 0 || b.numerator().signum() == 0) return ZERO;
      if (equals(ONE)) return other;
      if (other.equals(ONE)) return this;
      var left = numerator.gcd(b.denominator());
      var right = b.numerator().gcd(denominator);
      return new Fraction(
          numerator.divide(left).multiply(b.numerator().divide(right)),
          denominator.divide(right).multiply(b.denominator().divide(left)),
          true);
    }

    /** Exact product comparison without constructing and reducing a temporary rational. */
    public boolean multiplyEquals(Fraction other, Fraction expected) {
      var a = value;
      var b = Objects.requireNonNull(other).value;
      var c = Objects.requireNonNull(expected).value;
      return a.numerator()
          .multiply(b.numerator())
          .multiply(c.denominator())
          .equals(c.numerator().multiply(a.denominator()).multiply(b.denominator()));
    }

    public Fraction pow(int exponent) {
      if (exponent < 0) throw new IllegalArgumentException("Negative exponent");
      var v = canonical();
      return new Fraction(v.numerator().pow(exponent), v.denominator().pow(exponent), true);
    }

    public Fraction complement() {
      var v = canonical();
      return new Fraction(v.denominator().subtract(v.numerator()), v.denominator(), true);
    }

    public Fraction divide(Fraction other) {
      var v = Objects.requireNonNull(other).canonical();
      if (v.numerator().signum() == 0) throw new IllegalArgumentException("Division by zero");
      return multiply(new Fraction(v.denominator(), v.numerator(), true));
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
