package com.poe2craft.admin.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 프록시에서 거절한 요청을 운영 지표에 반영한다.
 *
 * <p>기존 계약: Nginx redirects rejected requests here without a body; this never executes application
 * work.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code rejected()}: 거절된 값을 조회·구성한다. 반환: {@code ResponseEntity<Void>}.
 * </ul>
 */
@RestController
public class EdgeRejectionController {
  @GetMapping("/api/edge-rejected")
  public ResponseEntity<Void> rejected() {
    return ResponseEntity.status(429)
        .cacheControl(CacheControl.noStore())
        .header("Retry-After", "1")
        .build();
  }
}
