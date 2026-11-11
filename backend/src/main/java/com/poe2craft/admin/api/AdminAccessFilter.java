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

/** Protect every current and future admin API; failure counters are bounded and expire. */
@Component
@Order(1)
public final class AdminAccessFilter extends OncePerRequestFilter {
  private final byte[] expected;
  private final Map<String, Attempt> attempts = new LinkedHashMap<>();

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
    return !request.getRequestURI().startsWith("/api/admin/");
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
