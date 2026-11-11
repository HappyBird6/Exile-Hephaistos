package com.poe2craft.admin.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.admin.application.*;
import com.poe2craft.configuration.BackendSettings;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class RequestLogStoreTest {
  @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");
  @TempDir Path directory;
  JdbcTemplate jdbc;
  DataSourceTransactionManager transactions;
  RequestLogStore store;

  RequestLogStore create(Path path) {
    return create(path, 1073741824L);
  }

  RequestLogStore create(Path path, long maximum) {
    var settings = mock(BackendSettings.class);
    when(settings.adminLogs())
        .thenReturn(
            new BackendSettings.AdminLogs(
                7,
                10000,
                500,
                2000,
                60000,
                5000,
                path.toString(),
                "Asia/Seoul",
                40,
                30,
                1073741824L,
                100000));
    return new RequestLogStore(jdbc, transactions, new ObjectMapper(), settings);
  }

  @BeforeEach
  void setup() {
    var source = new DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword());
    jdbc = new JdbcTemplate(source);
    transactions = new DataSourceTransactionManager(source);
    jdbc.execute("DROP SCHEMA IF EXISTS operations CASCADE");
    new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V202611110001__operations_request_logs.sql"))
        .execute(source);
    store = create(directory);
  }

  RequestEvent event(String at, int status, String ip) {
    return new RequestEvent(
        UUID.randomUUID(),
        Instant.parse(at),
        "POST",
        "/api/craft",
        "/api/craft",
        status,
        15,
        ip,
        "00000000-0000-0000-0000-000000000001",
        1,
        null,
        "Browser");
  }

  @Test
  void keepsNewestArchivesWithinByteBudget() throws Exception {
    var bounded = create(directory, 1048576L);
    for (int i = 0; i < 2; i++) {
      var name = "budget-" + i + ".jsonl.gz";
      Files.write(directory.resolve(name), new byte[600000]);
      var time = java.sql.Timestamp.from(Instant.now().minusSeconds(10 - i));
      jdbc.update(
          "INSERT INTO operations.log_archive VALUES (?,?,?,?,?,?,?)",
          UUID.randomUUID(),
          name,
          time,
          time,
          time,
          1,
          600000);
    }
    bounded.archiveBefore(Instant.parse("2000-01-01T00:00:00Z"), 500);
    assertThat(Files.exists(directory.resolve("budget-0.jsonl.gz"))).isFalse();
    assertThat(Files.exists(directory.resolve("budget-1.jsonl.gz"))).isTrue();
    assertThat(jdbc.queryForObject("SELECT sum(bytes) FROM operations.log_archive", Long.class))
        .isEqualTo(600000L);
  }

  @Test
  void removesExpiredArchivesEvenWithoutNewExpiredDatabaseRows() throws Exception {
    var file = directory.resolve("old.jsonl.gz");
    Files.writeString(file, "old");
    jdbc.update(
        "INSERT INTO operations.log_archive VALUES (?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        "old.jsonl.gz",
        java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(31))),
        java.sql.Timestamp.from(Instant.now()),
        java.sql.Timestamp.from(Instant.now()),
        1,
        3);
    store.archiveBefore(Instant.parse("2000-01-01T00:00:00Z"), 500);
    assertThat(Files.exists(file)).isFalse();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM operations.log_archive", Long.class))
        .isZero();
  }

  @Test
  void groupsKoreanDaysFiltersAndPaginates() {
    var rows = new ArrayList<RequestEvent>();
    rows.add(event("2026-11-10T14:59:59Z", 500, "prior"));
    for (int i = 0; i < 43; i++)
      rows.add(event("2026-11-10T15:00:00Z", i == 0 ? 429 : 200, "client"));
    store.insert(rows);
    var result =
        store.query(
            new RequestLogRepository.Filters(
                LocalDate.parse("2026-11-11"), "requests", "client", "", "", null, 2));
    assertThat((List<?>) result.get("rows")).hasSize(3);
    assertThat(((Map<?, ?>) result.get("summary")).get("requests")).isEqualTo(43L);
    assertThat(
            (List<?>)
                store
                    .query(
                        new RequestLogRepository.Filters(
                            LocalDate.parse("2026-11-11"), "limited", "", "", "", null, 1))
                    .get("rows"))
        .hasSize(1);
    assertThat(
            (List<?>)
                store
                    .query(
                        new RequestLogRepository.Filters(
                            LocalDate.parse("2026-11-11"),
                            "requests",
                            "' OR 1=1 --",
                            "",
                            "",
                            null,
                            1))
                    .get("rows"))
        .isEmpty();
  }

  @Test
  void archivesOneKoreanDateBeforeDeletingAndPreservesRetainedBoundary() throws Exception {
    store.insert(
        List.of(
            event("2026-11-02T14:59:59Z", 200, "old1"),
            event("2026-11-02T15:00:00Z", 500, "old2"),
            event("2026-11-03T15:00:00Z", 200, "retained")));
    Instant cutoff = Instant.parse("2026-11-03T15:00:00Z");
    assertThat(store.archiveBefore(cutoff, 500)).isTrue();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM operations.request_log", Integer.class))
        .isEqualTo(2);
    UUID id = jdbc.queryForObject("SELECT id FROM operations.log_archive", UUID.class);
    try (var stream = new GZIPInputStream(Files.newInputStream(store.download(id)))) {
      assertThat(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
          .contains("old1")
          .doesNotContain("old2");
    }
    assertThat(store.archiveBefore(cutoff, 500)).isTrue();
    assertThat(store.archiveBefore(cutoff, 500)).isFalse();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM operations.request_log", Integer.class))
        .isEqualTo(1);
    assertThat(store.download(UUID.randomUUID())).isNull();
  }

  @Test
  void failedArchiveNeverDeletesDatabaseRows() throws Exception {
    var file = Files.writeString(directory.resolve("not-directory"), "x");
    store.insert(List.of(event("2026-11-01T00:00:00Z", 200, "keep")));
    assertThatThrownBy(() -> create(file).archiveBefore(Instant.parse("2026-11-04T00:00:00Z"), 500))
        .isInstanceOf(java.io.UncheckedIOException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM operations.request_log", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM operations.log_archive", Integer.class))
        .isZero();
  }
}
