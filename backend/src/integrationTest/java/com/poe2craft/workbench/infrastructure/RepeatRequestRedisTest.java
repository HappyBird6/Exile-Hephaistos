package com.poe2craft.workbench.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.workbench.repeat.RepeatRejected;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class RepeatRequestRedisTest {
  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  LettuceConnectionFactory connection;
  RedisRepeatRequestStore first, second;
  ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void start() {
    connection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connection.afterPropertiesSet();
    connection.start();
    var redis = new StringRedisTemplate(connection);
    first = new RedisRepeatRequestStore(redis, json, BackendSettings.defaults());
    second = new RedisRepeatRequestStore(redis, json, BackendSettings.defaults());
  }

  @AfterEach
  void stop() {
    connection.destroy();
  }

  @Test
  void concurrentReplicaCannotRerollAndCanCancelTheOriginalRequest() throws Exception {
    String id = UUID.randomUUID().toString();
    var started = new CountDownLatch(1);
    var finish = new CountDownLatch(1);
    var calls = new AtomicInteger();
    try (var pool = Executors.newSingleThreadExecutor()) {
      var task =
          pool.submit(
              () ->
                  first.execute(
                      id,
                      "digest",
                      stop -> {
                        calls.incrementAndGet();
                        started.countDown();
                        try {
                          if (!finish.await(3, TimeUnit.SECONDS)) throw new IllegalStateException();
                        } catch (InterruptedException e) {
                          throw new IllegalStateException(e);
                        }
                        return json.createObjectNode()
                            .put("cancelled", stop.getAsBoolean())
                            .put("roll", 123);
                      }));
      assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(
              () ->
                  second.execute(
                      id,
                      "digest",
                      stop -> {
                        throw new AssertionError("reroll");
                      }))
          .isInstanceOf(RepeatRejected.class);
      second.cancel(id);
      finish.countDown();
      var result = task.get(3, TimeUnit.SECONDS);
      assertThat(result.path("cancelled").asBoolean()).isTrue();
      assertThat(
              second.execute(
                  id,
                  "digest",
                  stop -> {
                    throw new AssertionError("reroll");
                  }))
          .isEqualTo(result);
      assertThatThrownBy(() -> second.execute(id, "other", stop -> result))
          .isInstanceOf(RepeatRejected.class);
      assertThat(calls).hasValue(1);
    } finally {
      finish.countDown();
    }
  }

  @Test
  void busyRequestCanRetryTheSameIdOnceCapacityIsAvailable() {
    String id = UUID.randomUUID().toString();
    assertThatThrownBy(
            () ->
                first.execute(
                    id,
                    "same",
                    stop -> {
                      throw new RepeatRejected(429, "BUSY", "Busy");
                    }))
        .isInstanceOf(RepeatRejected.class);
    var result = second.execute(id, "same", stop -> json.createObjectNode().put("uses", 1));
    assertThat(
            first.execute(
                id,
                "same",
                stop -> {
                  throw new AssertionError("reroll");
                }))
        .isEqualTo(result);
  }

  @Test
  void ambiguousFailureLeavesPendingReceiptAndRejectedInputIsReplayable() {
    String id = UUID.randomUUID().toString();
    assertThatThrownBy(
            () ->
                first.execute(
                    id,
                    "same",
                    stop -> {
                      throw new IllegalStateException();
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                second.execute(
                    id,
                    "same",
                    stop -> {
                      throw new AssertionError();
                    }))
        .isInstanceOf(RepeatRejected.class);
    String rejected = UUID.randomUUID().toString();
    var result =
        first.execute(
            rejected,
            "same",
            stop -> {
              throw new RepeatRejected(422, "INVALID", "Invalid input");
            });
    assertThat(
            second.execute(
                rejected,
                "same",
                stop -> {
                  throw new AssertionError();
                }))
        .isEqualTo(result);
  }
}
