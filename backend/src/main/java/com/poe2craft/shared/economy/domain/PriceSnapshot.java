package com.poe2craft.shared.economy.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Prices are estimates from one league and one collection, separate from crafting rules. */
public record PriceSnapshot(
    String league, Instant updatedAt, BigDecimal exaltedPerDivine, Map<String, Quote> prices) {
  public record Quote(String name, BigDecimal divine, BigDecimal exalted) {}

  public PriceSnapshot {
    if (league == null
        || league.isBlank()
        || updatedAt == null
        || exaltedPerDivine == null
        || exaltedPerDivine.signum() <= 0
        || prices == null
        || prices.isEmpty()) throw new IllegalArgumentException("Invalid price snapshot");
    prices = Map.copyOf(prices);
  }
}
