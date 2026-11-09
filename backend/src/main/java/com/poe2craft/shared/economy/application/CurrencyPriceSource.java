package com.poe2craft.shared.economy.application;

import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Instant;

public interface CurrencyPriceSource {
  PriceSnapshot fetch(Instant collectedAt) throws Exception;
}
