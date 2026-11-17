package com.poe2craft.admin.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 관리자 비밀번호와 실패 횟수 제한으로 관리자 API를 보호한다.
 *
 * <p>기존 계약: Protect every current and future admin API; failure counters are bounded and expire.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code expected}: 기대값 ({@code byte[]}).
 *   <li>{@code attempts}: 시도수 ({@code Map<String, Attempt>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AdminAccessFilter(password)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code password}(비밀번호).
 *   <li>{@code shouldNotFilter(request)}: 현재 요청을 필터 적용에서 제외할지 판정한다. 입력: {@code request}(요청). 반환:
 *       {@code boolean}.
 *   <li>{@code doFilterInternal(request, response, chain)}: 요청을 검사하고 다음 필터 호출 및 종료 처리를 수행한다. 입력:
 *       {@code request}(요청), {@code response}(응답), {@code chain}(필터연쇄). 사용하는 예외: {@code
 *       ServletException}, {@code IOException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Attempt}: {@code AdminAccessFilter}에서 사용하는 Attempt의 불변 값이다.
 * </ul>
 */
@Component
@Order(1)
public final class AdminAccessFilter extends OncePerRequestFilter {
  private final byte[] expected;
  private final Map<String, Attempt> attempts = new LinkedHashMap<>();

  /**
   * {@code AdminAccessFilter}에서 사용하는 Attempt의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code count}: 개수 ({@code int}).
   *   <li>{@code until}: until ({@code long}).
   * </ul>
   */
  private record Attempt(int count, long until) {}

  public AdminAccessFilter(@Value("${ADMIN_PASSWORD:}") String password) {
    expected =
        password.isBlank()
            ? null
            : ("Basic "
                    + Base64.getEncoder()
                        .encodeToString(("admin:" + password).getBytes(StandardCharsets.UTF_8)))
                .getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path =
        org.springframework.web.util.UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
    return !path.equals("/api/admin") && !path.startsWith("/api/admin/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("Cache-Control", "no-store");
    if (expected == null) {
      response.setStatus(404);
      return;
    }
    String peer = request.getRemoteAddr(), header = request.getHeader("Authorization");
    long now = System.currentTimeMillis();
    synchronized (attempts) {
      attempts.entrySet().removeIf(e -> e.getValue().until() < now);
      var previous = attempts.get(peer);
      if (previous != null && previous.count() >= 10) {
        response.setHeader(
            "Retry-After", Long.toString(Math.max(1, (previous.until() - now + 999) / 1000)));
        response.setStatus(429);
        return;
      }
    }
    if (header == null
        || !MessageDigest.isEqual(expected, header.getBytes(StandardCharsets.UTF_8))) {
      synchronized (attempts) {
        if (attempts.size() >= 1024) attempts.remove(attempts.keySet().iterator().next());
        var previous = attempts.get(peer);
        attempts.put(
            peer,
            new Attempt(
                previous == null ? 1 : previous.count() + 1,
                previous == null ? now + 60000 : previous.until()));
      }
      response.setStatus(401);
      return;
    }
    synchronized (attempts) {
      attempts.remove(peer);
    }
    chain.doFilter(request, response);
  }
}
