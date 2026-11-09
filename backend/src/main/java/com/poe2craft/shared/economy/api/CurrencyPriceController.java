package com.poe2craft.shared.economy.api;

import com.poe2craft.shared.economy.application.CurrencyPriceService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/economy/prices")
public final class CurrencyPriceController {
  private final CurrencyPriceService prices;

  public CurrencyPriceController(CurrencyPriceService prices) {
    this.prices = prices;
  }

  @GetMapping
  public ResponseEntity<CurrencyPriceService.View> current() {
    return response(prices.get(false));
  }

  @PostMapping("/refresh")
  public ResponseEntity<CurrencyPriceService.View> refresh() {
    return response(prices.get(true));
  }

  private ResponseEntity<CurrencyPriceService.View> response(CurrencyPriceService.View view) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
  }
}
