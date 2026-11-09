package com.poe2craft.shared.economy.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.economy.application.CurrencyPriceCache;
import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisCurrencyPriceCache implements CurrencyPriceCache {
  private final StringRedisTemplate redis;
  private final ObjectMapper json;
  private final String snapshotKey, cooldownKey, lockKey;
  private static final DefaultRedisScript<Long> CLAIM =
      new DefaultRedisScript<>(
          "if redis.call('exists', KEYS[1]) == 1 or redis.call('exists', KEYS[2]) == 1 then return 0 end "
              + "redis.call('psetex', KEYS[1], ARGV[1], '1'); redis.call('psetex', KEYS[2], ARGV[2], ARGV[3]); return 1",
          Long.class);
  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end return 0",
          Long.class);
  private static final DefaultRedisScript<Long> SAVE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end redis.call('set', KEYS[2], ARGV[2]); return 1",
          Long.class);

  public RedisCurrencyPriceCache(StringRedisTemplate redis, ObjectMapper json, String prefix) {
    this.redis = redis;
    this.json = json;
    snapshotKey = prefix + ":snapshot";
    cooldownKey = prefix + ":cooldown";
    lockKey = prefix + ":lock";
  }

  @Override
  public PriceSnapshot read() {
    var raw = redis.opsForValue().get(snapshotKey);
    if (raw == null) return null;
    try {
      return json.readValue(raw, PriceSnapshot.class);
    } catch (java.io.IOException invalid) {
      throw new IllegalStateException("Invalid cached prices", invalid);
    }
  }

  @Override
  public String claim(Duration minimumInterval, Duration lease) {
    var token = UUID.randomUUID().toString();
    var acquired =
        redis.execute(
            CLAIM,
            List.of(cooldownKey, lockKey),
            "" + minimumInterval.toMillis(),
            "" + lease.toMillis(),
            token);
    if (!Long.valueOf(1).equals(acquired)) return null;
    return token;
  }

  @Override
  public void save(PriceSnapshot snapshot, String token) {
    try {
      var saved =
          redis.execute(
              SAVE, List.of(lockKey, snapshotKey), token, json.writeValueAsString(snapshot));
      if (!Long.valueOf(1).equals(saved))
        throw new IllegalStateException("Price refresh lease expired");
    } catch (java.io.IOException invalid) {
      throw new IllegalStateException("Cannot encode prices", invalid);
    }
  }

  @Override
  public void release(String token) {
    redis.execute(RELEASE, List.of(lockKey), token);
  }

  @Override
  public Instant nextRefreshAllowed(Instant now) {
    long cooldown = redis.getExpire(cooldownKey, TimeUnit.MILLISECONDS);
    long lock = redis.getExpire(lockKey, TimeUnit.MILLISECONDS);
    return now.plusMillis(Math.max(0, Math.max(cooldown, lock)));
  }
}
