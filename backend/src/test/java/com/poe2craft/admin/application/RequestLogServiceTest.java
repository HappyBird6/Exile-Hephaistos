package com.poe2craft.admin.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.poe2craft.configuration.BackendSettings;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RequestLogServiceTest {
  @Test
  void rejectionFloodOnlyQueuesOneDetailPerSecond() {
    var store = mock(RequestLogRepository.class);
    var service =
        new RequestLogService(
            store,
            BackendSettings.defaults(),
            Clock.fixed(Instant.parse("2026-11-11T00:00:00Z"), ZoneOffset.UTC));
    var event =
        new RequestEvent(
            UUID.randomUUID(),
            Instant.now(),
            "GET",
            "/api/test",
            "/api/test",
            429,
            0,
            "ip",
            null,
            0,
            null,
            null);
    for (int i = 0; i < 1000; i++) service.record(event);
    service.flush();
    verify(store).insert(argThat(events -> events.size() == 1));
    when(store.query(any())).thenReturn(Map.of());
    assertThat(
            service
                .query(
                    new RequestLogRepository.Filters(
                        service.today(), "requests", "", "", "", null, 1))
                .get("logging")
                .toString())
        .contains("dropped=999");
  }

  @Test
  void retentionChangesAtKoreanMidnightAndIncludesSevenDaysAgo() {
    var store = mock(RequestLogRepository.class);
    var settings = mock(BackendSettings.class);
    when(settings.adminLogs())
        .thenReturn(
            new BackendSettings.AdminLogs(
                7,
                100,
                50,
                2000,
                60000,
                5000,
                "unused",
                "Asia/Seoul",
                40,
                30,
                1073741824L,
                100000));
    var before =
        new RequestLogService(
            store, settings, Clock.fixed(Instant.parse("2026-11-10T14:59:59Z"), ZoneOffset.UTC));
    var after =
        new RequestLogService(
            store, settings, Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC));
    assertThat(before.today()).isEqualTo(LocalDate.parse("2026-11-10"));
    assertThat(after.today()).isEqualTo(LocalDate.parse("2026-11-11"));
    assertThat(after.earliestDate()).isEqualTo(LocalDate.parse("2026-11-04"));
    when(store.query(any())).thenReturn(Map.of());
    assertThatCode(
            () ->
                after.query(
                    new RequestLogRepository.Filters(
                        LocalDate.parse("2026-11-04"), "requests", "", "", "", null, 1)))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () ->
                after.query(
                    new RequestLogRepository.Filters(
                        LocalDate.parse("2026-11-03"), "requests", "", "", "", null, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void failedFlushRetriesAndBoundedQueueReportsOverflow() {
    var store = mock(RequestLogRepository.class);
    var settings = mock(BackendSettings.class);
    when(settings.adminLogs())
        .thenReturn(
            new BackendSettings.AdminLogs(
                7,
                100,
                50,
                2000,
                60000,
                5000,
                "unused",
                "Asia/Seoul",
                40,
                30,
                1073741824L,
                100000));
    when(store.query(any())).thenReturn(Map.of());
    var service = new RequestLogService(store, settings);
    var event =
        new RequestEvent(
            UUID.randomUUID(),
            Instant.now(),
            "GET",
            "/api/test",
            "/api/test",
            200,
            1,
            "127.0.0.1",
            null,
            0,
            null,
            null);
    for (int i = 0; i < 101; i++) service.record(event);
    doThrow(new IllegalStateException("db offline")).doNothing().when(store).insert(anyList());
    service.flush();
    assertThat(
            service
                .query(
                    new RequestLogRepository.Filters(
                        service.today(), "requests", "", "", "", null, 1))
                .get("logging"))
        .isEqualTo(Map.of("queued", 100, "dropped", 1L, "storage", "UNAVAILABLE", "archive", "OK"));
    service.flush();
    verify(store, times(2)).insert(anyList());
  }
}
