package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 캐시와 외부 수집기를 조정해 리그별 시세 스냅샷을 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code refreshWorker}: 갱신·작업자 ({@code java.util.concurrent.ExecutorService}).
 *   <li>{@code refreshing}: refreshing ({@code java.util.concurrent.atomic.AtomicBoolean}).
 *   <li>{@code LOG}: 로그 ({@code Logger}).
 *   <li>{@code source}: 출처 ({@code CurrencyPriceSource}).
 *   <li>{@code cache}: 캐시 ({@code CurrencyPriceCache}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code interval}: 구간 ({@code Duration}).
 *   <li>{@code minimumInterval}: 최소값·구간 ({@code Duration}).
 *   <li>{@code lease}: 사용권 ({@code Duration}).
 *   <li>{@code lastGood}: 마지막·Good ({@code PriceSnapshot}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code current(manualRefresh)}: 현재 버전의 정의나 실행 값을 조회한다. 입력: {@code manualRefresh}(수동·갱신).
 *       반환: {@code View}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code CurrencyPriceService(source, cache, clock, interval, minimumInterval, lease)}: 전달받은
 *       값으로 구성원을 초기화한다. 입력: {@code source}(출처), {@code cache}(캐시), {@code clock}(현재 시각 공급자), {@code
 *       interval}(구간), {@code minimumInterval}(최소값·구간), {@code lease}(사용권).
 *   <li>{@code get(manualRefresh)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code manualRefresh}(수동·갱신). 반환:
 *       {@code View}.
 *   <li>{@code view(snapshot, nextAllowed)}: 내부 계산 상태를 읽기 전용 조회 값으로 구성한다. 입력: {@code
 *       snapshot}(스냅샷), {@code nextAllowed}(다음·허용된). 반환: {@code View}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code View}: {@code CurrencyPriceService}에서 사용하는 조회표현의 불변 값이다.
 * </ul>
 */
public final class CurrencyPriceService implements AutoCloseable {
  private final java.util.concurrent.ExecutorService refreshWorker =
      java.util.concurrent.Executors.newSingleThreadExecutor(
          r -> {
            var thread = new Thread(r, "currency-price-refresh");
            thread.setDaemon(true);
            return thread;
          });
  private final java.util.concurrent.atomic.AtomicBoolean refreshing =
      new java.util.concurrent.atomic.AtomicBoolean();

  /**
   * HTTP reads never wait for the external price provider. The Redis lease still coordinates
   * replicas.
   */
  public View current(boolean manualRefresh) {
    var now = clock.instant();
    View current;
    try {
      var shared = cache.read();
      if (shared != null) lastGood = shared;
      current = view(lastGood, cache.nextRefreshAllowed(now));
    } catch (RuntimeException unavailable) {
      return view(lastGood, now.plus(minimumInterval));
    }
    if ((manualRefresh || current.stale() || current.unavailable())
        && !now.isBefore(current.nextRefreshAllowedAt())
        && refreshing.compareAndSet(false, true)) {
      try {
        refreshWorker.execute(
            () -> {
              try {
                get(manualRefresh);
              } finally {
                refreshing.set(false);
              }
            });
      } catch (java.util.concurrent.RejectedExecutionException stopped) {
        refreshing.set(false);
      }
    }
    // Read the flag first: when it is false after completion, the volatile snapshot already
    // contains that completed refresh. A fast source must not return old data with no follow-up.
    boolean busy = refreshing.get();
    var snapshot = lastGood;
    return new View(
        snapshot,
        current.nextRefreshAllowedAt(),
        snapshot != null && !now.isBefore(snapshot.updatedAt().plus(interval)),
        snapshot == null,
        busy);
  }

  @Override
  public void close() {
    refreshWorker.shutdownNow();
  }

  private static final Logger LOG = LoggerFactory.getLogger(CurrencyPriceService.class);
  private final CurrencyPriceSource source;
  private final CurrencyPriceCache cache;
  private final Clock clock;
  private final Duration interval, minimumInterval, lease;
  private volatile PriceSnapshot lastGood;

  public CurrencyPriceService(
      CurrencyPriceSource source,
      CurrencyPriceCache cache,
      Clock clock,
      Duration interval,
      Duration minimumInterval,
      Duration lease) {
    this.source = source;
    this.cache = cache;
    this.clock = clock;
    this.interval = interval;
    this.minimumInterval = minimumInterval;
    this.lease = lease;
  }

  /**
   * {@code CurrencyPriceService}에서 사용하는 조회표현의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code snapshot}: 스냅샷 ({@code PriceSnapshot}).
   *   <li>{@code nextRefreshAllowedAt}: 다음·갱신·허용된·기준 ({@code Instant}).
   *   <li>{@code stale}: stale ({@code boolean}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   *   <li>{@code refreshing}: refreshing ({@code boolean}).
   * </ul>
   */
  public record View(
      PriceSnapshot snapshot,
      Instant nextRefreshAllowedAt,
      boolean stale,
      boolean unavailable,
      boolean refreshing) {}

  public View get(boolean manualRefresh) {
    var now = clock.instant();
    try {
      var previous = cache.read();
      if (previous != null) lastGood = previous;
      if (manualRefresh || previous == null || !now.isBefore(previous.updatedAt().plus(interval))) {
        var token = cache.claim(minimumInterval, lease);
        if (token != null) {
          try {
            var fresh = source.fetch(clock.instant());
            cache.save(fresh, token);
            lastGood = fresh;
          } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            LOG.warn(
                "Currency price collection failed; retaining the last successful snapshot ({})",
                failure.getClass().getSimpleName());
          } finally {
            cache.release(token);
          }
        }
      }
      var shared = cache.read();
      if (shared != null) lastGood = shared;
      return view(lastGood, cache.nextRefreshAllowed(clock.instant()));
    } catch (RuntimeException unavailable) {
      // Never bypass the shared limit with an uncached upstream request when Redis is unavailable.
      LOG.warn(
          "Shared currency price cache unavailable ({})", unavailable.getClass().getSimpleName());
      return view(lastGood, now.plus(minimumInterval));
    }
  }

  private View view(PriceSnapshot snapshot, Instant nextAllowed) {
    return new View(
        snapshot,
        nextAllowed,
        snapshot != null && !clock.instant().isBefore(snapshot.updatedAt().plus(interval)),
        snapshot == null,
        refreshing.get());
  }
}
