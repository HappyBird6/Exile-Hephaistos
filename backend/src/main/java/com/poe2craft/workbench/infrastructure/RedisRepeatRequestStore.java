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

/** Retain the pending marker on ambiguous failure: never reroll an uncertain request on retry. */
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
