package com.poe2craft.admin.api;

import com.poe2craft.admin.application.OperationsMonitor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 허용된 브라우저 이벤트만 집계하고 사용자 입력 원문은 저장하지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code monitor}: 운영지표수집기 ({@code OperationsMonitor}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BrowserTelemetryController(monitor)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       monitor}(운영지표수집기).
 *   <li>{@code record(events, request)}: 허용된 이벤트나 관측값을 기록한다. 입력: {@code events}(이벤트목록), {@code
 *       request}(요청). 반환: {@code ResponseEntity<Void>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Event}: {@code BrowserTelemetryController}에서 사용하는 이벤트의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/telemetry")
public final class BrowserTelemetryController {
  private final OperationsMonitor monitor;

  public BrowserTelemetryController(OperationsMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * {@code BrowserTelemetryController}에서 사용하는 이벤트의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code count}: 개수 ({@code int}).
   * </ul>
   */
  public record Event(
      @NotNull
          @Pattern(
              regexp =
                  "view_workbench|view_history|runtime_error|promise_error|history_save|catalog_load")
          String kind,
      @Min(1) @Max(100) int count) {}

  @PostMapping
  public ResponseEntity<Void> record(
      @RequestBody @Size(max = 6) List<@Valid Event> events,
      jakarta.servlet.http.HttpServletRequest request) {
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
    request.setAttribute(
        "operations.pageViews",
        events.stream().filter(e -> e.kind().startsWith("view_")).mapToInt(Event::count).sum());
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
}
