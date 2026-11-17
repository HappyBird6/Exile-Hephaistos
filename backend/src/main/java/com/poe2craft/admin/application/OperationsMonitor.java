package com.poe2craft.admin.application;

import com.poe2craft.shared.economy.application.CurrencyPriceCache;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 서버 한 인스턴스의 요청·이벤트 지표를 제한된 메모리로 집계한다.
 *
 * <p>기존 계약: Bounded, per-instance telemetry. Never retains item text, URLs, user identifiers or
 * credentials.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code EVENTS}: 이벤트목록 ({@code Set<String>}).
 *   <li>{@code meters}: meters ({@code MeterRegistry}).
 *   <li>{@code health}: 상태점검 ({@code HealthEndpoint}).
 *   <li>{@code prices}: 시세목록 ({@code CurrencyPriceCache}).
 *   <li>{@code browser}: browser ({@code Map<String, Long>}).
 *   <li>{@code samples}: samples ({@code Deque<Map<String, Object>>}).
 *   <li>{@code previousRequests}: 이전·요청목록 ({@code long}).
 *   <li>{@code previousErrors}: 이전·오류목록 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code OperationsMonitor(meters, health, prices)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       meters}(meters), {@code health}(상태점검), {@code prices}(시세목록).
 *   <li>{@code record(kind, count)}: 허용된 이벤트나 관측값을 기록한다. 입력: {@code kind}(종류), {@code count}(개수).
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code totals()}: totals 값을 조회·구성한다. 반환: {@code long[]}.
 *   <li>{@code sample()}: sample 처리에서 {@code samples.addLast}, {@code Instant.now}을(를) 사용한다.
 *   <li>{@code status(component)}: 현재 진행 상태를 조회한다. 입력: {@code component}(구성요소). 반환: {@code String}.
 *   <li>{@code snapshot()}: 현재 내용을 불변 관측값으로 구성한다. 반환: {@code Map<String, Object>}.
 * </ul>
 */
@Service
public final class OperationsMonitor {
  public static final Set<String> EVENTS =
      Set.of(
          "view_workbench",
          "view_history",
          "runtime_error",
          "promise_error",
          "history_save",
          "catalog_load");
  private final MeterRegistry meters;
  private final HealthEndpoint health;
  private final CurrencyPriceCache prices;
  private final Map<String, Long> browser = new TreeMap<>();
  private final Deque<Map<String, Object>> samples = new ArrayDeque<>();
  private long previousRequests, previousErrors;

  public OperationsMonitor(MeterRegistry meters, HealthEndpoint health, CurrencyPriceCache prices) {
    this.meters = meters;
    this.health = health;
    this.prices = prices;
  }

  public synchronized void record(String kind, int count) {
    if (!EVENTS.contains(kind) || count < 1 || count > 100)
      throw new IllegalArgumentException("Invalid telemetry event");
    browser.merge(kind, (long) count, Long::sum);
  }

  private long[] totals() {
    long requests = 0, errors = 0, rejected = 0;
    for (var timer : meters.find("http.server.requests").timers()) {
      String uri = timer.getId().getTag("uri"), status = timer.getId().getTag("status");
      if (uri == null
          || !uri.startsWith("/api/")
          || uri.startsWith("/api/admin")
          || uri.startsWith("/api/telemetry")) continue;
      requests += timer.count();
      if (status != null && status.startsWith("5")) errors += timer.count();
      if ("429".equals(status)) rejected += timer.count();
    }
    return new long[] {requests, errors, rejected};
  }

  @Scheduled(fixedRate = 60000)
  public synchronized void sample() {
    var totals = totals();
    samples.addLast(
        Map.of(
            "at",
            Instant.now().toString(),
            "requests",
            Math.max(0, totals[0] - previousRequests),
            "errors",
            Math.max(0, totals[1] - previousErrors)));
    previousRequests = totals[0];
    previousErrors = totals[1];
    while (samples.size() > 60) samples.removeFirst();
  }

