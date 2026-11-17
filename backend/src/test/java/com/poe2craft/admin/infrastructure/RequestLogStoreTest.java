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

/**
 * 요청 로그를 저장하고 보관 기간이 지난 기록을 아카이브한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code db}: DB ({@code PostgreSQLContainer<?>}).
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code jdbc}: JDBC ({@code JdbcTemplate}).
 *   <li>{@code transactions}: transactions ({@code DataSourceTransactionManager}).
 *   <li>{@code store}: 저장소 ({@code RequestLogStore}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code create(path)}: 새 작업 또는 값을 생성한다. 입력: {@code path}(경로). 반환: {@code RequestLogStore}.
 *   <li>{@code create(path, maximum)}: 새 작업 또는 값을 생성한다. 입력: {@code path}(경로), {@code maximum}(최대값).
 *       반환: {@code RequestLogStore}.
 *   <li>{@code setup()}: 각 검증에 필요한 독립 초기 상태와 의존성을 준비한다.
 *   <li>{@code event(at, status, ip)}: 이벤트 처리에서 {@code UUID.randomUUID}, {@code Instant.parse}을(를)
 *       사용한다. 입력: {@code at}(기준), {@code status}(상태), {@code ip}(ip). 반환: {@code RequestEvent}.
 *   <li>{@code viewsAvoidUnusedQueriesAndPagesReuseFilteredStatistics()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code queryCalls()}: 조회조건·호출횟수 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code keepsNewestArchivesWithinByteBudget()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는
 *       예외: {@code Exception}.
 *   <li>{@code removesExpiredArchivesEvenWithoutNewExpiredDatabaseRows()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code groupsKoreanDaysFiltersAndPaginates()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code archivesOneKoreanDateBeforeDeletingAndPreservesRetainedBoundary()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code failedArchiveNeverDeletesDatabaseRows()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는
 *       예외: {@code Exception}.
 * </ul>
 */
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
                maximum,
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
  void viewsAvoidUnusedQueriesAndPagesReuseFilteredStatistics() {
    jdbc = spy(jdbc);
    store = create(directory);
    var date = LocalDate.parse("2026-11-11");
    store.insert(List.of(event("2026-11-11T00:00:00Z", 200, "192.0.2.1")));
    var f = new RequestLogRepository.Filters(date, "requests", "", "", "", null, 1);
    clearInvocations(jdbc);
    var dashboard = store.query(f, "dashboard");
    assertThat((List<?>) dashboard.get("rows")).isEmpty();
    assertThat(queryCalls()).isEqualTo(2);
    clearInvocations(jdbc);
    store.query(f, "logs");
    assertThat(queryCalls()).isEqualTo(5);
    clearInvocations(jdbc);
    store.query(new RequestLogRepository.Filters(date, "requests", "", "", "", null, 2), "logs");
    assertThat(queryCalls()).isEqualTo(1);
    clearInvocations(jdbc);
    store.query(
        new RequestLogRepository.Filters(date, "requests", "192.0.2.1", "", "", null, 1), "logs");
    assertThat(queryCalls()).isEqualTo(5);
  }

  long queryCalls() {
    return mockingDetails(jdbc).getInvocations().stream()
        .filter(i -> Set.of("queryForMap", "queryForList").contains(i.getMethod().getName()))
        .count();
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
