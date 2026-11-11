package com.poe2craft.admin.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Nginx redirects rejected requests here without a body; this never executes application work. */
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
