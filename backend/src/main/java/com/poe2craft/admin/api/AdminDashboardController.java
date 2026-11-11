package com.poe2craft.admin.api;

import com.poe2craft.admin.application.OperationsMonitor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** AdminAccessFilter protects this endpoint and future admin routes. */
@RestController
@RequestMapping("/api/admin")
public final class AdminDashboardController {
  private final OperationsMonitor monitor;
  private final com.poe2craft.configuration.BackendSettings.AdminLogs logSettings;

  public AdminDashboardController(
      OperationsMonitor monitor, com.poe2craft.configuration.BackendSettings settings) {
    this.monitor = monitor;
    this.logSettings = settings.adminLogs();
  }

  @GetMapping("/dashboard")
  public ResponseEntity<?> dashboard() {
    var result = new java.util.LinkedHashMap<String, Object>(monitor.snapshot());
    result.put(
        "logPolicy",
        java.util.Map.of(
            "previousDays",
            logSettings.previousDays(),
            "timeZone",
            logSettings.timeZone(),
            "pageSize",
            logSettings.pageSize()));
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
  }
}
