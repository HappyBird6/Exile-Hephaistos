package com.poe2craft.crafting.domain.goalfilter;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.StateBucket;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ExactNumericDistributionTest {
  @Test
  void accumulatedUnreducedSumsKeepCanonicalJsonAndConcurrentValueIdentity() throws Exception {
    var random = new Random(732);
    var values = new ArrayList<Fraction>();
    var expected = new ArrayList<Fraction>();
    for (int trial = 0; trial < 100; trial++) {
      var sum = Fraction.ZERO;
      var numerator = java.math.BigInteger.ZERO;
      var denominator = java.math.BigInteger.ONE;
      for (int i = 0; i < 30; i++) {
        var n = java.math.BigInteger.valueOf(random.nextInt(25));
        var d = java.math.BigInteger.valueOf(1 + random.nextInt(25));
        sum = sum.add(new Fraction(n, d));
        numerator = numerator.multiply(d).add(n.multiply(denominator));
        denominator = denominator.multiply(d);
        var gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
      }
      values.add(sum);
      expected.add(new Fraction(numerator, denominator));
    }
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
      var jobs = new ArrayList<java.util.concurrent.Future<?>>();
      for (int thread = 0; thread < 8; thread++)
        jobs.add(
            pool.submit(
                () -> {
                  var json = new com.fasterxml.jackson.databind.ObjectMapper();
                  for (int i = 0; i < values.size(); i++) {
                    var actual = values.get(i);
                    var oracle = expected.get(i);
                    assertThat(actual).isEqualTo(oracle).hasSameHashCodeAs(oracle);
                    assertThat(actual.numerator()).isEqualTo(oracle.numerator());
                    assertThat(actual.denominator()).isEqualTo(oracle.denominator());
                    assertThat(actual.multiply(Fraction.of(17, 23)))
                        .isEqualTo(
                            new Fraction(
                                oracle.numerator().multiply(java.math.BigInteger.valueOf(17)),
                                oracle.denominator().multiply(java.math.BigInteger.valueOf(23))));
                    assertThat(json.writeValueAsString(actual))
                        .isEqualTo(json.writeValueAsString(oracle));
                  }
                  return null;
                }));
      for (var job : jobs) job.get();
    }
  }

  @Test
  void canonicalArithmeticPreservesValueIdentityAndJsonContract() throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var half = Fraction.of(2, 4);
    assertThat(json.writeValueAsString(half)).isEqualTo("{\"numerator\":1,\"denominator\":2}");
    assertThat(json.readValue("{\"numerator\":2,\"denominator\":4}", Fraction.class))
        .isEqualTo(half);
    assertThat(half.toString()).isEqualTo("Fraction[numerator=1, denominator=2]");
    assertThat(half.hashCode())
        .isEqualTo(31 * half.numerator().hashCode() + half.denominator().hashCode());
    assertThat(Fraction.ZERO.pow(0)).isEqualTo(Fraction.ONE);
    assertThatThrownBy(() -> half.pow(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> half.divide(Fraction.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Fraction.of(3, 2).complement())
        .isInstanceOf(IllegalArgumentException.class);
    for (int a = 0; a <= 12; a++)
      for (int b = 1; b <= 12; b++) {
        var value = Fraction.of(a, b);
        assertThat(value.pow(11))
            .isEqualTo(new Fraction(value.numerator().pow(11), value.denominator().pow(11)));
        if (a <= b) assertThat(value.complement()).isEqualTo(Fraction.of(b - a, b));
        for (int c = 1; c <= 12; c++) {
          var divisor = Fraction.of(c, b + 1);
          assertThat(value.divide(divisor))
              .isEqualTo(Fraction.of((long) a * (b + 1), (long) b * c));
        }
      }
  }

  @Test
  void arithmeticMatchesDirectRationalOracleWithLargeSharedDenominators() {
    var random = new Random(913);
    for (int i = 0; i < 250; i++) {
      var shared = new java.math.BigInteger(1024, random).add(java.math.BigInteger.ONE);
      var a =
          new Fraction(
              new java.math.BigInteger(256, random),
              shared.multiply(java.math.BigInteger.valueOf(1 + random.nextInt(31))));
      var b =
          new Fraction(
              new java.math.BigInteger(256, random),
              shared.multiply(java.math.BigInteger.valueOf(1 + random.nextInt(31))));
      assertThat(a.add(b))
          .isEqualTo(
              new Fraction(
                  a.numerator()
                      .multiply(b.denominator())
                      .add(b.numerator().multiply(a.denominator())),
                  a.denominator().multiply(b.denominator())));
      assertThat(a.multiply(b))
          .isEqualTo(
              new Fraction(
                  a.numerator().multiply(b.numerator()),
                  a.denominator().multiply(b.denominator())));
      assertThat(a.multiplyEquals(b, a.multiply(b))).isTrue();
      assertThat(a.add(a).multiplyEquals(b.add(b), a.multiply(b).multiply(Fraction.of(4, 1))))
          .isTrue();
      assertThat(a.multiplyEquals(b, a.multiply(b).add(Fraction.ONE))).isFalse();
      assertThat(a.add(Fraction.ZERO)).isEqualTo(a);
      assertThat(Fraction.ZERO.add(a)).isEqualTo(a);
      assertThat(a.multiply(Fraction.ONE)).isEqualTo(a);
      assertThat(Fraction.ONE.multiply(a)).isEqualTo(a);
      assertThat(a.multiply(Fraction.ZERO)).isEqualTo(Fraction.ZERO);
      assertThat(a.add(a)).isEqualTo(new Fraction(a.numerator().shiftLeft(1), a.denominator()));
    }
  }

  ItemState state(long cold, long all) {
    return new ItemState(
        "synthetic",
        "base",
        82,
        ItemState.Rarity.RARE,
        List.of(),
        List.of(new ModifierInstance("joint-mod", Map.of("cold", cold, "all", all))),
        Set.of());
  }

  Status evaluate(ItemState state) {
    var values = state.explicits().getFirst().values();
    return values.get("cold") + values.get("all") >= 22 ? Status.MATCH : Status.NO_MATCH;
  }

  @Test
  void fullNumericKeysSeparateStatesMergedByLegacyBucketAndAgreeWithEnumeration() {
    var low = state(10, 0);
    var high = state(10, 12);
    assertThat(StateBucket.from(low)).isEqualTo(StateBucket.from(high));
    assertThat(low).isNotEqualTo(high);
    Function<ItemState, List<Outcome>> kernel =
        item -> List.of(new Outcome(low, Fraction.of(3, 4)), new Outcome(high, Fraction.of(1, 4)));
    var result = ExactNumericDistribution.firstHit(low, List.of(kernel), this::evaluate, 100);
    // Four equally likely elementary cases: low, low, low, high.
    int exhaustiveSuccess = 0;
    for (var outcome : List.of(low, low, low, high))
      if (evaluate(outcome) == Status.MATCH) exhaustiveSuccess++;
    assertThat(result.success()).isEqualTo(Fraction.of(exhaustiveSuccess, 4));
    assertThat(result.failure()).isEqualTo(Fraction.of(3, 4));
    assertThat(result.unresolved()).isEqualTo(Fraction.ZERO);
  }

  @Test
  void hybridJointTuplesAreNotIndependentMarginals() {
    var root = state(0, 0);
    Function<ItemState, List<Outcome>> joint =
        item ->
            List.of(
                new Outcome(state(10, 12), Fraction.of(1, 2)),
                new Outcome(state(0, 0), Fraction.of(1, 2)));
    var result = ExactNumericDistribution.firstHit(root, List.of(joint), this::evaluate, 100);
    // Independent decomposition of these correlated stats would incorrectly yield 1/4.
    assertThat(result.success()).isEqualTo(Fraction.of(1, 2));
  }

  @Test
  void firstHitAbsorbsBeforeDestructiveNextStepAndBudgetRetainsMass() {
    var root = state(0, 0);
    Function<ItemState, List<Outcome>> first =
        item ->
            List.of(
                new Outcome(state(10, 12), Fraction.of(1, 2)),
                new Outcome(root, Fraction.of(1, 2)));
    Function<ItemState, List<Outcome>> destroy =
        item -> {
          assertThat(evaluate(item)).isEqualTo(Status.NO_MATCH);
          return List.of(new Outcome(root, Fraction.ONE));
        };
    var result =
        ExactNumericDistribution.firstHit(root, List.of(first, destroy), this::evaluate, 100);
    assertThat(result.success()).isEqualTo(Fraction.of(1, 2));
    assertThat(result.failure()).isEqualTo(Fraction.of(1, 2));
    var partial = ExactNumericDistribution.firstHit(root, List.of(first), this::evaluate, 2);
    assertThat(partial.success()).isEqualTo(Fraction.of(1, 2));
    assertThat(partial.unresolved()).isEqualTo(Fraction.of(1, 2));
    assertThat(partial.success().add(partial.failure()).add(partial.unresolved()))
        .isEqualTo(Fraction.ONE);
    assertThat(partial.upper()).isEqualTo(Fraction.ONE);
    assertThat(partial.complete()).isFalse();
  }

  @Test
  void unsupportedEvaluationNeverBecomesFirstHitSuccessAndInvalidKernelsAreRejected() {
    var root = state(10, 12);
    var result = ExactNumericDistribution.firstHit(root, List.of(), item -> Status.UNSUPPORTED, 1);
    assertThat(result.success()).isEqualTo(Fraction.ZERO);
    assertThat(result.unresolved()).isEqualTo(Fraction.ONE);
    Function<ItemState, List<Outcome>> invalid =
        item -> List.of(new Outcome(root, Fraction.of(1, 2)));
    assertThatThrownBy(
            () ->
                ExactNumericDistribution.firstHit(
                    root, List.of(invalid), item -> Status.NO_MATCH, 100))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
