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

/**
 * HTTP 본문 크기와 요청 빈도를 제한하고 과도한 작업을 파싱 전에 거절한다.
 *
 * <p>기존 계약: Instance-wide bounded admission, including direct backend access and unknown-length
 * bodies.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code limits}: 한도목록 ({@code BackendSettings.RequestProtection}).
 *   <li>{@code inFlight}: 입력·진행중 ({@code Semaphore}).
 *   <li>{@code clock}: 현재 시각 공급자 ({@code LongSupplier}).
 *   <li>{@code libraryBatchCost}: 보관함·일괄실행·비용 ({@code int}).
 *   <li>{@code tokens}: tokens ({@code double}).
 *   <li>{@code last}: 마지막 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ApiRequestProtection(settings, libraryBatchCost)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code settings}(운영설정), {@code libraryBatchCost}(보관함·일괄실행·비용).
 *   <li>{@code ApiRequestProtection(limits, clock)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       limits}(한도목록), {@code clock}(현재 시각 공급자).
 *   <li>{@code ApiRequestProtection(limits, clock, libraryBatchCost)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code limits}(한도목록), {@code clock}(현재 시각 공급자), {@code libraryBatchCost}(보관함·일괄실행·비용).
 *   <li>{@code shouldNotFilter(request)}: 현재 요청을 필터 적용에서 제외할지 판정한다. 입력: {@code request}(요청). 반환:
 *       {@code boolean}.
 *   <li>{@code acquireRate(cost)}: acquire·Rate 처리에서 {@code clock.getAsLong}, {@code
 *       limits.burst}을(를) 사용한다. 입력: {@code cost}(비용). 반환: {@code boolean}.
 *   <li>{@code doFilterInternal(request, response, chain)}: 요청을 검사하고 다음 필터 호출 및 종료 처리를 수행한다. 입력:
 *       {@code request}(요청), {@code response}(응답), {@code chain}(필터연쇄). 사용하는 예외: {@code
 *       ServletException}, {@code IOException}.
 *   <li>{@code reject(response, status)}: 허용되지 않는 요청을 거절한다. 입력: {@code response}(응답), {@code
 *       status}(상태). 사용하는 예외: {@code IOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code BufferedRequest}: {@code ApiRequestProtection} 내부의 Buffered·요청 처리를 맡는다.
 * </ul>
 */
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

  /**
   * {@code ApiRequestProtection} 내부의 Buffered·요청 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code body}: 본문 ({@code byte[]}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code BufferedRequest(request, body)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code request}(요청),
   *       {@code body}(본문).
   *   <li>{@code getInputStream()}: 입력·스트림을(를) 조회한다. 반환: {@code ServletInputStream}. 사용하는 예외:
   *       {@code UnsupportedOperationException}.
   *   <li>{@code getReader()}: 읽기변환기을(를) 조회한다. 반환: {@code BufferedReader}.
   * </ul>
   */
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
