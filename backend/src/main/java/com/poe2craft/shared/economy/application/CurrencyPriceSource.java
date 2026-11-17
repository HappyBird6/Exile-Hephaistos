package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Instant;

/**
 * 외부 시세 수집기의 입력·출력 계약을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code fetch(collectedAt)}: fetch의 실행 계약을 정의한다. 입력: {@code collectedAt}(collected·기준). 반환:
 *       {@code PriceSnapshot}. 사용하는 예외: {@code Exception}.
 * </ul>
 */
public interface CurrencyPriceSource {
  PriceSnapshot fetch(Instant collectedAt) throws Exception;
}
