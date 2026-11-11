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
    var view = prices.current(false);
    return ResponseEntity.ok()
        .eTag(etag(view))
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePublic())
        .body(view);
  }

  public record Status(
      String revision,
      java.time.Instant nextRefreshAllowedAt,
      boolean stale,
      boolean unavailable,
      boolean refreshing) {}

  @GetMapping("/status")
  public ResponseEntity<Status> status() {
    var view = prices.current(false);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new Status(
                revision(view),
                view.nextRefreshAllowedAt(),
                view.stale(),
                view.unavailable(),
                view.refreshing()));
  }

  private String revision(CurrencyPriceService.View view) {
    return view.snapshot() == null
        ? null
        : view.snapshot().league() + ":" + view.snapshot().updatedAt();
  }

  private String etag(CurrencyPriceService.View view) {
    // Once the cooldown expires, moving "allowed now" timestamps are semantically equivalent.
    String value =
        revision(view)
            + ":"
            + view.stale()
            + ":"
            + view.unavailable()
            + ":"
            + view.refreshing()
            + ":"
            + (view.nextRefreshAllowedAt().isAfter(java.time.Instant.now())
                ? view.nextRefreshAllowedAt()
                : "ready");
    try {
      return "W/\""
          + java.util.HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
          + "\"";
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<CurrencyPriceService.View> refresh() {
    return response(prices.current(true));
  }

  private ResponseEntity<CurrencyPriceService.View> response(CurrencyPriceService.View view) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
  }
}
