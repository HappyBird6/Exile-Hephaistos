package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.workbench.display.DisplayLocale;
import com.poe2craft.workbench.repeat.ChaosRepeatSubmission;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Public·Contract 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code everyPublishedLocaleHasAnExplicitServerFormatter()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다.
 *   <li>{@code transportReadsThePublicContractRatherThanItsOwnLimit()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
class PublicContractTest {
  @Test
  void everyPublishedLocaleHasAnExplicitServerFormatter() {
    var codes = PublicContracts.LOCALE_CODES;
    assertThat(Arrays.stream(DisplayLocale.values()).map(DisplayLocale::code).toList())
        .containsExactlyInAnyOrderElementsOf(codes);
  }

  @Test
  void transportReadsThePublicContractRatherThanItsOwnLimit() {
    assertThat(ChaosRepeatSubmission.MAX_BODY_BYTES)
        .isEqualTo(PublicContracts.REPEAT.maximumBodyBytes());
    assertThat(PublicContracts.REPEAT.defaultUses())
        .isBetween(PublicContracts.REPEAT.minimumUses(), PublicContracts.REPEAT.maximumUses());
  }
}
