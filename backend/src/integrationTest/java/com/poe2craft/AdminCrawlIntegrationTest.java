package com.poe2craft;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.poe2craft.season.api.CrawlModels.*;
import com.poe2craft.season.application.port.*;
import com.poe2craft.season.application.service.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
    properties = {
      "app.crawl.enabled=true",
      "spring.flyway.locations=classpath:db/migration,classpath:db/testmigration",
      "spring.data.redis.host=localhost",
      "spring.data.redis.port=6379"
    })
class AdminCrawlIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired CrawlStore store;
  @Autowired CrawlService service;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean CrawlRunner runner;

  @BeforeEach
  void resetData() {
    jdbc.update("delete from season.crawl_run");
    var settings = store.settings(true);
    store.update(
        new Update(
            settings.version(),
            List.of(
                new Target(
                    UUID.randomUUID(),
                    "Synthetic capture",
                    "https://poe2db.tw/us/Currency",
                    true))),
        true);
  }

  @Test
  void optimisticSettingsPersistAndEnqueuedSnapshotDoesNotChange() {
    var before = service.settings();
    var queued = store.enqueue();
    var edited =
        service.update(
            new Update(
                before.version(),
                List.of(
                    new Target(
                        UUID.randomUUID(), "Amulets", "https://poe2db.tw/kr/Amulets", true))));
    assertThat(edited.version()).isEqualTo(before.version() + 1);
    assertThatThrownBy(() -> service.update(new Update(before.version(), before.targets())))
        .hasMessage("STALE_SETTINGS");
    assertThat(store.get(queued.id()).targets()).isEqualTo(before.targets());
    assertThatThrownBy(store::enqueue).hasMessage("RUN_ACTIVE");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into season.crawl_run(id,status,targets) values (?,'QUEUED','[]')",
                    UUID.randomUUID()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void staleRunReleasesLeaseAndNoTargetsIsExplicit() {
    var old = store.enqueue();
    jdbc.update(
        "update season.crawl_run set created_at=now()-interval '11 minutes' where id=?", old.id());
    var next = store.enqueue();
    assertThat(next.id()).isNotEqualTo(old.id());
    assertThat(store.get(old.id()).errorCode()).isEqualTo("RUN_EXPIRED");
    store.fail(next.id(), "SYNTHETIC_FAILURE");
    service.update(new Update(service.settings().version(), List.of()));
    assertThatThrownBy(store::enqueue).hasMessage("NO_ENABLED_TARGETS");
  }

  @Test
  void backgroundCaptureRecordsSuccessAndSanitizedFailure() throws Exception {
    var source =
        new Source(
            "https://poe2db.tw/us/Currency",
            200,
            "a".repeat(64),
            42,
            Instant.parse("2026-09-22T00:00:00Z"));
    when(runner.capture(any())).thenReturn(List.of(source));
    Run success = service.launch();
    awaitFinished(success.id());
    assertThat(store.get(success.id()).status()).isEqualTo("RAW_CAPTURED");
    assertThat(store.get(success.id()).sources()).containsExactly(source);
    when(runner.capture(any())).thenThrow(new IllegalStateException("synthetic provider secret"));
    Run failure = service.launch();
    awaitFinished(failure.id());
    assertThat(store.get(failure.id()).errorCode()).isEqualTo("CAPTURE_FAILED");
  }

  @Test
  void disabledRunnerNeverLaunches() {
    try (var disabled = new AutoCloseService(store, runner)) {
      assertThatThrownBy(disabled.service::launch).hasMessage("RUNNER_DISABLED");
      verifyNoInteractions(runner);
    }
  }

  private void awaitFinished(UUID id) throws InterruptedException {
    long until = System.nanoTime() + 5_000_000_000L;
    while (System.nanoTime() < until) {
      if (store.get(id).finishedAt() != null) return;
      Thread.sleep(25);
    }
    fail("Background capture did not finish");
  }

  private static class AutoCloseService implements AutoCloseable {
    final CrawlService service;

    AutoCloseService(CrawlStore store, CrawlRunner runner) {
      service = new CrawlService(store, runner, false);
    }

    public void close() {
      service.close();
    }
  }

  @Test
  void bootstrapDatabaseUpgradesWithoutChangingExistingSchema() {

    // Separate database isolates global module schema names from the application schema.
    try (var upgrade = new PostgreSQLContainer<>("postgres:17.6-alpine")) {
      upgrade.start();
      var initial =
          Flyway.configure()
              .dataSource(upgrade.getJdbcUrl(), upgrade.getUsername(), upgrade.getPassword())
              .target("202609210001")
              .load();
      assertThat(initial.migrate().migrationsExecuted).isEqualTo(1);
      var current =
          Flyway.configure()
              .dataSource(upgrade.getJdbcUrl(), upgrade.getUsername(), upgrade.getPassword())
              .load();
      assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
      assertThat(current.migrate().migrationsExecuted).isZero();
    }
  }
}
