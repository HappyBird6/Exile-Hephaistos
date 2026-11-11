package com.poe2craft.bootstrap.http;

import com.poe2craft.admin.application.RequestEvent;
import com.poe2craft.admin.application.RequestLogService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(-100)
public final class RequestLogFilter extends OncePerRequestFilter {
  private final ObjectProvider<RequestLogService> logs;

  public RequestLogFilter(ObjectProvider<RequestLogService> logs) {
    this.logs = logs;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !UrlPathHelper.defaultInstance.getLookupPathForRequest(request).startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Instant at = Instant.now();
    long start = System.nanoTime();
    String failure = null;
    try {
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException error) {
      failure = error.getClass().getSimpleName();
      throw error;
    } finally {
      var service = logs.getIfAvailable();
      String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
      int status = failure == null ? response.getStatus() : 500;
      if (service != null && !(path.startsWith("/api/admin/") && status < 400)) {
        String visitor = null;
        if (request.getCookies() != null)
          for (var cookie : request.getCookies())
            if (cookie.getName().equals("hephaistos_visitor")
                && cookie
                    .getValue()
                    .matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
              visitor = cookie.getValue().toLowerCase(Locale.ROOT);
        Object matched = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        int views = request.getAttribute("operations.pageViews") instanceof Integer n ? n : 0;
        String method = request.getMethod();
        if (path.equals("/api/edge-rejected") && request.getHeader("X-Rejected-Path") != null) {
          path = request.getHeader("X-Rejected-Path").split("\\?", 2)[0];
          matched = "[edge] " + path;
          if (request.getHeader("X-Rejected-Method") != null)
            method = request.getHeader("X-Rejected-Method");
        }
        service.record(
            new RequestEvent(
                UUID.randomUUID(),
                at,
                clean(method, 12),
                clean(path, 256),
                clean(matched == null ? "/unmatched" : matched.toString(), 256),
                status,
                Math.max(0, (System.nanoTime() - start) / 1_000_000),
                clean(request.getRemoteAddr(), 64),
                visitor,
                views,
                clean(failure, 120),
                clean(request.getHeader("User-Agent"), 256)));
      }
    }
  }

  private static String clean(String value, int max) {
    if (value == null) return null;
    value = value.replaceAll("[\\p{Cntrl}]", "");
    return value.substring(0, Math.min(max, value.length()));
  }
}
