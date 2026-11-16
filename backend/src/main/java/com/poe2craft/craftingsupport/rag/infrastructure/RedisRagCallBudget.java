package com.poe2craft.craftingsupport.rag.infrastructure;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.rag.application.*;
import java.time.*;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisRagCallBudget implements RagCallBudget {
  private static final DefaultRedisScript<Long> ADMIT =
      new DefaultRedisScript<>(
          """
      if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
      if tonumber(redis.call('GET', KEYS[1]) or '0') >= tonumber(ARGV[1]) then return 0 end
      redis.call('INCR', KEYS[1])
      redis.call('EXPIRE', KEYS[1], 172800)
      redis.call('SET', KEYS[2], '1', 'PX', ARGV[2])
      return 1
      """,
          Long.class);
  private final StringRedisTemplate redis;
  private final BackendSettings.SupportRag settings;
  private final Clock clock;

  public RedisRagCallBudget(
      StringRedisTemplate redis, BackendSettings.SupportRag settings, Clock clock) {
    this.redis = redis;
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public void acquire() {
    Long result;
    try {
      result =
          redis.execute(
              ADMIT,
              List.of(
                  "support-rag:{budget}:" + LocalDate.now(clock.withZone(ZoneOffset.UTC)),
                  "support-rag:{budget}:cooldown"),
              Integer.toString(settings.dailyCalls()),
              Long.toString(settings.cooldown().toMillis()));
    } catch (RuntimeException error) {
      throw new RagFailure(503, "RAG_BUDGET_UNAVAILABLE");
    }
    if (!Long.valueOf(1).equals(result)) throw new RagFailure(429, "RAG_CALL_LIMIT");
  }
}
