package com.poe2craft.shared.crafting.domain;

import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import java.util.Set;

/**
 * 과거·자동·정책 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Explicit pre-deferral fixtures keep previously implemented omen laws under regression
 * tests.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code HistoricalAutomaticPolicy()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code beforeDeferrals()}: 변경전·Deferrals 값을 조회·구성한다. 반환: {@code AutomaticCurrencyPolicy}.
 * </ul>
 */
public final class HistoricalAutomaticPolicy {
  private HistoricalAutomaticPolicy() {}

  public static AutomaticCurrencyPolicy beforeDeferrals() {
    return withHistoricalChaos(
        Set.of("OMEN:SINISTRAL_ERASURE", "OMEN:DEXTRAL_ERASURE", "OMEN:WHITTLING"));
  }

  /** 두 옵션 카오스의 과거 수치 분포를 보존하면서 현행 티어 묶음을 검증한다. */
  public static AutomaticCurrencyPolicy beforeFirstAffixRestriction() {
    return withHistoricalChaos(Set.of());
  }

  private static AutomaticCurrencyPolicy withHistoricalChaos(Set<String> deferred) {
    var current = AutomaticCurrencyPolicyLoader.load().document();
    var entries =
        current.entries().stream()
            .map(
                e ->
                    e.id().equals("CHAOS")
                        ? new AutomaticCurrencyPolicy.Entry(
                            e.number(),
                            e.id(),
                            e.nameKo(),
                            e.sourceUrl(),
                            e.effectKo(),
                            e.decision(),
                            "Historical pre-first-affix fixture",
                            AutomaticCurrencyPolicy.Condition.GOAL_ACQUISITION)
                        : deferred.contains(e.id())
                            ? new AutomaticCurrencyPolicy.Entry(
                                e.number(),
                                e.id(),
                                e.nameKo(),
                                e.sourceUrl(),
                                e.effectKo(),
                                AutomaticCurrencyPolicy.Decision.CONDITIONAL,
                                "Historical engine fixture",
                                AutomaticCurrencyPolicy.Condition.TEMPORARY_REMOVAL)
                            : e)
            .toList();
    return new AutomaticCurrencyPolicy(
        new AutomaticCurrencyPolicy.Document(
            current.schemaVersion(),
            current.policyVersion(),
            current.sourceUrl(),
            current.sourceModifiedAt(),
            current.maximumExploredExplicits(),
            entries));
  }
}
