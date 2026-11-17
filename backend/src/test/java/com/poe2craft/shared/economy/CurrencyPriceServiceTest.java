package com.poe2craft.shared.economy;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.economy.application.*;
import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 캐시와 외부 수집기를 조정해 리그별 시세 스냅샷을 제공한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code snapshot(time, rate)}: 현재 내용을 불변 관측값으로 구성한다. 입력: {@code time}(시간), {@code
 *       rate}(rate). 반환: {@code PriceSnapshot}.
 *   <li>{@code service(source, cache, clock)}: 서비스 처리에서 {@code Duration.ofMinutes}, {@code
 *       Duration.ofSeconds}을(를) 사용한다. 입력: {@code source}(출처), {@code cache}(캐시), {@code clock}(현재
 *       시각 공급자). 반환: {@code CurrencyPriceService}.
 *   <li>{@code httpReadReturnsLastSnapshotWhileOneBackgroundRefreshWaits()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code automaticThirtyMinuteRefreshAndManualFiveMinuteCooldownAreSharedAcrossServices()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code failedCollectionKeepsLastGoodAndDoesNotHammerProvider()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code java.io.IOException}.
 *   <li>{@code concurrentVisitorsUseOneRefreshAndReceiveSharedSnapshot()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code coldStartFailureReturnsUnavailableAndSharesTheCooldown()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code java.io.IOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Time}: {@code CurrencyPriceServiceTest} 내부의 시간 처리를 맡는다.
 *   <li>{@code Cache}: {@code CurrencyPriceServiceTest} 내부의 캐시 처리를 맡는다.
 * </ul>
 */
class CurrencyPriceServiceTest {
  /**
   * {@code CurrencyPriceServiceTest} 내부의 시간 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code now}: 현재시각 ({@code Instant}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code getZone()}: 시간대을(를) 조회한다. 반환: {@code ZoneId}.
   *   <li>{@code withZone(zone)}: {@code this} 값을 조회한다. 입력: {@code zone}(시간대). 반환: {@code Clock}.
   *   <li>{@code instant()}: {@code now} 값을 조회한다. 반환: {@code Instant}.
   *   <li>{@code advance(duration)}: 허용된 계산량만큼 작업을 진행한다. 입력: {@code duration}(소요시간).
   * </ul>
   */
  static final class Time extends Clock {
    Instant now = Instant.parse("2026-11-09T00:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }
  }

  /**
   * {@code CurrencyPriceServiceTest} 내부의 캐시 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code clock}: 현재 시각 공급자 ({@code Time}).
   *   <li>{@code snapshot}: 스냅샷 ({@code PriceSnapshot}).
   *   <li>{@code allowed}: 허용된 ({@code Instant}).
   *   <li>{@code token}: 인증토큰 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Cache(clock)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code clock}(현재 시각 공급자).
   *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code PriceSnapshot}.
   *   <li>{@code claim(interval, lease)}: claim 처리에서 {@code clock.instant}, {@code
   *       UUID.randomUUID}을(를) 사용한다. 입력: {@code interval}(구간), {@code lease}(사용권). 반환: {@code
   *       String}.
   *   <li>{@code save(snapshot, token)}: 현재 값을 저장 경계에 기록한다. 입력: {@code snapshot}(스냅샷), {@code
   *       token}(인증토큰). 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code release(token)}: 해제의 입력에 따른 분기·결과를 처리한다. 입력: {@code token}(인증토큰).
   *   <li>{@code nextRefreshAllowed(now)}: 다음·갱신·허용된 처리에서 {@code allowed.isBefore}을(를) 사용한다. 입력:
   *       {@code now}(현재시각). 반환: {@code Instant}.
   * </ul>
   */
  static final class Cache implements CurrencyPriceCache {
    final Time clock;
    PriceSnapshot snapshot;
    Instant allowed = Instant.EPOCH;
    String token;

    Cache(Time clock) {
      this.clock = clock;
    }

    public synchronized PriceSnapshot read() {
      return snapshot;
    }

    public synchronized String claim(Duration interval, Duration lease) {
      if (token != null || clock.instant().isBefore(allowed)) return null;
      allowed = clock.instant().plus(interval);
      token = UUID.randomUUID().toString();
      return token;
    }

    public synchronized void save(PriceSnapshot snapshot, String token) {
      if (!Objects.equals(token, this.token)) throw new IllegalStateException();
      this.snapshot = snapshot;
    }

    public synchronized void release(String token) {
      if (Objects.equals(token, this.token)) this.token = null;
    }

    public Instant nextRefreshAllowed(Instant now) {
      return allowed.isBefore(now) ? now : allowed;
    }
  }

