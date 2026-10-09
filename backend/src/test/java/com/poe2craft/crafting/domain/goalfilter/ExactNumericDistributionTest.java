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
