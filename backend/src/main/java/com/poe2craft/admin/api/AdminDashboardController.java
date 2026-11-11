package com.poe2craft.admin.api;

import com.poe2craft.admin.application.OperationsMonitor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** AdminAccessFilter protects this endpoint and future admin routes. */
@RestController
@RequestMapping("/api/admin")
public final class AdminDashboardController {
  private final OperationsMonitor monitor;

  public AdminDashboardController(OperationsMonitor monitor) {
    this.monitor = monitor;
  }

  @GetMapping("/dashboard")
  public ResponseEntity<?> dashboard() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(monitor.snapshot());
  }
}
