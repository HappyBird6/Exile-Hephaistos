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
