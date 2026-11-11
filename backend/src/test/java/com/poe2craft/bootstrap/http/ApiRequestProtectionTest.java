package com.poe2craft.bootstrap.http;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.configuration.BackendSettings;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class ApiRequestProtectionTest {
  @Test
  void rateBudgetCannotBeBypassedByForwardedHeadersAndRefills() throws Exception {
    var time = new AtomicLong();
    var filter =
        new ApiRequestProtection(new BackendSettings.RequestProtection(2, 2, 1, 1024), time::get);
    var calls = new AtomicLong();
    FilterChain handler = (req, res) -> calls.incrementAndGet();
    for (int i = 0; i < 3; i++) {
      var request = new MockHttpServletRequest("GET", "/api/%61dmin/dashboard");
      request.addHeader("X-Forwarded-For", "192.0.2." + i);
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, handler);
      assertThat(response.getStatus()).isEqualTo(i < 2 ? 200 : 429);
      if (i == 2) assertThat(response.getHeader("Retry-After")).isEqualTo("1");
    }
    assertThat(calls.get()).isEqualTo(2);
    time.addAndGet(500_000_000);
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/test"), new MockHttpServletResponse(), handler);
    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  void boundsUnknownLengthAndKeepsBodyIntactAndReleasesPermits() throws Exception {
    var filter =
        new ApiRequestProtection(
            new BackendSettings.RequestProtection(20, 20, 1, 1024), System::nanoTime);
    var oversized =
        new MockHttpServletRequest("POST", "/api/test") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    oversized.setContent(new byte[1025]);
    var response = new MockHttpServletResponse();
    filter.doFilter(
        oversized,
        response,
        (req, res) -> {
          throw new AssertionError("must not execute");
        });
    assertThat(response.getStatus()).isEqualTo(413);
    var good = new MockHttpServletRequest("POST", "/api/test");
    good.setContent("{\"value\":17}".getBytes(StandardCharsets.UTF_8));
    filter.doFilter(
        good,
        new MockHttpServletResponse(),
        (req, res) -> {
          assertThat(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
              .isEqualTo("{\"value\":17}");
          var busy = new MockHttpServletResponse();
          filter.doFilter(
              new MockHttpServletRequest("GET", "/api/test"),
              busy,
              (r, s) -> {
                throw new AssertionError("busy");
              });
          assertThat(busy.getStatus()).isEqualTo(429);
        });
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    new MockHttpServletRequest("GET", "/api/test"),
                    new MockHttpServletResponse(),
                    (r, s) -> {
                      throw new java.io.IOException("test");
                    }))
        .isInstanceOf(java.io.IOException.class);
    var calls = new AtomicLong();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/test"),
        new MockHttpServletResponse(),
        (r, s) -> calls.incrementAndGet());
    assertThat(calls.get()).isEqualTo(1);
  }
}
