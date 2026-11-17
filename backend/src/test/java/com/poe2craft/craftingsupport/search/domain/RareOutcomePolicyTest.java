package com.poe2craft.craftingsupport.search.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 전체 조건부 방법 분포에 적용하는 희귀 결과 경계 정책이다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code p(n, d)}: p 처리에서 {@code CertifiedInterval.enclose}, {@code Fraction.of}을(를) 사용한다.
 *       입력: {@code n}(n), {@code d}(d). 반환: {@code CertifiedInterval}.
 *   <li>{@code point()}: 지점 값을 조회·구성한다. 반환: {@code CertifiedMethodRunner.Observation<String,
 *       String>}.
 *   <li>{@code groupsBeforeCuttingKeepsGoalsUnknownAndCostsWithoutRenormalization()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code combinedRareMassCannotExceedMethodAllowanceAndDisabledIsIdentity()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code unknownPolicyVersionIsRejected()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class RareOutcomePolicyTest {
  static CertifiedInterval p(long n, long d) {
    return CertifiedInterval.enclose(Fraction.of(n, d), 64);
  }

  static CertifiedMethodRunner.Observation<String, String> point() {
    var exits = new EnumMap<Exit, Map<String, CertifiedInterval>>(Exit.class);
    for (var exit : Exit.values()) exits.put(exit, new LinkedHashMap<>());
    exits
        .get(Exit.CHECKPOINT)
        .putAll(
            Map.of("rare", p(1, 128), "goal", p(1, 128), "common-a", p(1, 4), "common-b", p(1, 4)));
    exits.get(Exit.UNRESOLVED).put("unknown", p(1, 128));
    exits.get(Exit.LOSS).put("lost", p(61, 128));
    return new CertifiedMethodRunner.Observation<>(
        100, exits, Map.of(), Map.of("orb", p(1, 1)), Map.of(), p(1, 1));
  }

  @Test
  void groupsBeforeCuttingKeepsGoalsUnknownAndCostsWithoutRenormalization() {
    var policy = new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, 300000, 300000);
    var before = point();
    var after =
        policy.apply(before, s -> s.startsWith("common") ? "common" : s, "goal"::equals, true);
    assertThat(after.exits().get(Exit.REBASE)).containsOnlyKeys("rare");
    assertThat(after.exits().get(Exit.GOAL)).containsEntry("goal", p(1, 128));
    assertThat(after.exits().get(Exit.CHECKPOINT))
        .containsEntry("common-a", p(1, 4))
        .containsEntry("common-b", p(1, 4));
    assertThat(after.exits().get(Exit.UNRESOLVED)).isEqualTo(before.exits().get(Exit.UNRESOLVED));
    var total = p(0, 1);
    for (var law : after.exits().values()) for (var mass : law.values()) total = total.add(mass);
    assertThat(total).isEqualTo(p(1, 1));
    assertThat(after.initialUses()).containsEntry("orb", p(1, 1));
    assertThat(policy.apply(after, s -> s, "goal"::equals, true)).isSameAs(after);
  }

  @Test
  void combinedRareMassCannotExceedMethodAllowanceAndDisabledIsIdentity() {
    var before = point();
    assertThat(RareOutcomePolicy.disabled().apply(before, s -> s, s -> false, true))
        .isSameAs(before);
    var policy = new RareOutcomePolicy(RareOutcomePolicy.VERSION, true, 10000, 10000);
    var after = policy.apply(before, s -> s, s -> false, true);
    // Equal-probability groups are accepted or retained together, independent of map order.
    assertThat(after.exits().get(Exit.REBASE)).isEmpty();
    assertThat(after.exits().get(Exit.UNRESOLVED)).isEqualTo(before.exits().get(Exit.UNRESOLVED));
  }

  @Test
  void unknownPolicyVersionIsRejected() {
    assertThatThrownBy(() -> new RareOutcomePolicy("future", true, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
