package com.poe2craft.shared.economy.api;

import com.poe2craft.shared.economy.api.dto.Status;
import com.poe2craft.shared.economy.application.CurrencyPriceService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * prices에서 화폐 시세를 읽어 HTTP 응답을 만든다. current는 캐시 가능한 현재 시세, status는 판본과 갱신 상태, refresh는 갱신을 요청한 뒤의
 * 시세를 반환한다.
 */
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
        .eTag(CurrencyPriceRevision.etag(view))
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePublic())
        .body(view);
  }

  @GetMapping("/status")
  public ResponseEntity<Status> status() {
    var view = prices.current(false);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new Status(
                CurrencyPriceRevision.revision(view),
                view.nextRefreshAllowedAt(),
                view.stale(),
                view.unavailable(),
                view.refreshing()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<CurrencyPriceService.View> refresh() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(prices.current(true));
  }
}
