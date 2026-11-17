package com.poe2craft.craftingsupport.search.domain;

import java.util.Map;

/**
 * 힙 또는 외부 저장소에 실리는 순서 있는 가변 확률 분포 계약이다.
 *
 * <p>기존 계약: Ordered mutable probability law whose payload may live outside the Java heap.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code firstEntry()}: 최초·항목 값을 조회·구성한다. 반환: {@code Map.Entry<K, CertifiedInterval>}.
 *   <li>{@code residentEntries()}: 메모리상주·항목목록 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 */
public interface CertifiedWorkingLaw<K> extends Map<K, CertifiedInterval>, AutoCloseable {
  Map.Entry<K, CertifiedInterval> firstEntry();

  /** Resident entries owned by this map; shared caches are accounted by its store. */
  long residentEntries();

  @Override
  void close();
}
