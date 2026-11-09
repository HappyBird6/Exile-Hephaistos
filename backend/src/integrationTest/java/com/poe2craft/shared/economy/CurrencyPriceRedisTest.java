package com.poe2craft.shared.economy;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.economy.domain.PriceSnapshot;
import com.poe2craft.shared.economy.infrastructure.RedisCurrencyPriceCache;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CurrencyPriceRedisTest {
  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  LettuceConnectionFactory connection;
  StringRedisTemplate redis;
  RedisCurrencyPriceCache first, second;

  @BeforeEach
  void start() {
    connection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connection.afterPropertiesSet();
    connection.start();
    redis = new StringRedisTemplate(connection);
    var mapper = new ObjectMapper().findAndRegisterModules();
    String prefix = "test:{prices-" + UUID.randomUUID() + "}:v1";
    first = new RedisCurrencyPriceCache(redis, mapper, prefix);
    second = new RedisCurrencyPriceCache(redis, mapper, prefix);
  }

  @AfterEach
  void stop() {
    connection.destroy();
  }

  private PriceSnapshot snapshot() {
    return new PriceSnapshot(
        "Current League",
        Instant.parse("2026-11-09T00:00:00Z"),
        new BigDecimal("500"),
        Map.of(
            "Divine Orb",
            new PriceSnapshot.Quote("Divine Orb", BigDecimal.ONE, new BigDecimal("500"))));
  }

  @Test
  void atomicClaimCooldownAndSnapshotAreSharedAcrossInstances() throws Exception {
    try (var workers = Executors.newFixedThreadPool(8)) {
      var futures = new ArrayList<Future<String>>();
      for (int i = 0; i < 16; i++) {
        var cache = i % 2 == 0 ? first : second;
        futures.add(
            workers.submit(() -> cache.claim(Duration.ofMinutes(5), Duration.ofSeconds(60))));
      }
      var claims = new ArrayList<String>();
      for (var result : futures) {
        var token = result.get(3, TimeUnit.SECONDS);
        if (token != null) claims.add(token);
      }
      assertThat(claims).hasSize(1);
      var token = claims.getFirst();
      first.save(snapshot(), token);
      first.release(token);
      assertThat(second.read()).isEqualTo(snapshot());
      assertThat(second.claim(Duration.ofMinutes(5), Duration.ofSeconds(60))).isNull();
      var now = Instant.now();
      assertThat(second.nextRefreshAllowed(now))
          .isBetween(now.plusSeconds(290), now.plusSeconds(300));
    }
  }

  @Test
  void expiredOrForeignLeaseCannotOverwriteNewerSnapshot() {
    var token = first.claim(Duration.ofSeconds(1), Duration.ofSeconds(1));
    first.save(snapshot(), token);
    assertThatThrownBy(() -> second.save(snapshot(), "foreign-token"))
        .isInstanceOf(IllegalStateException.class);
    second.release("foreign-token");
    first.save(snapshot(), token);
    first.release(token);
    assertThatThrownBy(() -> first.save(snapshot(), token))
        .isInstanceOf(IllegalStateException.class);
    assertThat(second.read()).isEqualTo(snapshot());
  }

  @Test
  void expiredCollectorCannotWriteOrReleaseTheNewOwnerLease() throws Exception {
    var expired = first.claim(Duration.ofMillis(10), Duration.ofMillis(10));
    Thread.sleep(40);
    var current = second.claim(Duration.ofMinutes(5), Duration.ofSeconds(60));
    assertThat(current).isNotNull();
    assertThatThrownBy(() -> first.save(snapshot(), expired))
        .isInstanceOf(IllegalStateException.class);
    first.release(expired);
    second.save(snapshot(), current);
    assertThat(first.read()).isEqualTo(snapshot());
  }
}
