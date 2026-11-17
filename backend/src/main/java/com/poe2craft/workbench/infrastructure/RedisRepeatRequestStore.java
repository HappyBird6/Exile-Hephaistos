package com.poe2craft.workbench.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.repeat.RepeatRejected;
import com.poe2craft.workbench.repeat.RepeatRequestStore;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 중복 요청과 취소를 Redis로 관리한다. 결과가 불확실하면 대기 표식을 유지한다.
 *
 * <p>기존 계약: Retain the pending marker on ambiguous failure: never reroll an uncertain request on
 * retry.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code redis}: Redis ({@code StringRedisTemplate}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code ttl}: 만료시간 ({@code Duration}).
 *   <li>{@code REPLACE}: REPLACE ({@code DefaultRedisScript<Long>}).
 *   <li>{@code RELEASE}: 해제 ({@code DefaultRedisScript<Long>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RedisRepeatRequestStore(redis, json, settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       redis}(Redis), {@code json}(JSON), {@code settings}(운영설정).
 *   <li>{@code key(id)}: 키 처리에서 {@code RulesetBoundary.identity}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code String}.
 *   <li>{@code execute(id, digest, operation)}: execute 처리에서 {@code TRUE.equals}, {@code
 *       redis.opsForValue}을(를) 사용한다. 입력: {@code id}(식별자), {@code digest}(다이제스트), {@code
 *       operation}(연산). 반환: {@code JsonNode}. 사용하는 예외: {@code RepeatRejected}, {@code
 *       IllegalStateException}.
 *   <li>{@code cancel(id)}: 작업 취소를 반영하고 이후 실행을 막는다. 입력: {@code id}(식별자).
 * </ul>
 */
@Component
public final class RedisRepeatRequestStore implements RepeatRequestStore {
  private final StringRedisTemplate redis;
  private final ObjectMapper json;
  private final Duration ttl;
  private static final DefaultRedisScript<Long> REPLACE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end redis.call('psetex', KEYS[1], ARGV[3], ARGV[2]); return 1",
          Long.class);

  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end return 0",
          Long.class);

  public RedisRepeatRequestStore(
      StringRedisTemplate redis, ObjectMapper json, BackendSettings settings) {
    this.redis = redis;
    this.json = json;
    this.ttl = settings.workbench().repeatReceiptTtl();
  }

  private String key(String id) {
    return "hephaistos:repeat:{" + RulesetBoundary.identity() + ":" + id + "}";
  }

  @Override
  public JsonNode execute(String id, String digest, Function<BooleanSupplier, JsonNode> operation) {
    String key = key(id), pending = digest + ":pending";
    if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, pending, ttl))) {
      String stored = redis.opsForValue().get(key);
      if (stored == null || !stored.startsWith(digest + ":"))
        throw new RepeatRejected(
            409, "REPEAT_REQUEST_CONFLICT", "This request ID is already bound to another request.");
      if (stored.equals(pending))
        throw new RepeatRejected(
            409,
            "REPEAT_REQUEST_PENDING",
            "This request is still running or its outcome is uncertain.");
      try {
        return json.readTree(stored.substring(digest.length() + 1));
      } catch (java.io.IOException invalid) {
        throw new IllegalStateException("Invalid repeat receipt");
      }
    }
    final long[] nextPoll = {0};
    final boolean[] cancelled = {false};
    BooleanSupplier stop =
        () -> {
          if (cancelled[0]) return true;
          long now = System.nanoTime();
          if (now >= nextPoll[0]) {
            nextPoll[0] = now + Duration.ofMillis(100).toNanos();
            cancelled[0] = Boolean.TRUE.equals(redis.hasKey(key + ":cancel"));
          }
          return cancelled[0];
        };
    JsonNode outcome;
    try {
      outcome = operation.apply(stop);
    } catch (RepeatRejected rejected) {
      // Admission rejection performed no work. A Retry-After response must allow the same
      // operation ID to be admitted later, while ambiguous execution failures remain fenced.
      if (rejected.status() == 429) {
        redis.execute(RELEASE, List.of(key), pending);
        throw rejected;
      }
      // Deterministically invalid input keeps its rejection receipt.
      outcome =
          json.createObjectNode()
              .put("rejectedStatus", rejected.status())
              .put("rejectedCode", rejected.code())
              .put("rejectedDetail", rejected.getMessage());
    }
    var saved =
        redis.execute(
            REPLACE, List.of(key), pending, digest + ":" + outcome, Long.toString(ttl.toMillis()));
    if (!Long.valueOf(1).equals(saved))
      throw new IllegalStateException("Repeat receipt lease expired");
    return outcome;
  }

  @Override
  public void cancel(String id) {
    // UUID is an unguessable capability, also used to retrieve a receipt. Do not create arbitrary
    // jobs.
    if (Boolean.TRUE.equals(redis.hasKey(key(id))))
      redis.opsForValue().set(key(id) + ":cancel", "1", ttl);
  }
}
