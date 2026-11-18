package com.poe2craft.shared.economy.api;

import com.poe2craft.shared.economy.application.CurrencyPriceService;

/** 시세 응답의 판본을 계산한다. revision은 리그와 갱신 시각을 조합하고 etag는 시세 상태와 새로고침 가능 여부까지 반영한다. */
final class CurrencyPriceRevision {
  private CurrencyPriceRevision() {}

  static String revision(CurrencyPriceService.View view) {
    return view.snapshot() == null
        ? null
        : view.snapshot().league() + ":" + view.snapshot().updatedAt();
  }

  static String etag(CurrencyPriceService.View view) {
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
}
