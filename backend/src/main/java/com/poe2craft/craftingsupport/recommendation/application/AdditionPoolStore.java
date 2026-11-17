package com.poe2craft.craftingsupport.recommendation.application;

import java.util.*;

/**
 * 추가 풀 계산 결과의 저장과 조회 경계를 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code find(namespace, key)}: 조건에 맞는 항목을 찾는다. 입력: {@code namespace}(이름공간), {@code key}(키).
 *       반환: {@code Optional<AdditionPoolCache.Data>}.
 *   <li>{@code save(namespace, key, data)}: 현재 값을 저장 경계에 기록한다. 입력: {@code namespace}(이름공간), {@code
 *       key}(키), {@code data}(데이터).
 * </ul>
 */
public interface AdditionPoolStore {
  Optional<AdditionPoolCache.Data> find(String namespace, String key);

  void save(String namespace, String key, AdditionPoolCache.Data data);
}
