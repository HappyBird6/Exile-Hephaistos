package com.poe2craft.admin.application;

import com.poe2craft.configuration.BackendSettings;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RequestLogService {
  private final Clock clock;
  private final RequestLogRepository store;
  private final BackendSettings.AdminLogs settings;
  private final ArrayBlockingQueue<RequestEvent> queue;
  private final AtomicLong dropped = new AtomicLong();
  private LocalDate budgetDay;
  private int dailyCount;
  private long rejectionSecond = -1;
  private volatile String storageStatus = "OK";
  private volatile String archiveStatus = "OK";

  @org.springframework.beans.factory.annotation.Autowired
  public RequestLogService(RequestLogRepository store, BackendSettings settings) {
    this(store, settings, Clock.systemUTC());
  }

  RequestLogService(RequestLogRepository store, BackendSettings settings, Clock clock) {
    this.clock = clock;
    this.store = store;
    this.settings = settings.adminLogs();
    queue = new ArrayBlockingQueue<>(this.settings.queueCapacity());
  }

  public synchronized void record(RequestEvent event) {
    var day = today();
    if (!day.equals(budgetDay)) {
      budgetDay = day;
      dailyCount = 0;
    }
    long second = clock.instant().getEpochSecond();
    if (dailyCount >= settings.dailyRecords()
        || (event.status() == 429 && rejectionSecond == second)) {
      dropped.incrementAndGet();
      return;
    }
    if (event.status() == 429) rejectionSecond = second;
    if (queue.offer(event)) dailyCount++;
    else dropped.incrementAndGet();
  }

  @Scheduled(fixedDelayString = "${backend.admin-logs.flush-millis}")
  public void flush() {
    var events = new ArrayList<RequestEvent>();
    queue.drainTo(events, settings.batchSize());
    if (events.isEmpty()) return;
    try {
      store.insert(events);
      storageStatus = "OK";
    } catch (RuntimeException failure) {
      storageStatus = "UNAVAILABLE";
      for (var event : events) if (!queue.offer(event)) dropped.incrementAndGet();
    }
  }

  @jakarta.annotation.PreDestroy
  public void close() {
    while (!queue.isEmpty()) {
      flush();
      if (!storageStatus.equals("OK")) break;
    }
  }

  @Scheduled(fixedDelayString = "${backend.admin-logs.archive-millis}")
  public void archive() {
    try {
      long deadline =
          System.nanoTime()
              + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(settings.archiveWorkMillis());
      while (store.archiveBefore(
              earliestDate().atStartOfDay(ZoneId.of(settings.timeZone())).toInstant(),
              settings.batchSize())
          && System.nanoTime() < deadline) {}
      archiveStatus = "OK";
    } catch (RuntimeException failure) {
      archiveStatus = "UNAVAILABLE";
    }
  }

  public LocalDate today() {
    return LocalDate.now(clock.withZone(ZoneId.of(settings.timeZone())));
  }

  public LocalDate earliestDate() {
    return today().minusDays(settings.previousDays());
  }

  public Map<String, Object> query(RequestLogRepository.Filters filters) {
    if (filters.date().isBefore(earliestDate()) || filters.date().isAfter(today()))
      throw new IllegalArgumentException("Date outside retained range");
    var result = new LinkedHashMap<String, Object>(store.query(filters));
    result.put("today", today().toString());
    result.put("earliestDate", earliestDate().toString());
    result.put(
        "logging",
        Map.of(
            "queued",
            queue.size(),
            "dropped",
            dropped.get(),
            "storage",
            storageStatus,
            "archive",
            archiveStatus));
    return result;
  }

  public Map<String, Object> archives(int page) {
    return store.archives(page);
  }

  public Path download(UUID id) {
    return store.download(id);
  }
}
