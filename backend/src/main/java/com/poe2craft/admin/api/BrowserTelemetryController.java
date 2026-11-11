package com.poe2craft.admin.api;

import com.poe2craft.admin.application.OperationsMonitor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telemetry")
public final class BrowserTelemetryController {
  private final OperationsMonitor monitor;

  public BrowserTelemetryController(OperationsMonitor monitor) {
    this.monitor = monitor;
  }

  public record Event(
      @NotNull
          @Pattern(
              regexp =
                  "view_workbench|view_history|runtime_error|promise_error|history_save|catalog_load")
          String kind,
      @Min(1) @Max(100) int count) {}

  @PostMapping
  public ResponseEntity<Void> record(@RequestBody @Size(max = 6) List<@Valid Event> events) {
    if (events == null
        || events.size() > 6
        || events.stream()
            .anyMatch(
                e ->
                    e == null
                        || e.kind() == null
                        || !OperationsMonitor.EVENTS.contains(e.kind())
                        || e.count() < 1
                        || e.count() > 100)) return ResponseEntity.badRequest().build();
    events.forEach(e -> monitor.record(e.kind(), e.count()));
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
