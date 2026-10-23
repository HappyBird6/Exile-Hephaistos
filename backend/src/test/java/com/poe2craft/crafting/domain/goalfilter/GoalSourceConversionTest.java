package com.poe2craft.crafting.domain.goalfilter;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class GoalSourceConversionTest {
  private GoalEvaluator.Evaluation evaluate(
      Type type, GoalNumber number, Range rowRange, Range groupRange, BigDecimal weight) {
    var row = new Entry("e", "s", "flat", rowRange, weight, false);
    var goal =
        new GoalFilter(
            1, "test", null, List.of(new Group("g", type, false, groupRange, List.of(row))));
    return new GoalEvaluator()
        .evaluate(
            goal,
            Status.MATCH,
            Map.of("s", GoalEvaluator.Observation.presentNumber(number)),
            List.of());
  }

  @Test
  void repeatingRegenerationNeverUsesRoundedDecimalForInclusiveBounds() {
    var number = new GoalNumber(BigDecimal.valueOf(91), BigDecimal.valueOf(60));
    var roundedUp = number.display();
    assertThat(number.ratio()).isEqualTo(new GoalNumber.Ratio("91", "60"));
    assertThat(evaluate(Type.AND, number, new Range(roundedUp, null), null, null).status())
        .isEqualTo(Status.NO_MATCH);
    var lower = new BigDecimal("1.516666666666666666666666666666666");
    assertThat(evaluate(Type.AND, number, new Range(null, lower), null, null).status())
        .isEqualTo(Status.NO_MATCH);
    var result = evaluate(Type.AND, number, new Range(lower, roundedUp), null, null);
    assertThat(result.status()).isEqualTo(Status.MATCH);
    assertThat(result.groups().getFirst().entries().getFirst().exactValue())
        .isEqualTo(number.ratio());
  }

  @Test
  void bothWeightedModesAndPseudoSumsKeepExactRationalValues() {
    var number = new GoalNumber(BigDecimal.ONE, BigDecimal.valueOf(3));
    var below = new BigDecimal("0.9999999999999999999999999999999999");
    for (var type : List.of(Type.WEIGHTED_V1, Type.WEIGHTED_V2)) {
      var result =
          evaluate(
              type,
              number,
              new Range(null, null),
              new Range(BigDecimal.ONE, BigDecimal.ONE),
              BigDecimal.valueOf(3));
      assertThat(result.status()).isEqualTo(Status.MATCH);
      assertThat(result.groups().getFirst().score()).isEqualByComparingTo(BigDecimal.ONE);
      assertThat(
              evaluate(
                      type,
                      number,
                      new Range(null, null),
                      new Range(null, below),
                      BigDecimal.valueOf(3))
                  .status())
          .isEqualTo(Status.NO_MATCH);
    }
    var source = GoalEvaluator.Observation.presentNumber(number);
    var sum =
        GoalEvaluator.pseudo(
            List.of(new GoalCatalog.Contribution("s", BigDecimal.valueOf(3))), Map.of("s", source));
    assertThat(sum.exact().within(new Range(BigDecimal.ONE, BigDecimal.ONE))).isTrue();
    assertThat(sum.exact().ratio()).isNull();
  }

  @Test
  void reviewedLeechConversionHasNoPrematureRounding() {
    var leech = new GoalNumber(BigDecimal.valueOf(601), BigDecimal.valueOf(100));
    assertThat(leech.display()).isEqualByComparingTo("6.01");
    assertThat(leech.ratio()).isNull();
    assertThat(leech.within(new Range(new BigDecimal("6.01"), new BigDecimal("6.01")))).isTrue();
    assertThat(leech.within(new Range(null, new BigDecimal("6.009999")))).isFalse();
  }
}
