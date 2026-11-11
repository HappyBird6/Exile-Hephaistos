package com.poe2craft.admin.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.admin.application.OperationsMonitor;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDashboardControllerTest {
  @Test
  void authenticatesEncodedAndMatrixParameterPathsBeforeRouting() throws Exception {
    var monitor = mock(OperationsMonitor.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new AdminDashboardController(monitor))
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
    MockMvcBuilders.standaloneSetup(new AdminDashboardController(monitor))
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
        MockMvcBuilders.standaloneSetup(new AdminDashboardController(monitor))
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
        MockMvcBuilders.standaloneSetup(new AdminDashboardController(monitor))
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
