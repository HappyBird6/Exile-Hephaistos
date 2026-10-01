package com.poe2craft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class InfrastructureIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4.5-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testmigration");
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired Flyway flyway;
  @Autowired DSLContext dsl;
  @Autowired StringRedisTemplate redis;
  @Autowired MockMvc mvc;
  @Autowired jakarta.persistence.EntityManager entityManager;
  @Autowired org.springframework.transaction.support.TransactionTemplate transaction;
  @Autowired com.poe2craft.crafting.application.AdditionPoolStore supportPools;
  @Autowired com.poe2craft.item.ItemCatalog itemCatalog;

  @Test
  void supportPoolSurvivesAColdCacheAndKeepsVersionNamespacesSeparate() {
    var root =
        com.poe2craft.crafting.domain.StateBucket.from(
            com.poe2craft.item.SolarAmulet.initial(itemCatalog));
    var first =
        new com.poe2craft.crafting.application.AdditionPoolCache(
            itemCatalog, supportPools, "integration-rule", "integration-ledger", 2);
    var expected =
        first.get(
            root,
            com.poe2craft.crafting.domain.WorkbenchCurrency.PERFECT_TRANSMUTATION,
            java.util.Set.of());
    var fresh =
        new com.poe2craft.crafting.application.AdditionPoolCache(
            itemCatalog, supportPools, "integration-rule", "integration-ledger", 2);
    assertThat(
            fresh.get(
                root,
                com.poe2craft.crafting.domain.WorkbenchCurrency.PERFECT_TRANSMUTATION,
                java.util.Set.of()))
        .isEqualTo(expected);
    assertThat(fresh.stats().persistedHits()).isEqualTo(1);
    var changed =
        new com.poe2craft.crafting.application.AdditionPoolCache(
            itemCatalog, supportPools, "integration-next-rule", "integration-ledger", 2);
    changed.get(
        root,
        com.poe2craft.crafting.domain.WorkbenchCurrency.PERFECT_TRANSMUTATION,
        java.util.Set.of());
    assertThat(changed.stats().persistedHits()).isZero();
    assertThat(changed.stats().computedPools()).isEqualTo(1);
    assertThat(
            dsl.fetchOne("select count(distinct namespace) from crafting.support_addition_pool")
                .get(0, Long.class))
        .isGreaterThanOrEqualTo(2);
  }

  @Test
  void jpaFlushIsVisibleToJooqAndRollbackIsShared() {
    transaction.executeWithoutResult(
        status -> {
          entityManager.persist(new SyntheticProbe(1L, "synthetic"));
          entityManager.flush();
          assertThat(
                  dsl.fetchOne("select label from synthetic_probe where id = 1")
                      .get(0, String.class))
              .isEqualTo("synthetic");
          status.setRollbackOnly();
        });
    assertThat(dsl.fetchOne("select count(*) from synthetic_probe").get(0, Long.class)).isZero();
  }

  @Test
  void emptyDatabaseMigratesAndRestartDoesNotReapplyMigration() {
    assertThat(flyway.info().applied()).hasSize(4);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(
            dsl.fetchCount(
                dsl.selectFrom("information_schema.schemata").where("schema_name = 'season'")))
        .isEqualTo(1);
    assertThat(dsl.fetchOne("select 1").get(0, Integer.class)).isEqualTo(1);
  }

  @Test
  void redisRoundTripUsesIsolatedContainer() {
    redis.opsForValue().set("bootstrap:probe", "ready");
    assertThat(redis.opsForValue().get("bootstrap:probe")).isEqualTo("ready");
  }

  @Test
  void probesReflectAvailableInfrastructureWithoutExposingDetails() throws Exception {
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
    mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/presets")).andExpect(status().isNotFound());
  }

  @Test
  void removedAdminEndpointsDoNotCreateSessionsOrExposeData() throws Exception {
    for (String path :
        new String[] {
          "/api/v1/admin/session", "/api/v1/admin/crawl-runs", "/api/v1/admin/crawl-settings"
        }) {
      var result = mvc.perform(get(path)).andExpect(status().isNotFound()).andReturn();
      assertThat(result.getRequest().getSession(false)).isNull();
    }
    for (String path :
        new String[] {"/api/v1/admin/login", "/api/v1/admin/logout", "/api/v1/admin/crawl-runs"}) {
      mvc.perform(post(path)).andExpect(status().isNotFound());
    }
  }
}
