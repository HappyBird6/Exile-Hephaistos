package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Duration;
import java.time.Instant;

/** A single refresh lease and cooldown are shared by every caller and backend instance. */
public interface CurrencyPriceCache {
  PriceSnapshot read();

  String claim(Duration minimumInterval, Duration lease);

  void save(PriceSnapshot snapshot, String token);

  void release(String token);

  Instant nextRefreshAllowed(Instant now);
}
