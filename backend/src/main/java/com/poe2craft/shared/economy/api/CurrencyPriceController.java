package com.poe2craft.shared.economy.api;

import com.poe2craft.shared.economy.application.CurrencyPriceService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 제작 화폐 시세 조회 요청을 가격 서비스에 전달한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code prices}: 시세목록 ({@code CurrencyPriceService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CurrencyPriceController(prices)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code prices}(시세목록).
 *   <li>{@code current()}: 현재 버전의 정의나 실행 값을 조회한다. 반환: {@code
 *       ResponseEntity<CurrencyPriceService.View>}.
 *   <li>{@code status()}: 현재 진행 상태를 조회한다. 반환: {@code ResponseEntity<Status>}.
 *   <li>{@code revision(view)}: 리비전 처리에서 {@code view.snapshot}을(를) 사용한다. 입력: {@code view}(조회표현).
 *       반환: {@code String}.
 *   <li>{@code etag(view)}: etag 처리에서 {@code view.stale}, {@code view.unavailable}을(를) 사용한다. 입력:
 *       {@code view}(조회표현). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code refresh()}: 보유 자료의 갱신을 시도한다. 반환: {@code ResponseEntity<CurrencyPriceService.View>}.
 *   <li>{@code response(view)}: 외부로 반환할 응답 값을 구성한다. 입력: {@code view}(조회표현). 반환: {@code
 *       ResponseEntity<CurrencyPriceService.View>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Status}: {@code CurrencyPriceController}에서 사용하는 상태의 불변 값이다.
 * </ul>
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
        .eTag(etag(view))
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(30)).cachePublic())
        .body(view);
  }

  /**
   * {@code CurrencyPriceController}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code String}).
   *   <li>{@code nextRefreshAllowedAt}: 다음·갱신·허용된·기준 ({@code java.time.Instant}).
   *   <li>{@code stale}: stale ({@code boolean}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   *   <li>{@code refreshing}: refreshing ({@code boolean}).
   * </ul>
   */
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