  private static PriceSnapshot snapshot(Instant time, int rate) {
    return new PriceSnapshot(
        "Current League",
        time,
        BigDecimal.valueOf(rate),
        Map.of(
            "Exalted Orb",
            new PriceSnapshot.Quote("Exalted Orb", new BigDecimal("0.002"), BigDecimal.ONE)));
  }

  private CurrencyPriceService service(CurrencyPriceSource source, Cache cache, Time clock) {
    return new CurrencyPriceService(
        source,
        cache,
        clock,
        Duration.ofMinutes(30),
        Duration.ofMinutes(5),
        Duration.ofSeconds(60));
  }

  @Test
  void httpReadReturnsLastSnapshotWhileOneBackgroundRefreshWaits() throws Exception {
    var time = new Time();
    var cache = new Cache(time);
    cache.snapshot = snapshot(time.instant().minus(Duration.ofHours(1)), 500);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var calls = new AtomicInteger();
    try (var service =
        service(
            now -> {
              calls.incrementAndGet();
              started.countDown();
              release.await(3, TimeUnit.SECONDS);
              return snapshot(now, 600);
            },
            cache,
            time)) {
      var current = service.current(false);
      assertThat(current.snapshot().exaltedPerDivine()).isEqualByComparingTo("500");
      assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
      for (int i = 0; i < 20; i++)
        assertThat(service.current(true).snapshot()).isEqualTo(current.snapshot());
      assertThat(calls).hasValue(1);
      release.countDown();
    } finally {
      release.countDown();
    }
  }

  @Test
  void automaticThirtyMinuteRefreshAndManualFiveMinuteCooldownAreSharedAcrossServices() {
    var time = new Time();
    var cache = new Cache(time);
    var requests = new AtomicInteger();
    CurrencyPriceSource source = collected -> snapshot(collected, 500 + requests.incrementAndGet());
    var first = service(source, cache, time);
    var second = service(source, cache, time);
    var original = first.get(false);
    assertThat(requests).hasValue(1);
    assertThat(second.get(true).snapshot()).isSameAs(original.snapshot());
    assertThat(requests).hasValue(1);
    time.advance(Duration.ofMinutes(5));
    var fresh = second.get(true);
    assertThat(requests).hasValue(2);
    assertThat(first.get(true).snapshot()).isSameAs(fresh.snapshot());
    assertThat(requests).hasValue(2);
    time.advance(Duration.ofMinutes(29));
    first.get(false);
    assertThat(requests).hasValue(2);
    time.advance(Duration.ofMinutes(1));
    first.get(false);
    assertThat(requests).hasValue(3);
  }

  @Test
  void failedCollectionKeepsLastGoodAndDoesNotHammerProvider() {
    var time = new Time();
    var cache = new Cache(time);
    cache.snapshot = snapshot(time.instant(), 500);
    var requests = new AtomicInteger();
    var service =
        service(
            now -> {
              requests.incrementAndGet();
              throw new java.io.IOException();
            },
            cache,
            time);
    time.advance(Duration.ofMinutes(31));
    var stale = service.get(true);
    assertThat(stale.snapshot()).isSameAs(cache.snapshot);
    assertThat(stale.stale()).isTrue();
    assertThat(stale.unavailable()).isFalse();
    service.get(true);
    service.get(false);
    assertThat(requests).hasValue(1);
    assertThat(stale.nextRefreshAllowedAt()).isEqualTo(time.instant().plus(Duration.ofMinutes(5)));
  }

  @Test
  void concurrentVisitorsUseOneRefreshAndReceiveSharedSnapshot() throws Exception {
    var time = new Time();
    var cache = new Cache(time);
    cache.snapshot = snapshot(time.instant(), 500);
    var started = new CountDownLatch(1);
    var finish = new CountDownLatch(1);
    var requests = new AtomicInteger();
    CurrencyPriceSource source =
        now -> {
          requests.incrementAndGet();
          started.countDown();
          finish.await(2, TimeUnit.SECONDS);
          return snapshot(now, 600);
        };
    var one = service(source, cache, time);
    var two = service(source, cache, time);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var updating = executor.submit(() -> one.get(true));
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(two.get(true).snapshot().exaltedPerDivine()).isEqualByComparingTo("500");
      finish.countDown();
      updating.get(2, TimeUnit.SECONDS);
      assertThat(two.get(true).snapshot().exaltedPerDivine()).isEqualByComparingTo("600");
      assertThat(requests).hasValue(1);
    }
  }

  @Test
  void coldStartFailureReturnsUnavailableAndSharesTheCooldown() {
    var time = new Time();
    var cache = new Cache(time);
    var requests = new AtomicInteger();
    var service =
        service(
            now -> {
              requests.incrementAndGet();
              throw new java.io.IOException();
            },
            cache,
            time);
    assertThat(service.get(true).unavailable()).isTrue();
    assertThat(service.get(true).snapshot()).isNull();
    assertThat(requests).hasValue(1);
  }
}
