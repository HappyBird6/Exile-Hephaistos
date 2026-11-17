package com.poe2craft.admin.application;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

/**
 * 요청 로그 조회와 집계에 필요한 저장소 연산을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code insert(events)}: insert의 실행 계약을 정의한다. 입력: {@code events}(이벤트목록).
 *   <li>{@code query(filters)}: 조회조건의 실행 계약을 정의한다. 입력: {@code filters}(필터목록). 반환: {@code
 *       Map<String, Object>}.
 *   <li>{@code query(filters, view)}: 조회조건의 입력에 따른 분기·결과를 처리한다. 입력: {@code filters}(필터목록), {@code
 *       view}(조회표현). 반환: {@code Map<String, Object>}.
 *   <li>{@code archiveBefore(cutoff, limit)}: 아카이브·변경전의 실행 계약을 정의한다. 입력: {@code cutoff}(cutoff),
 *       {@code limit}(한도). 반환: {@code boolean}.
 *   <li>{@code archives(page)}: archives의 실행 계약을 정의한다. 입력: {@code page}(페이지). 반환: {@code
 *       Map<String, Object>}.
 *   <li>{@code download(id)}: 다운로드의 실행 계약을 정의한다. 입력: {@code id}(식별자). 반환: {@code Path}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Filters}: {@code RequestLogRepository}에서 사용하는 필터목록의 불변 값이다.
 * </ul>
 */
public interface RequestLogRepository {

  /**
   * {@code RequestLogRepository}에서 사용하는 필터목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code date}: 날짜 ({@code LocalDate}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code ip}: ip ({@code String}).
   *   <li>{@code visitor}: visitor ({@code String}).
   *   <li>{@code route}: 경로 ({@code String}).
   *   <li>{@code status}: 현재 처리 상태 ({@code Integer}).
   *   <li>{@code page}: 페이지 ({@code int}).
   * </ul>
   */
  record Filters(
      LocalDate date,
      String kind,
      String ip,
      String visitor,
      String route,
      Integer status,
      int page) {}

  void insert(List<RequestEvent> events);

  Map<String, Object> query(Filters filters);

  default Map<String, Object> query(Filters filters, String view) {
    return query(filters);
  }

  boolean archiveBefore(Instant cutoff, int limit);

  Map<String, Object> archives(int page);

  Path download(UUID id);
}
