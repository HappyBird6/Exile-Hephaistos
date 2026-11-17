package com.poe2craft.craftingsupport.search.application;

import java.util.List;

/**
 * 소유자와 입력·진행 커서를 보존하는 자동 탐색 문서 저장 계약이다.
 *
 * <p>기존 계약: Atomic bounded job documents; the original goal and incomplete cursor are retained.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ids()}: 식별자목록 값을 조회·구성한다. 반환: {@code List<String>}.
 *   <li>{@code read(id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code id}(식별자). 반환: {@code byte[]}.
 *   <li>{@code open(id)}: 호출자가 닫아야 하는 읽기 스트림이다. 파일 구현은 큰 저장 문서의 전체 byte[] 할당을 피한다. 입력: {@code
 *       id}(식별자). 반환: {@code java.io.InputStream}.
 *   <li>{@code save(id, value)}: 현재 값을 저장 경계에 기록한다. 입력: {@code id}(식별자), {@code value}(값).
 *   <li>{@code delete(id)}: 삭제의 실행 계약을 정의한다. 입력: {@code id}(식별자).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code BudgetExceeded}: {@code AutomaticJobRepository} 내부의 계산예산·Exceeded 처리를 맡는다.
 * </ul>
 */
public interface AutomaticJobRepository {
  /**
   * {@code AutomaticJobRepository} 내부의 계산예산·Exceeded 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code BudgetExceeded()}: 전달받은 값으로 구성원을 초기화한다.
   * </ul>
   */
  final class BudgetExceeded extends IllegalArgumentException {
    public BudgetExceeded() {
      super("Job document exceeds storage budget");
    }
  }

  List<String> ids();

  byte[] read(String id);

  /** 호출자가 닫아야 하는 읽기 스트림이다. 파일 구현은 큰 저장 문서의 전체 byte[] 할당을 피한다. */
  default java.io.InputStream open(String id) {
    return new java.io.ByteArrayInputStream(read(id));
  }

  void save(String id, byte[] value);

  void delete(String id);
}
