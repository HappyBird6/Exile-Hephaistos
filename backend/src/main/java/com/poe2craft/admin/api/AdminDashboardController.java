package com.poe2craft.admin.api;

import com.poe2craft.admin.application.OperationsMonitor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * 운영 지표와 로그 보관 정책을 관리자 응답으로 제공한다.
 *
 * <p>기존 계약: AdminAccessFilter protects this endpoint and future admin routes.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code monitor}: 운영지표수집기 ({@code OperationsMonitor}).
 *   <li>{@code logSettings}: 로그·운영설정 ({@code
 *       com.poe2craft.configuration.BackendSettings.AdminLogs}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AdminDashboardController(monitor, settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       monitor}(운영지표수집기), {@code settings}(운영설정).
 *   <li>{@code dashboard()}: 관리자용 운영 지표 응답을 구성한다. 반환: {@code ResponseEntity<?>}.
 * </ul>
 */
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
