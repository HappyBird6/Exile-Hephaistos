package com.poe2craft.craftingsupport.goal.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 목표·출처·Conversion 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code evaluate(type, number, rowRange, groupRange, weight)}: 입력 조건이나 정책을 평가해 결과를 구한다. 입력:
 *       {@code type}(타입), {@code number}(숫자), {@code rowRange}(행·범위), {@code groupRange}(그룹·범위),
 *       {@code weight}(가중치). 반환: {@code GoalEvaluator.Evaluation}.
 *   <li>{@code repeatingRegenerationNeverUsesRoundedDecimalForInclusiveBounds()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code bothWeightedModesAndPseudoSumsKeepExactRationalValues()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code reviewedLeechConversionHasNoPrematureRounding()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 * </ul>
 */
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
