package com.poe2craft.admin.application;

import com.poe2craft.configuration.BackendSettings;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 요청 로그 수집과 조회 및 날짜별 보관 작업을 조정한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code clock}: 현재 시각 공급자 ({@code Clock}).
 *   <li>{@code store}: 저장소 ({@code RequestLogRepository}).
 *   <li>{@code settings}: 운영설정 ({@code BackendSettings.AdminLogs}).
 *   <li>{@code queue}: 대기열 ({@code ArrayBlockingQueue<RequestEvent>}).
 *   <li>{@code dropped}: dropped ({@code AtomicLong}).
 *   <li>{@code budgetDay}: 계산예산·Day ({@code LocalDate}).
 *   <li>{@code dailyCount}: daily·개수 ({@code int}).
 *   <li>{@code rejectionSecond}: 거절·두번째; 초기값 {@code -1} ({@code long}).
 *   <li>{@code storageStatus}: 저장소·상태; 초기값 {@code "OK"} ({@code String}).
 *   <li>{@code archiveStatus}: 아카이브·상태; 초기값 {@code "OK"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RequestLogService(store, settings)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       store}(저장소), {@code settings}(운영설정).
 *   <li>{@code RequestLogService(store, settings, clock)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       store}(저장소), {@code settings}(운영설정), {@code clock}(현재 시각 공급자).
 *   <li>{@code record(event)}: 허용된 이벤트나 관측값을 기록한다. 입력: {@code event}(이벤트).
 *   <li>{@code flush()}: 수집한 기록을 저장소에 반영한다.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 *   <li>{@code archive()}: 아카이브 처리에서 {@code MILLISECONDS.toNanos}, {@code
 *       settings.archiveWorkMillis}을(를) 사용한다.
 *   <li>{@code today()}: today 값을 조회·구성한다. 반환: {@code LocalDate}.
 *   <li>{@code earliestDate()}: earliest·날짜 값을 조회·구성한다. 반환: {@code LocalDate}.
 *   <li>{@code query(filters)}: 조회조건의 입력에 따른 분기·결과를 처리한다. 입력: {@code filters}(필터목록). 반환: {@code
 *       Map<String, Object>}.
 *   <li>{@code query(filters, view)}: 조회조건 처리에서 {@code filters.date}, {@code view.equals}을(를) 사용한다.
 *       입력: {@code filters}(필터목록), {@code view}(조회표현). 반환: {@code Map<String, Object>}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code archives(page)}: archives 처리에서 {@code store.archives}을(를) 사용한다. 입력: {@code
 *       page}(페이지). 반환: {@code Map<String, Object>}.
 *   <li>{@code download(id)}: 다운로드 처리에서 {@code store.download}을(를) 사용한다. 입력: {@code id}(식별자). 반환:
 *       {@code Path}.
 * </ul>
 */
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
    return query(filters, "all");
  }

  public Map<String, Object> query(RequestLogRepository.Filters filters, String view) {
    if (filters.date().isBefore(earliestDate()) || filters.date().isAfter(today()))
      throw new IllegalArgumentException("Date outside retained range");
    var result =
        new LinkedHashMap<String, Object>(
            view.equals("all") ? store.query(filters) : store.query(filters, view));
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
