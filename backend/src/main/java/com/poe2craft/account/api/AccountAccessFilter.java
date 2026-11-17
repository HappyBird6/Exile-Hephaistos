package com.poe2craft.account.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * 계정 API에 세션 인증과 요청 크기 제한을 적용한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code redis}: Redis ({@code StringRedisTemplate}).
 *   <li>{@code attempts}: 시도수 ({@code int}).
 *   <li>{@code newSessions}: new·세션목록 ({@code int}).
 *   <li>{@code RATE}: RATE ({@code DefaultRedisScript<Long>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AccountAccessFilter(redis, attempts, newSessions)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       redis}(Redis), {@code attempts}(시도수), {@code newSessions}(new·세션목록).
 *   <li>{@code shouldNotFilter(r)}: 현재 요청을 필터 적용에서 제외할지 판정한다. 입력: {@code r}(r). 반환: {@code
 *       boolean}.
 *   <li>{@code doFilterInternal(r, s, chain)}: 요청을 검사하고 다음 필터 호출 및 종료 처리를 수행한다. 입력: {@code r}(r),
 *       {@code s}(s), {@code chain}(필터연쇄). 사용하는 예외: {@code IOException}, {@code ServletException}.
 *   <li>{@code admit(category, r, s, limit)}: 현재 제한 아래 요청을 받아들일지 판정한다. 입력: {@code category}(분류),
 *       {@code r}(r), {@code s}(s), {@code limit}(한도). 반환: {@code boolean}.
 * </ul>
 */
@Component
@Order(2)
public class AccountAccessFilter extends OncePerRequestFilter {
  private final StringRedisTemplate redis;
  private final int attempts;
  private final int newSessions;
  private static final DefaultRedisScript<Long> RATE =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",
          Long.class);

  public AccountAccessFilter(
      StringRedisTemplate redis,
      @Value("${account.auth.attempts-per-minute}") int attempts,
      @Value("${account.auth.new-sessions-per-minute}") int newSessions) {
    this.redis = redis;
    this.attempts = attempts;
    this.newSessions = newSessions;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest r) {
    return !UrlPathHelper.defaultInstance
        .getLookupPathForRequest(r)
        .matches("/api/(account|community)/.*");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
      throws IOException, ServletException {
    s.setHeader("Cache-Control", "no-store");
    if (!r.getMethod().equals("GET") && !r.getMethod().equals("HEAD")) {
      var session = r.getSession(false);
      String expected = session == null ? null : (String) session.getAttribute("account.csrf");
      String supplied = r.getHeader("X-CSRF-Token");
      if (expected == null
          || supplied == null
          || !MessageDigest.isEqual(
              expected.getBytes(StandardCharsets.UTF_8),
              supplied.getBytes(StandardCharsets.UTF_8))) {
        s.setStatus(403);
        return;
      }
    }
    String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(r);
    // Cookie-less polling must not allocate an unbounded number of Redis sessions.
    // Existing sessions remain usable even after this IP exhausts its creation budget.
    boolean creatingSession =
        (path.equals("/api/account/me") || path.equals("/api/community/context"))
            && r.getSession(false) == null;
    if (creatingSession) {
      if (!admit("session-create", r, s, newSessions)) return;
    }
    if (path.equals("/api/account/login") || path.equals("/api/account/register")) {
      if (!admit("rate", r, s, attempts)) return;
    }
    chain.doFilter(r, s);
  }

  private boolean admit(String category, HttpServletRequest r, HttpServletResponse s, int limit) {
    try {
      Long count =
          redis.execute(RATE, List.of("hephaistos:account:" + category + ":" + r.getRemoteAddr()));
      if (count != null && count <= limit) return true;
      s.setHeader("Retry-After", "60");
      s.setStatus(429);
    } catch (RuntimeException e) {
      s.setStatus(503);
    }
    return false;
  }
}
