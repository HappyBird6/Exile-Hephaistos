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

/**
 * 공개 게시 기능의 쓰기 요청 빈도를 제한한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code redis}: Redis ({@code StringRedisTemplate}).
 *   <li>{@code limit}: 한도 ({@code int}).
 *   <li>{@code SCRIPT}: SCRIPT ({@code DefaultRedisScript<Long>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CommunityWriteLimit(redis, limit)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code redis}(Redis),
 *       {@code limit}(한도).
 *   <li>{@code shouldNotFilter(r)}: 현재 요청을 필터 적용에서 제외할지 판정한다. 입력: {@code r}(r). 반환: {@code
 *       boolean}.
 *   <li>{@code doFilterInternal(r, s, chain)}: 요청을 검사하고 다음 필터 호출 및 종료 처리를 수행한다. 입력: {@code r}(r),
 *       {@code s}(s), {@code chain}(필터연쇄). 사용하는 예외: {@code IOException}, {@code ServletException}.
 * </ul>
 */
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