  private String status(String component) {
    try {
      var result = health.healthForPath(component);
      return result == null ? "UNKNOWN" : result.getStatus().getCode();
    } catch (RuntimeException unavailable) {
      return "UNKNOWN";
    }
  }

  public synchronized Map<String, Object> snapshot() {
    var totals = totals();
    double duration = 0, maximum = 0;
    for (var timer : meters.find("http.server.requests").timers()) {
      String uri = timer.getId().getTag("uri");
      if (uri == null
          || !uri.startsWith("/api/")
          || uri.startsWith("/api/admin")
          || uri.startsWith("/api/telemetry")) continue;
      duration += timer.totalTime(TimeUnit.MILLISECONDS);
      maximum = Math.max(maximum, timer.max(TimeUnit.MILLISECONDS));
    }
    var memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    var os = ManagementFactory.getOperatingSystemMXBean();
    double cpu =
        os instanceof com.sun.management.OperatingSystemMXBean extended
            ? extended.getCpuLoad()
            : -1;
    var result = new LinkedHashMap<String, Object>();
    result.put("observedAt", Instant.now().toString());
    result.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    result.put("requests", totals[0]);
    result.put("serverErrors", totals[1]);
    result.put("rejectedRequests", totals[2]);
    result.put("meanLatencyMs", totals[0] == 0 ? 0 : duration / totals[0]);
    result.put("recentMaxLatencyMs", maximum);
    result.put("heapUsedBytes", memory.getUsed());
    result.put("heapMaxBytes", memory.getMax());
    result.put(
        "memoryPools",
        ManagementFactory.getMemoryPoolMXBeans().stream()
            .filter(pool -> pool.getUsage() != null)
            .map(
                pool ->
                    Map.of(
                        "name",
                        pool.getName(),
                        "used",
                        pool.getUsage().getUsed(),
                        "committed",
                        pool.getUsage().getCommitted(),
                        "max",
                        pool.getUsage().getMax()))
            .toList());
    result.put(
        "garbageCollectors",
        ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(
                gc ->
                    Map.of(
                        "name",
                        gc.getName(),
                        "count",
                        gc.getCollectionCount(),
                        "millis",
                        gc.getCollectionTime()))
            .toList());
    result.put("processors", Runtime.getRuntime().availableProcessors());
    result.put("cpuPercent", cpu < 0 ? null : cpu * 100);
    result.put("database", status("db"));
    result.put("redis", status("redis"));
    var connections = meters.find("hikaricp.connections.active").gauges();
    result.put(
        "databaseConnections",
        connections.isEmpty() ? null : connections.stream().mapToDouble(g -> g.value()).sum());
    try {
      var snapshot = prices.read();
      result.put("priceUpdatedAt", snapshot == null ? null : snapshot.updatedAt().toString());
      result.put(
          "priceStale",
          snapshot == null
              || snapshot.updatedAt().isBefore(Instant.now().minus(Duration.ofHours(1))));
    } catch (RuntimeException unavailable) {
      result.put("priceUpdatedAt", null);
      result.put("priceStale", true);
    }
    result.put("browserEvents", Map.copyOf(browser));
    result.put("traffic", List.copyOf(samples));
    var alerts = new ArrayList<String>();
    if (!"UP".equals(result.get("database"))) alerts.add("DATABASE");
    if (!"UP".equals(result.get("redis"))) alerts.add("REDIS");
    if (Boolean.TRUE.equals(result.get("priceStale"))) alerts.add("PRICES");
    if (memory.getMax() > 0 && memory.getUsed() > .85 * memory.getMax()) alerts.add("MEMORY");
    var last = samples.peekLast();
    if (last != null && (long) last.get("errors") > 0) alerts.add("SERVER_ERRORS");
    result.put("alerts", alerts);
    return result;
  }
}
