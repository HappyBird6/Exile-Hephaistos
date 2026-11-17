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

/**
 * 반복·요청·Redis 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code REDIS}: Redis ({@code GenericContainer<?>}).
 *   <li>{@code connection}: DB연결 ({@code LettuceConnectionFactory}).
 *   <li>{@code first}: 최초 ({@code RedisRepeatRequestStore}).
 *   <li>{@code second}: 두번째 ({@code RedisRepeatRequestStore}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code start()}: 각 검증에 필요한 독립 초기 상태와 의존성을 준비한다.
 *   <li>{@code stop()}: 검증에서 사용한 상태와 자원을 정리한다.
 *   <li>{@code concurrentReplicaCannotRerollAndCanCancelTheOriginalRequest()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code IllegalStateException}, {@code AssertionError}, {@code
 *       Exception}.
 *   <li>{@code busyRequestCanRetryTheSameIdOnceCapacityIsAvailable()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code RepeatRejected}, {@code AssertionError}.
 *   <li>{@code ambiguousFailureLeavesPendingReceiptAndRejectedInputIsReplayable()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code IllegalStateException}, {@code AssertionError},
 *       {@code RepeatRejected}.
 * </ul>
 */
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
