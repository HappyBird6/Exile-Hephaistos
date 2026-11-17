package com.poe2craft.craftingsupport.search.application;

import java.util.List;

/**
 * 구간 확률 탐색의 로컬 작업 파일과 공개 입력 저장 계약이다.
 *
 * <p>기존 계약: Local durable job files; implementation owns file naming and atomic descriptor
 * publication.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code create(descriptor)}: 새 작업 또는 값을 생성한다. 입력: {@code descriptor}(기술정보). 반환: {@code
 *       MethodGraphStore}.
 *   <li>{@code open(descriptor)}: 조회할 자원 또는 작업 범위를 연다. 입력: {@code descriptor}(기술정보). 반환: {@code
 *       MethodGraphStore}.
 *   <li>{@code list()}: 조건에 맞는 목록을 조회한다. 반환: {@code List<Descriptor>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Descriptor}: {@code CertifiedJobRepository}에서 사용하는 기술정보의 불변 값이다.
 * </ul>
 */
public interface CertifiedJobRepository {
  /**
   * {@code CertifiedJobRepository}에서 사용하는 기술정보의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code identity}: 정체성 ({@code MethodGraphStore.Identity}).
   * </ul>
   */
  record Descriptor(String id, MethodGraphStore.Identity identity) {}

  MethodGraphStore create(Descriptor descriptor);

  MethodGraphStore open(Descriptor descriptor);

  List<Descriptor> list();
}
