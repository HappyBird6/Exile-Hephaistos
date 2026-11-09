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

class CurrencyPriceServiceTest {
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
