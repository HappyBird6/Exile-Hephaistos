package com.poe2craft.craftingsupport.llmtest.application;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 실제 모델 전송 직전 reserve가 UTC 날짜별 호출 수를 Redis에서 원자적으로 예약한다. limit은 서버 전체의 하루 한도이며, 미리보기는 소모하지 않는다. */
@Component
public class CardCallBudget {
  private static final DefaultRedisScript<Long> RESERVE =
      new DefaultRedisScript<>(
          """
      if tonumber(redis.call('GET', KEYS[1]) or '0') >= tonumber(ARGV[1]) then return 0 end
      redis.call('INCR', KEYS[1])
      redis.call('EXPIRE', KEYS[1], 172800)
      return 1
      """,
          Long.class);
  private final StringRedisTemplate redis;
  private final int limit;

  public CardCallBudget(
      StringRedisTemplate redis, @Value("${SUPPORT_TEST_DAILY_CALLS:20}") int limit) {
    this.redis = redis;
    this.limit = limit;
  }

  void reserve() {
    try {
      var day = LocalDate.now(ZoneOffset.UTC);
      var accepted =
          redis.execute(
              RESERVE, List.of("hephaistos:support-test:calls:" + day), Integer.toString(limit));
      if (!Long.valueOf(1).equals(accepted))
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily call limit reached");
    } catch (DataAccessException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Call budget unavailable");
    }
  }
}
