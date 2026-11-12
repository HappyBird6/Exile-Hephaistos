package com.poe2craft.bootstrap.http;

import com.poe2craft.configuration.BackendSettings;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/** Instance-wide bounded admission, including direct backend access and unknown-length bodies. */
@Component
@Order(0)
@Import(com.poe2craft.configuration.BackendConfiguration.class)
public final class ApiRequestProtection extends OncePerRequestFilter {
  private final BackendSettings.RequestProtection limits;
  private final Semaphore inFlight;
  private final LongSupplier clock;
  private final int libraryBatchCost;
  private double tokens;
  private long last;

  @Autowired
  public ApiRequestProtection(
      BackendSettings settings, @Value("${account.library.read-batch-size}") int libraryBatchCost) {
    this(settings.requestProtection(), System::nanoTime, libraryBatchCost);
  }

  ApiRequestProtection(BackendSettings.RequestProtection limits, LongSupplier clock) {
    this(limits, clock, 1);
  }

  ApiRequestProtection(
      BackendSettings.RequestProtection limits, LongSupplier clock, int libraryBatchCost) {
    this.libraryBatchCost = libraryBatchCost;
    this.limits = limits;
    this.clock = clock;
    inFlight = new Semaphore(limits.maxInFlight());
    tokens = limits.burst();
    last = clock.getAsLong();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
    return !path.equals("/api") && !path.startsWith("/api/");
  }

  private synchronized boolean acquireRate(int cost) {
    long now = clock.getAsLong();
    tokens =
        Math.min(
            limits.burst(),
            tokens + Math.max(0, now - last) / 1_000_000_000d * limits.requestsPerSecond());
    last = now;
    if (tokens < cost) return false;
    tokens -= cost;
    return true;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
    // Batching reduces round trips, not the admitted document/response-volume budget.
    int cost =
        request.getMethod().equals("POST") && path.equals("/api/account/library/read-batch")
            ? libraryBatchCost
            : 1;
    if (!acquireRate(cost)) {
      reject(response, 429);
      return;
    }
    if (request.getContentLengthLong() > limits.maxBodyBytes()) {
      reject(response, 413);
      return;
    }
    if (!inFlight.tryAcquire()) {
      reject(response, 429);
      return;
    }
    try {
      // Buffer at most the configured bound plus one byte; never trust Content-Length alone.
      byte[] body = request.getInputStream().readNBytes(limits.maxBodyBytes() + 1);
      if (body.length > limits.maxBodyBytes()) {
        reject(response, 413);
        return;
      }
      chain.doFilter(new BufferedRequest(request, body), response);
    } finally {
      inFlight.release();
    }
  }

  private static void reject(HttpServletResponse response, int status) throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setHeader("Cache-Control", "no-store");
    if (status == 429) response.setHeader("Retry-After", "1");
    response
        .getWriter()
        .write(
            status == 429
                ? "{\"type\":\"about:blank\",\"title\":\"Too many requests\",\"status\":429,\"code\":\"API_BUSY\"}"
                : "{\"type\":\"about:blank\",\"title\":\"Payload too large\",\"status\":413,\"code\":\"PAYLOAD_TOO_LARGE\"}");
  }

  private static final class BufferedRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    BufferedRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
      var input = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return input.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return input.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return input.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException("Synchronous API only");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
