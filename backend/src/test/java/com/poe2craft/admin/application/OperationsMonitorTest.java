package com.poe2craft.admin.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.poe2craft.shared.economy.application.CurrencyPriceCache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthEndpoint;

/**
 * 서버 한 인스턴스의 요청·이벤트 지표를 제한된 메모리로 집계한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code reportsRealMetricsExcludesOwnTrafficAndBoundsHistory()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code rejectsArbitraryPayloadCategories()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class OperationsMonitorTest {
  @Test
  void reportsRealMetricsExcludesOwnTrafficAndBoundsHistory() {
    var registry = new SimpleMeterRegistry();
    var monitor =
        new OperationsMonitor(registry, mock(HealthEndpoint.class), mock(CurrencyPriceCache.class));
    registry
        .timer("http.server.requests", "uri", "/api/craft", "status", "500")
        .record(20, TimeUnit.MILLISECONDS);
    registry
        .timer("http.server.requests", "uri", "/api/admin/dashboard", "status", "200")
        .record(100, TimeUnit.MILLISECONDS);
    registry
        .timer("http.server.requests", "uri", "/api/telemetry", "status", "204")
        .record(50, TimeUnit.MILLISECONDS);
    monitor.record("view_workbench", 2);
    monitor.sample();
    var snapshot = monitor.snapshot();
    assertThat(snapshot.get("requests")).isEqualTo(1L);
    assertThat(snapshot.get("serverErrors")).isEqualTo(1L);
    assertThat(snapshot.get("meanLatencyMs")).isEqualTo(20.0);
    assertThat(((List<?>) snapshot.get("alerts")).contains("SERVER_ERRORS")).isTrue();
    for (int i = 0; i < 70; i++) monitor.sample();
    assertThat((List<?>) monitor.snapshot().get("traffic")).hasSize(60);
  }

  @Test
  void rejectsArbitraryPayloadCategories() {
    var monitor =
        new OperationsMonitor(
            new SimpleMeterRegistry(), mock(HealthEndpoint.class), mock(CurrencyPriceCache.class));
    assertThatThrownBy(() -> monitor.record("private item text", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> monitor.record("runtime_error", 101))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
