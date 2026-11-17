package com.poe2craft.admin.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.admin.application.OperationsMonitor;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 운영 지표와 로그 보관 정책을 관리자 응답으로 제공한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code authenticatesEncodedAndMatrixParameterPathsBeforeRouting()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code disabledWithoutConfiguredPassword()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외:
 *       {@code Exception}.
 *   <li>{@code rejectsMissingIncorrectAndRepeatedPasswordsBeforeReadingMetrics()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code returnsUncacheableMetricsOnlyWithConfiguredCredential()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class AdminDashboardControllerTest {
  @Test
  void authenticatesEncodedAndMatrixParameterPathsBeforeRouting() throws Exception {
    var monitor = mock(OperationsMonitor.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminDashboardController(
                    monitor, com.poe2craft.configuration.BackendSettings.defaults()))
            .addFilters(new AdminAccessFilter("test-password"))
            .build();
    for (String path :
        List.of("/api/admin;x/dashboard", "/api/%61dmin/dashboard", "/api/admin%2Fdashboard")) {
      mvc.perform(get(java.net.URI.create(path))).andExpect(status().isUnauthorized());
    }
    verifyNoInteractions(monitor);
  }

  @Test
  void disabledWithoutConfiguredPassword() throws Exception {
    var monitor = mock(OperationsMonitor.class);
    MockMvcBuilders.standaloneSetup(
            new AdminDashboardController(
                monitor, com.poe2craft.configuration.BackendSettings.defaults()))
        .addFilters(new AdminAccessFilter(""))
        .build()
        .perform(get("/api/admin/dashboard"))
        .andExpect(status().isNotFound());
    verifyNoInteractions(monitor);
  }

  @Test
  void rejectsMissingIncorrectAndRepeatedPasswordsBeforeReadingMetrics() throws Exception {
    var monitor = mock(OperationsMonitor.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminDashboardController(
                    monitor, com.poe2craft.configuration.BackendSettings.defaults()))
            .addFilters(new AdminAccessFilter("test-password"))
            .build();
    for (int i = 0; i < 10; i++)
      mvc.perform(get("/api/admin/dashboard").header("Authorization", "Basic invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(get("/api/admin/dashboard"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
    verifyNoInteractions(monitor);
  }

  @Test
  void returnsUncacheableMetricsOnlyWithConfiguredCredential() throws Exception {
    var monitor = mock(OperationsMonitor.class);
    when(monitor.snapshot()).thenReturn(Map.of("requests", 12L));
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AdminDashboardController(
                    monitor, com.poe2craft.configuration.BackendSettings.defaults()))
            .addFilters(new AdminAccessFilter("test-password"))
            .build();
    mvc.perform(
            get("/api/admin/dashboard")
                .header(
                    "Authorization",
                    "Basic "
                        + Base64.getEncoder()
                            .encodeToString(
                                "admin:test-password"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8))))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.requests").value(12));
    mvc.perform(get("/api/admin/future-menu")).andExpect(status().isUnauthorized());
  }
}
