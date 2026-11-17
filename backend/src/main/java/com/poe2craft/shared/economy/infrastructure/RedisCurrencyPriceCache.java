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

/**
 * Redis에서 시세·갱신 임대·실패 후 재시도 시간을 공유한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code redis}: Redis ({@code StringRedisTemplate}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code snapshotKey}: 스냅샷·키 ({@code String}).
 *   <li>{@code cooldownKey}: cooldown·키 ({@code String}).
 *   <li>{@code lockKey}: 잠금·키 ({@code String}).
 *   <li>{@code CLAIM}: CLAIM ({@code DefaultRedisScript<Long>}).
 *   <li>{@code RELEASE}: 해제 ({@code DefaultRedisScript<Long>}).
 *   <li>{@code SAVE}: SAVE ({@code DefaultRedisScript<Long>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RedisCurrencyPriceCache(redis, json, prefix)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       redis}(Redis), {@code json}(JSON), {@code prefix}(접두).
 *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code PriceSnapshot}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code claim(minimumInterval, lease)}: claim 처리에서 {@code UUID.randomUUID}, {@code
 *       redis.execute}을(를) 사용한다. 입력: {@code minimumInterval}(최소값·구간), {@code lease}(사용권). 반환:
 *       {@code String}.
 *   <li>{@code save(snapshot, token)}: 현재 값을 저장 경계에 기록한다. 입력: {@code snapshot}(스냅샷), {@code
 *       token}(인증토큰). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code release(token)}: 해제 처리에서 {@code redis.execute}을(를) 사용한다. 입력: {@code token}(인증토큰).
 *   <li>{@code nextRefreshAllowed(now)}: 다음·갱신·허용된 처리에서 {@code redis.getExpire}, {@code
 *       now.plusMillis}을(를) 사용한다. 입력: {@code now}(현재시각). 반환: {@code Instant}.
 * </ul>
 */
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
