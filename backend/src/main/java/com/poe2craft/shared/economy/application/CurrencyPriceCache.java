package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Duration;
import java.time.Instant;

/**
 * 여러 요청·서버가 시세 갱신 잠금과 재시도 간격을 공유하는 계약이다.
 *
 * <p>기존 계약: A single refresh lease and cooldown are shared by every caller and backend instance.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code PriceSnapshot}.
 *   <li>{@code claim(minimumInterval, lease)}: claim의 실행 계약을 정의한다. 입력: {@code
 *       minimumInterval}(최소값·구간), {@code lease}(사용권). 반환: {@code String}.
 *   <li>{@code save(snapshot, token)}: 현재 값을 저장 경계에 기록한다. 입력: {@code snapshot}(스냅샷), {@code
 *       token}(인증토큰).
 *   <li>{@code release(token)}: 해제의 실행 계약을 정의한다. 입력: {@code token}(인증토큰).
 *   <li>{@code nextRefreshAllowed(now)}: 다음·갱신·허용된의 실행 계약을 정의한다. 입력: {@code now}(현재시각). 반환: {@code
 *       Instant}.
 * </ul>
 */
public interface CurrencyPriceCache {
  PriceSnapshot read();

  String claim(Duration minimumInterval, Duration lease);

  void save(PriceSnapshot snapshot, String token);

  void release(String token);

  Instant nextRefreshAllowed(Instant now);
}
