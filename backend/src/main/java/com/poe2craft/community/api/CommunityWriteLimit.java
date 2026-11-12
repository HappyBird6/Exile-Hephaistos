package com.poe2craft.community.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(3)
public class CommunityWriteLimit extends OncePerRequestFilter {
  private final StringRedisTemplate redis;
  private final int limit;
  private static final DefaultRedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",
          Long.class);

  public CommunityWriteLimit(
      StringRedisTemplate redis, @Value("${community.writes-per-minute}") int limit) {
    this.redis = redis;
    this.limit = limit;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest r) {
    return !UrlPathHelper.defaultInstance.getLookupPathForRequest(r).startsWith("/api/community/")
        || r.getMethod().equals("GET")
        || r.getMethod().equals("HEAD");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
      throws IOException, ServletException {
    try {
      Long n = redis.execute(SCRIPT, List.of("hephaistos:community:write:" + r.getRemoteAddr()));
      if (n == null || n > limit) {
        s.setHeader("Retry-After", "60");
        s.setStatus(429);
        return;
      }
    } catch (RuntimeException e) {
      s.setStatus(503);
      return;
    }
    chain.doFilter(r, s);
  }
}
