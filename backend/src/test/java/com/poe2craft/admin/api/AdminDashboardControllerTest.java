package com.poe2craft.admin.api;

import static org.assertj.core.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AdminDashboardControllerTest {
  @Test
  void disabledWithoutConfiguredPassword() {
    var controller = new AdminDashboardController(new SimpleMeterRegistry(), "");
    assertThatThrownBy(() -> controller.dashboard(null))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsMissingAndIncorrectCredentialsWithoutExposingMetrics() {
    var controller = new AdminDashboardController(new SimpleMeterRegistry(), "test-password");
    for (String header : new String[] {null, "Basic invalid"}) {
      var response = controller.dashboard(header);
      assertThat(response.getStatusCode().value()).isEqualTo(401);
      assertThat(response.getBody()).isNull();
      assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }
  }

  @Test
  void authorizedResponseExcludesDashboardPollingFromTraffic() {
    var meters = new SimpleMeterRegistry();
    meters
        .timer("http.server.requests", "uri", "/api/craft", "status", "200")
        .record(10, TimeUnit.MILLISECONDS);
    meters
        .timer("http.server.requests", "uri", "/api/admin/dashboard", "status", "200")
        .record(50, TimeUnit.MILLISECONDS);
    var controller = new AdminDashboardController(meters, "test-password");
    var response =
        controller.dashboard(
            "Basic "
                + Base64.getEncoder()
                    .encodeToString("admin:test-password".getBytes(StandardCharsets.UTF_8)));
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(((java.util.Map<?, ?>) response.getBody()).get("requests")).isEqualTo(1L);
  }
}
