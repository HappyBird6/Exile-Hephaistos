package com.poe2craft.admin.api;

import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Read-only operations console. Credentials are configured by the operator, never bundled. */
@RestController
@RequestMapping("/api/admin")
public final class AdminDashboardController {
  private final MeterRegistry meters;
  private final byte[] credential;

  public AdminDashboardController(
      MeterRegistry meters, @Value("${ADMIN_PASSWORD:}") String password) {
    this.meters = meters;
    credential =
        password.isBlank()
            ? null
            : ("Basic "
                    + Base64.getEncoder()
                        .encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8)))
                .getBytes(StandardCharsets.UTF_8);
  }

  @GetMapping("/dashboard")
  public ResponseEntity<?> dashboard(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (credential == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    if (authorization == null
        || !MessageDigest.isEqual(credential, authorization.getBytes(StandardCharsets.UTF_8)))
      return ResponseEntity.status(401).cacheControl(CacheControl.noStore()).build();
    long requests = 0, errors = 0, rejected = 0;
    double duration = 0, maximum = 0;
    for (var timer : meters.find("http.server.requests").timers()) {
      String uri = timer.getId().getTag("uri"), status = timer.getId().getTag("status");
      if (uri == null || !uri.startsWith("/api/") || uri.startsWith("/api/admin")) continue;
      requests += timer.count();
      duration += timer.totalTime(TimeUnit.MILLISECONDS);
      maximum = Math.max(maximum, timer.max(TimeUnit.MILLISECONDS));
      if (status != null && status.startsWith("5")) errors += timer.count();
      if ("429".equals(status)) rejected += timer.count();
    }
    var memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            Map.ofEntries(
                Map.entry("observedAt", java.time.Instant.now().toString()),
                Map.entry("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000),
                Map.entry("requests", requests),
                Map.entry("serverErrors", errors),
                Map.entry("rejectedRequests", rejected),
                Map.entry("meanLatencyMs", requests == 0 ? 0 : duration / requests),
                Map.entry("recentMaxLatencyMs", maximum),
                Map.entry("heapUsedBytes", memory.getUsed()),
                Map.entry("heapMaxBytes", memory.getMax()),
                Map.entry("processors", Runtime.getRuntime().availableProcessors())));
  }
}
