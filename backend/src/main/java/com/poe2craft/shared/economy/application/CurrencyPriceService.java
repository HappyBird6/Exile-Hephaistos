package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CurrencyPriceService {
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

  public record View(
      PriceSnapshot snapshot, Instant nextRefreshAllowedAt, boolean stale, boolean unavailable) {}

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
        snapshot == null);
  }
}
